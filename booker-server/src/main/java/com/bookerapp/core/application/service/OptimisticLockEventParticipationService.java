package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import com.bookerapp.core.domain.repository.EventRepository;
import com.bookerapp.core.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockEventParticipationService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final AtomicInteger retryCounter = new AtomicInteger(0);
    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final long RETRY_DELAY_MS = 5;

    @Transactional
    public EventParticipationDto.Response participateInEvent(EventParticipationDto.Request request) {
        log.info("Optimistic Lock participation request for event: {}, member: {}", request.getEventId(), request.getMemberId());

        for (int attempts = 0; attempts < MAX_RETRY_ATTEMPTS; attempts++) {
            try {
                return attemptParticipationWithOptimisticLock(request);
            } catch (OptimisticLockException | OptimisticLockingFailureException e) {
                retryCounter.incrementAndGet();

                if (attempts == MAX_RETRY_ATTEMPTS - 1) {
                    log.error("Max optimistic lock retries exceeded for event: {}, member: {}, treating as waiting",
                            request.getEventId(), request.getMemberId());
                    // After max retries, treat as waiting list participant
                    return new EventParticipationDto.Response(null, "WAITING", null,
                            "참여 신청이 많아 대기자 명단에 등록되었습니다.");
                }

                log.warn("OptimisticLockException occurred, attempt {}/{} for event: {}, member: {}",
                        attempts + 1, MAX_RETRY_ATTEMPTS, request.getEventId(), request.getMemberId());

                try {
                    Thread.sleep(RETRY_DELAY_MS * (attempts + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("처리가 중단되었습니다.");
                }
            }
        }

        throw new RuntimeException("예상치 못한 오류가 발생했습니다.");
    }

    private EventParticipationDto.Response attemptParticipationWithOptimisticLock(EventParticipationDto.Request request) {
        Event event = eventRepository.findByIdWithOptimisticLock(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        log.info("Optimistic Lock attempt - Event: {}, Current version: {}, Current participants: {}",
                request.getEventId(), event.getVersion(), event.getParticipants().size());

        Member member = memberRepository.findByMemberId(request.getMemberId())
                .orElseGet(() -> {
                    Member newMember = new Member(request.getMemberId(), request.getMemberName(), request.getMemberEmail());
                    return memberRepository.save(newMember);
                });

        if (isAlreadyParticipating(event, member)) {
            return new EventParticipationDto.Response(null, "ALREADY_PARTICIPATING", null, "이미 참여 신청된 이벤트입니다.");
        }

        EventParticipation participation;
        String status;
        Integer waitingNumber = null;
        String message;

        long confirmedCount = event.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
                .count();

        log.info("Before adding participant - Event: {}, Confirmed count: {}, Max: {}, IsFullyBooked: {}",
                request.getEventId(), confirmedCount, event.getMaxParticipants(), event.isFullyBooked());

        if (event.isFullyBooked()) {
            int nextWaitingNumber = getNextWaitingNumber(event);
            participation = new EventParticipation(event, member, ParticipationStatus.WAITING, nextWaitingNumber);
            event.getParticipants().add(participation);

            status = "WAITING";
            waitingNumber = nextWaitingNumber;
            message = "대기자 명단에 등록되었습니다. 대기 순번: " + nextWaitingNumber;

            log.info("Added to waiting list (Optimistic Lock) - Event: {}, Member: {}, Waiting Number: {}, Version: {}",
                    request.getEventId(), request.getMemberId(), nextWaitingNumber, event.getVersion());
        } else {
            participation = new EventParticipation(event, member, ParticipationStatus.CONFIRMED);
            event.getParticipants().add(participation);

            status = "CONFIRMED";
            message = "참여가 확정되었습니다.";

            log.info("Confirmed participation (Optimistic Lock) - Event: {}, Member: {}, Version: {}",
                    request.getEventId(), request.getMemberId(), event.getVersion());
        }

        try {
            Event savedEvent = eventRepository.save(event);
            log.info("Optimistic Lock success - Event: {}, New version: {}, Final participants: {}",
                    request.getEventId(), savedEvent.getVersion(), savedEvent.getParticipants().size());
        } catch (OptimisticLockingFailureException | OptimisticLockException e) {
            log.warn("Optimistic Lock failed - Event: {}, Member: {}, Exception: {}",
                    request.getEventId(), request.getMemberId(), e.getClass().getSimpleName());
            throw e;
        }

        return new EventParticipationDto.Response(participation.getId(), status, waitingNumber, message);
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
