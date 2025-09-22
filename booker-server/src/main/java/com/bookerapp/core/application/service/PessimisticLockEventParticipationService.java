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

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PessimisticLockEventParticipationService {

    private final EventRepository eventRepository;
    private final EntityManager entityManager;
    private final AtomicInteger lockCount = new AtomicInteger(0);

    @Transactional
    public EventParticipationDto.Response participateInEvent(EventParticipationDto.Request request) {
        log.info("Pessimistic lock participation request for event: {}, member: {}", request.getEventId(), request.getMemberId());
        
        lockCount.incrementAndGet();
        
        // 비관적 락을 사용하여 이벤트 조회
        Event event = entityManager.find(Event.class, request.getEventId(), LockModeType.PESSIMISTIC_WRITE);
        if (event == null) {
            throw new RuntimeException("Event not found");
        }

        Member member = new Member(request.getMemberId(), request.getMemberName(), request.getMemberEmail());

        if (isAlreadyParticipating(event, member)) {
            return new EventParticipationDto.Response(null, "ALREADY_PARTICIPATING", null, "이미 참여 신청된 이벤트입니다.");
        }

        if (event.isFullyBooked()) {
            int nextWaitingNumber = getNextWaitingNumber(event);
            EventParticipation participation = new EventParticipation(event, member, ParticipationStatus.WAITING, nextWaitingNumber);
            event.getParticipants().add(participation);

            log.info("Added to waiting list (Pessimistic Lock) - Event: {}, Member: {}, Waiting Number: {}",
                    request.getEventId(), request.getMemberId(), nextWaitingNumber);

            return new EventParticipationDto.Response(participation.getId(), "WAITING", nextWaitingNumber,
                    "대기자 명단에 등록되었습니다. 대기 순번: " + nextWaitingNumber);
        } else {
            EventParticipation participation = new EventParticipation(event, member, ParticipationStatus.CONFIRMED);
            event.getParticipants().add(participation);

            log.info("Confirmed participation (Pessimistic Lock) - Event: {}, Member: {}", request.getEventId(), request.getMemberId());

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

    public int getLockCount() {
        return lockCount.get();
    }

    public void resetLockCount() {
        lockCount.set(0);
    }

    public static class ParticipationInfo {
        private final String status;
        private final Integer waitingNumber;
        private final String message;

        public ParticipationInfo(String status, Integer waitingNumber, String message) {
            this.status = status;
            this.waitingNumber = waitingNumber;
            this.message = message;
        }

        public String getStatus() { return status; }
        public Integer getWaitingNumber() { return waitingNumber; }
        public String getMessage() { return message; }
    }
}
