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

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasEventParticipationService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final AtomicInteger retryCounter = new AtomicInteger(0);
    private static final int MAX_RETRY_ATTEMPTS = 5;  // 재시도 횟수 감소
    private static final long BASE_RETRY_DELAY_MS = 1;  // 지연 시간 단축

    @Transactional
    public EventParticipationDto.Response participateInEvent(EventParticipationDto.Request request) {
        log.info("CAS participation request for event: {}, member: {}", request.getEventId(), request.getMemberId());

        for (int attempts = 0; attempts < MAX_RETRY_ATTEMPTS; attempts++) {
            try {
                return attemptParticipationWithOptimizedCas(request);
            } catch (ConcurrentModificationException e) {
                retryCounter.incrementAndGet();

                if (attempts == MAX_RETRY_ATTEMPTS - 1) {
                    log.error("Max CAS retries exceeded for event: {}, member: {}", request.getEventId(), request.getMemberId());
                    throw new RuntimeException("참여 신청 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
                }

                log.debug("CAS retry attempt {}/{} for event: {}", attempts + 1, MAX_RETRY_ATTEMPTS, request.getEventId());

                // 짧은 지연만 적용
                if (attempts > 0) {
                    try {
                        Thread.sleep(BASE_RETRY_DELAY_MS << attempts);  // 지수 백오프: 1, 2, 4ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("처리가 중단되었습니다.");
                    }
                }
            }
        }

        throw new RuntimeException("예상치 못한 오류가 발생했습니다.");
    }

    @Transactional
    private EventParticipationDto.Response attemptParticipationWithOptimizedCas(EventParticipationDto.Request request) {
        Event event = eventRepository.findById(request.getEventId())
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

        // CAS 연산: 확정 참여자 수 기준으로 원자적 판단
        EventParticipation participation;
        if (info.confirmedCount < event.getMaxParticipants()) {
            participation = new EventParticipation(event, member, ParticipationStatus.CONFIRMED);
            event.getParticipants().add(participation);

            // 저장 후 실제 확정자 수 검증 (CAS 검증)
            Event savedEvent = eventRepository.save(event);
            // LazyInitializationException 방지를 위해 participants 컬렉션 명시적 초기화
            savedEvent.getParticipants().size();
            int actualConfirmedCount = countConfirmedParticipants(savedEvent);

            if (actualConfirmedCount > event.getMaxParticipants()) {
                throw new ConcurrentModificationException("Concurrent participation detected");
            }

            log.info("Confirmed participation (CAS) - Event: {}, Member: {}", request.getEventId(), request.getMemberId());
            return new EventParticipationDto.Response(participation.getId(), "CONFIRMED", null, "참여가 확정되었습니다.");
        } else {
            int nextWaitingNumber = info.maxWaitingNumber + 1;
            participation = new EventParticipation(event, member, ParticipationStatus.WAITING, nextWaitingNumber);
            event.getParticipants().add(participation);

            eventRepository.save(event);

            log.info("Added to waiting list (CAS) - Event: {}, Member: {}, Waiting Number: {}",
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

    private int countConfirmedParticipants(Event event) {
        int count = 0;
        for (EventParticipation p : event.getParticipants()) {
            if (p.getStatus() == ParticipationStatus.CONFIRMED) {
                count++;
            }
        }
        return count;
    }

    public int getRetryCount() {
        return retryCounter.get();
    }

    public void resetRetryCount() {
        retryCounter.set(0);
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
