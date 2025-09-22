package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import com.bookerapp.core.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockEventParticipationService {

    private final EventRepository eventRepository;
    private final AtomicInteger retryCounter = new AtomicInteger(0);

    @Transactional
    @Retryable(
        value = {OptimisticLockException.class},
        maxAttempts = 10,
        backoff = @Backoff(delay = 10, multiplier = 1.5)
    )
    public EventParticipationDto.Response participateInEvent(EventParticipationDto.Request request) {
        log.info("Optimistic lock participation request for event: {}, member: {}", request.getEventId(), request.getMemberId());
        retryCounter.incrementAndGet();
        return attemptParticipation(request);
    }

    @Recover
    public EventParticipationDto.Response recoverFromOptimisticLockException(OptimisticLockException e, EventParticipationDto.Request request) {
        log.error("Max retry attempts exceeded for event: {}, member: {}", request.getEventId(), request.getMemberId());
        throw new RuntimeException("참여 신청 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
    }

    private EventParticipationDto.Response attemptParticipation(EventParticipationDto.Request request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Member member = new Member(request.getMemberId(), request.getMemberName(), request.getMemberEmail());

        if (isAlreadyParticipating(event, member)) {
            return new EventParticipationDto.Response(null, "ALREADY_PARTICIPATING", null, "이미 참여 신청된 이벤트입니다.");
        }

        if (event.isFullyBooked()) {
            int nextWaitingNumber = getNextWaitingNumber(event);
            EventParticipation participation = new EventParticipation(event, member, ParticipationStatus.WAITING, nextWaitingNumber);
            event.getParticipants().add(participation);
            eventRepository.save(event); // 낙관적 락을 위한 명시적 저장

            log.info("Added to waiting list (Optimistic Lock) - Event: {}, Member: {}, Waiting Number: {}",
                    request.getEventId(), request.getMemberId(), nextWaitingNumber);

            return new EventParticipationDto.Response(participation.getId(), "WAITING", nextWaitingNumber,
                    "대기자 명단에 등록되었습니다. 대기 순번: " + nextWaitingNumber);
        } else {
            EventParticipation participation = new EventParticipation(event, member, ParticipationStatus.CONFIRMED);
            event.getParticipants().add(participation);
            eventRepository.save(event); // 낙관적 락을 위한 명시적 저장

            log.info("Confirmed participation (Optimistic Lock) - Event: {}, Member: {}", request.getEventId(), request.getMemberId());

            return new EventParticipationDto.Response(participation.getId(), "CONFIRMED", null, "참여가 확정되었습니다.");
        }
    }

    private boolean isAlreadyParticipating(Event event, Member member) {
        return event.getParticipants().stream()
                .anyMatch(p -> p.getParticipant().equals(member) &&
                        (p.getStatus() == ParticipationStatus.CONFIRMED || p.getStatus() == ParticipationStatus.WAITING));
    }

    private int getNextWaitingNumber(Event event) {
        return event.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipationStatus.WAITING)
                .map(EventParticipation::getWaitingNumber)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    public int getRetryCount() {
        return retryCounter.get();
    }

    public void resetRetryCount() {
        retryCounter.set(0);
    }
}
