package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.dto.CasRetryCountDto;
import com.bookerapp.core.domain.model.dto.EventParticipationDto;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.repository.EventParticipationRepository;
import com.bookerapp.core.domain.repository.EventRepository;
import com.bookerapp.core.domain.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Event Participation Service
 * 이벤트 참여 신청 비즈니스 로직 처리
 *
 * 동시성 제어 전략:
 * 1. Synchronized: 메서드 레벨 동기화 + Pessimistic Lock
 * 2. CAS (Compare-And-Swap): Optimistic Lock + Retry 메커니즘
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventParticipationService {

    private final EventParticipationRepository participationRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;

    // CAS 재시도 횟수 저장 (thread-safe)
    private final AtomicInteger casRetryCount = new AtomicInteger(0);

    // 최대 재시도 횟수
    private static final int MAX_RETRIES = 10;

    /**
     * Synchronized 방식 참여 신청
     *
     * Pessimistic Lock을 사용하여 이벤트 엔티티를 잠금 후 처리
     * synchronized 키워드로 메서드 레벨에서 동시성 제어
     *
     * 장점: 확실한 동시성 제어, 재시도 불필요
     * 단점: 성능 저하 (단일 스레드 처리)
     *
     * @param eventId 이벤트 ID
     * @param request 참여 신청 요청 (memberId 포함)
     * @return 참여 신청 응답
     * @throws IllegalStateException 이미 참여한 이벤트인 경우
     * @throws EntityNotFoundException 이벤트 또는 회원을 찾을 수 없는 경우
     */
    @Transactional
    public synchronized EventParticipationDto.Response participateWithSynchronized(
            Long eventId, EventParticipationDto.Request request) {

        log.info("Starting synchronized participation - EventId: {}, MemberId: {}",
                 eventId, request.getMemberId());

        // 1. 중복 참여 검증
        validateDuplicateParticipation(eventId, request.getMemberId());

        // 2. 이벤트 조회 (Pessimistic Lock)
        Event event = eventRepository.findWithPessimisticLockById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("이벤트를 찾을 수 없습니다: " + eventId));

        // 3. 회원 조회
        Member member = memberRepository.findByMemberId(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + request.getMemberId()));

        // 4. 참여자 추가 (Event 엔티티 내부 로직 활용)
        event.addParticipant(member);

        // 5. 변경사항 저장
        eventRepository.save(event);

        // 6. 생성된 참여 정보 조회 및 응답 생성
        EventParticipation participation = findLatestParticipation(eventId, request.getMemberId());

        log.info("Synchronized participation successful - ParticipationId: {}, Status: {}",
                 participation.getId(), participation.getStatus());

        return EventParticipationDto.Response.from(participation, "SYNCHRONIZED");
    }

    /**
     * CAS 방식 참여 신청
     *
     * Optimistic Lock을 사용하여 버전 충돌 시 재시도
     * BaseEntity의 @Version 필드를 활용한 낙관적 잠금
     *
     * 장점: 높은 처리량, 낮은 경합 상황에서 효율적
     * 단점: 재시도 필요, 높은 경합 시 실패 가능성
     *
     * @param eventId 이벤트 ID
     * @param request 참여 신청 요청 (memberId 포함)
     * @return 참여 신청 응답
     * @throws IllegalStateException 최대 재시도 횟수 초과 또는 이미 참여한 경우
     * @throws EntityNotFoundException 이벤트 또는 회원을 찾을 수 없는 경우
     */
    @Transactional
    public EventParticipationDto.Response participateWithCAS(
            Long eventId, EventParticipationDto.Request request) {

        log.info("Starting CAS participation - EventId: {}, MemberId: {}",
                 eventId, request.getMemberId());

        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                // 1. 중복 참여 검증
                validateDuplicateParticipation(eventId, request.getMemberId());

                // 2. 이벤트 조회 (Optimistic Lock - @Version 활용)
                Event event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new EntityNotFoundException("이벤트를 찾을 수 없습니다: " + eventId));

                // 3. 회원 조회
                Member member = memberRepository.findByMemberId(request.getMemberId())
                        .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + request.getMemberId()));

                // 4. 참여자 추가
                event.addParticipant(member);

                // 5. 저장 (버전 충돌 가능 - OptimisticLockException)
                eventRepository.save(event);

                // 6. 성공 시 응답 생성
                EventParticipation participation = findLatestParticipation(eventId, request.getMemberId());

                // 재시도 횟수 누적 (성공 시에만)
                casRetryCount.addAndGet(attempt);

                log.info("CAS participation successful - ParticipationId: {}, Status: {}, Attempts: {}",
                         participation.getId(), participation.getStatus(), attempt);

                return EventParticipationDto.Response.from(participation, "CAS");

            } catch (OptimisticLockException e) {
                attempt++;
                casRetryCount.incrementAndGet();

                log.warn("CAS participation conflict detected - Attempt: {}/{}, EventId: {}",
                         attempt, MAX_RETRIES, eventId);

                if (attempt >= MAX_RETRIES) {
                    log.error("CAS participation failed - Max retries exceeded: {}", MAX_RETRIES);
                    throw new IllegalStateException(
                            "CAS 재시도 횟수 초과: " + MAX_RETRIES + "회. 나중에 다시 시도해주세요.");
                }

                // 재시도 전 대기 (Exponential Backoff)
                try {
                    long sleepTime = 50L * attempt;
                    Thread.sleep(sleepTime);
                    log.debug("CAS retry backoff - Sleeping for {}ms", sleepTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("CAS retry interrupted", ie);
                    throw new IllegalStateException("재시도 중 인터럽트 발생", ie);
                }
            }
        }

        throw new IllegalStateException("예상치 못한 오류 발생");
    }

    /**
     * CAS 재시도 횟수 조회
     *
     * @return 재시도 횟수 정보
     */
    @Transactional(readOnly = true)
    public CasRetryCountDto getCasRetryCount() {
        int count = casRetryCount.get();
        LocalDateTime now = LocalDateTime.now();

        log.info("CAS retry count queried - Count: {}, Time: {}", count, now);

        return new CasRetryCountDto(count, now);
    }

    /**
     * CAS 재시도 횟수 초기화
     *
     * @return 초기화된 재시도 횟수 정보
     */
    public CasRetryCountDto resetCasRetryCount() {
        casRetryCount.set(0);
        LocalDateTime now = LocalDateTime.now();

        log.info("CAS retry count reset - Time: {}", now);

        return new CasRetryCountDto(0, now, "CAS retry count has been reset");
    }

    /**
     * 중복 참여 검증
     *
     * @param eventId 이벤트 ID
     * @param memberId 회원 ID
     * @throws IllegalStateException 이미 참여한 경우
     */
    private void validateDuplicateParticipation(Long eventId, String memberId) {
        if (participationRepository.existsByEventIdAndParticipantMemberId(eventId, memberId)) {
            log.warn("Duplicate participation attempt - EventId: {}, MemberId: {}", eventId, memberId);
            throw new IllegalStateException("이미 참여한 이벤트입니다.");
        }
    }

    /**
     * 최신 참여 정보 조회
     *
     * @param eventId 이벤트 ID
     * @param memberId 회원 ID
     * @return 참여 정보
     * @throws EntityNotFoundException 참여 내역을 찾을 수 없는 경우
     */
    private EventParticipation findLatestParticipation(Long eventId, String memberId) {
        return participationRepository.findByEventIdAndParticipantMemberId(eventId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("참여 내역을 찾을 수 없습니다."));
    }
}
