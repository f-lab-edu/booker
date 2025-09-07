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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PessimisticLockEventParticipationService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final AtomicInteger lockWaitCounter = new AtomicInteger(0);

    @Transactional
    public EventParticipationDto.Response participateInEvent(EventParticipationDto.Request request) {
        log.info("Pessimistic lock participation request for event: {}, member: {}", request.getEventId(), request.getMemberId());

        try {
            return attemptParticipationWithPessimisticLock(request);
        } catch (Exception e) {
            log.error("Pessimistic lock participation failed for event: {}, member: {}", request.getEventId(), request.getMemberId(), e);
            throw new RuntimeException("참여 신청 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    @Transactional
    private EventParticipationDto.Response attemptParticipationWithPessimisticLock(EventParticipationDto.Request request) {
        // 비관적 락으로 Event 조회 (데이터베이스 레벨에서 배타적 락)
        Event event = eventRepository.findByIdWithPessimisticWriteLock(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // LazyInitializationException 방지를 위해 participants 컬렉션 명시적 초기화
        event.getParticipants().size();

        Member member = memberRepository.findByMemberId(request.getMemberId())
                .orElseGet(() -> {
                    Member newMember = new Member(request.getMemberId(), request.getMemberName(), request.getMemberEmail());
                    return memberRepository.save(newMember);
                });

        ParticipationInfo info = analyzeParticipation(event, member);

        if (info.isAlreadyParticipating) {
            return new EventParticipationDto.Response(null, "ALREADY_PARTICIPATING", null, "이미 참여 신청된 이벤트입니다.");
        }

        // 비관적 락으로 보호된 상태에서 참여 처리
        EventParticipation participation;
        if (info.confirmedCount < event.getMaxParticipants()) {
            participation = new EventParticipation(event, member, ParticipationStatus.CONFIRMED);
            event.getParticipants().add(participation);

            eventRepository.save(event);

            log.info("Confirmed participation (Pessimistic Lock) - Event: {}, Member: {}", request.getEventId(), request.getMemberId());
            return new EventParticipationDto.Response(participation.getId(), "CONFIRMED", null, "참여가 확정되었습니다.");
        } else {
            int nextWaitingNumber = info.maxWaitingNumber + 1;
            participation = new EventParticipation(event, member, ParticipationStatus.WAITING, nextWaitingNumber);
            event.getParticipants().add(participation);

            eventRepository.save(event);

            log.info("Added to waiting list (Pessimistic Lock) - Event: {}, Member: {}, Waiting Number: {}",
                    request.getEventId(), request.getMemberId(), nextWaitingNumber);
            return new EventParticipationDto.Response(participation.getId(), "WAITING", nextWaitingNumber,
                    "대기자 명단에 등록되었습니다. 대기 순번: " + nextWaitingNumber);
        }
    }

    // 한 번의 순회로 모든 필요한 정보 수집 (성능 최적화)
    private ParticipationInfo analyzeParticipation(Event event, Member member) {
        int confirmedCount = 0;
        int maxWaitingNumber = 0;
        boolean isAlreadyParticipating = false;

        for (EventParticipation p : event.getParticipants()) {
            if (p.getStatus() == ParticipationStatus.CONFIRMED) {
                confirmedCount++;
            } else if (p.getStatus() == ParticipationStatus.WAITING) {
                maxWaitingNumber = Math.max(maxWaitingNumber, p.getWaitingNumber() != null ? p.getWaitingNumber() : 0);
            }

            // 중복 참여 검증: member ID로 직접 비교 (LazyInitializationException 방지)
            if (p.getParticipant() != null && p.getParticipant().getMemberId().equals(member.getMemberId()) &&
                (p.getStatus() == ParticipationStatus.CONFIRMED || p.getStatus() == ParticipationStatus.WAITING)) {
                isAlreadyParticipating = true;
            }
        }

        return new ParticipationInfo(confirmedCount, maxWaitingNumber, isAlreadyParticipating);
    }

    public int getLockWaitCount() {
        return lockWaitCounter.get();
    }

    public void resetLockWaitCount() {
        lockWaitCounter.set(0);
    }

    private static class ParticipationInfo {
        final int confirmedCount;
        final int maxWaitingNumber;
        final boolean isAlreadyParticipating;

        ParticipationInfo(int confirmedCount, int maxWaitingNumber, boolean isAlreadyParticipating) {
            this.confirmedCount = confirmedCount;
            this.maxWaitingNumber = maxWaitingNumber;
            this.isAlreadyParticipating = isAlreadyParticipating;
        }
    }
}
