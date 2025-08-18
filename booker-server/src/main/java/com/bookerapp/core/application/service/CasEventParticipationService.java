package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import com.bookerapp.core.domain.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.persistence.OptimisticLockException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasEventParticipationService {

    private final EventRepository eventRepository;
    private final AtomicInteger retryCounter = new AtomicInteger(0);

    @Transactional
    public EventParticipationDto.Response participateInEvent(EventParticipationDto.Request request) {
        log.info("CAS participation request for event: {}, member: {}", request.getEventId(), request.getMemberId());

        int maxRetries = 10;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                return attemptParticipation(request);
            } catch (OptimisticLockException e) {
                retryCount++;
                retryCounter.incrementAndGet();
                log.warn("CAS retry attempt {} for event: {}, member: {}", retryCount, request.getEventId(), request.getMemberId());

                if (retryCount >= maxRetries) {
                    log.error("Max retries exceeded for event: {}, member: {}", request.getEventId(), request.getMemberId());
                    throw new RuntimeException("참여 신청 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
                }

                try {
                    Thread.sleep(10); // 짧은 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("참여 신청이 중단되었습니다.");
                }
            }
        }

        throw new RuntimeException("참여 신청 처리 중 오류가 발생했습니다.");
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

            log.info("Added to waiting list (CAS) - Event: {}, Member: {}, Waiting Number: {}",
                    request.getEventId(), request.getMemberId(), nextWaitingNumber);

            return new EventParticipationDto.Response(participation.getId(), "WAITING", nextWaitingNumber,
                    "대기자 명단에 등록되었습니다. 대기 순번: " + nextWaitingNumber);
        } else {
            EventParticipation participation = new EventParticipation(event, member, ParticipationStatus.CONFIRMED);
            event.getParticipants().add(participation);

            log.info("Confirmed participation (CAS) - Event: {}, Member: {}", request.getEventId(), request.getMemberId());

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
