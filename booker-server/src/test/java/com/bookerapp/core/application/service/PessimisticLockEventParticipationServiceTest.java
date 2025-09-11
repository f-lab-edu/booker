package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PessimisticLockEventParticipationServiceTest {

    @Autowired
    private PessimisticLockEventParticipationService pessimisticLockService;

    @Autowired
    private EventRepository eventRepository;

    private Event testEvent;
    private final int maxParticipants = 5;
    private final int concurrentUsers = 20;

    @BeforeEach
    void setUp() {
        Member presenter = new Member("presenter1", "Presenter", "presenter@test.com");
        testEvent = new Event(
                "Pessimistic Lock Test Event",
                "Test Description",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                maxParticipants,
                presenter
        );
        testEvent = eventRepository.save(testEvent);
        pessimisticLockService.resetLockCount();
    }

    @Test
    @DisplayName("비관적 락 - 단일 사용자 이벤트 참여 성공")
    void singleUserParticipationSuccess() {
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                testEvent.getId(),
                "user1",
                "User 1",
                "user1@test.com"
        );

        EventParticipationDto.Response response = pessimisticLockService.participateInEvent(request);

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getMessage()).contains("참여가 확정되었습니다");
        assertThat(pessimisticLockService.getLockCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("비관적 락 - 동시 요청 처리 정확성 검증")
    void concurrentParticipationAccuracy() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            Future<EventParticipationDto.Response> future = executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            testEvent.getId(),
                            "user" + userId,
                            "User " + userId,
                            "user" + userId + "@test.com"
                    );
                    return pessimisticLockService.participateInEvent(request);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(30, TimeUnit.SECONDS);

        for (Future<EventParticipationDto.Response> future : futures) {
            try {
                EventParticipationDto.Response response = future.get();
                if ("CONFIRMED".equals(response.getStatus())) {
                    confirmedCount.incrementAndGet();
                } else if ("WAITING".equals(response.getStatus())) {
                    waitingCount.incrementAndGet();
                }
            } catch (Exception e) {
                System.err.println("Request failed: " + e.getMessage());
            }
        }

        executor.shutdown();

        assertThat(confirmedCount.get()).isEqualTo(maxParticipants);
        assertThat(waitingCount.get()).isEqualTo(concurrentUsers - maxParticipants);
        assertThat(pessimisticLockService.getLockCount()).isEqualTo(concurrentUsers);

        System.out.println("확정 참가자: " + confirmedCount.get() + "명");
        System.out.println("대기자: " + waitingCount.get() + "명");
        System.out.println("락 사용 횟수: " + pessimisticLockService.getLockCount());
    }

    @Test
    @DisplayName("비관적 락 - 대기 순번 정확성 검증")
    void waitingOrderAccuracy() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            Future<EventParticipationDto.Response> future = executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            testEvent.getId(),
                            "user" + userId,
                            "User " + userId,
                            "user" + userId + "@test.com"
                    );
                    return pessimisticLockService.participateInEvent(request);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(30, TimeUnit.SECONDS);

        List<Integer> waitingNumbers = new ArrayList<>();
        for (Future<EventParticipationDto.Response> future : futures) {
            try {
                EventParticipationDto.Response response = future.get();
                if ("WAITING".equals(response.getStatus()) && response.getWaitingNumber() != null) {
                    waitingNumbers.add(response.getWaitingNumber());
                }
            } catch (Exception e) {
                System.err.println("Request failed: " + e.getMessage());
            }
        }

        executor.shutdown();

        assertThat(waitingNumbers).hasSize(concurrentUsers - maxParticipants);
        assertThat(waitingNumbers).containsExactlyInAnyOrder(
                java.util.stream.IntStream.rangeClosed(1, concurrentUsers - maxParticipants)
                        .boxed()
                        .toArray(Integer[]::new)
        );
    }

    @Test
    @DisplayName("비관적 락 - 중복 참여 방지")
    void duplicateParticipationPrevention() {
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                testEvent.getId(),
                "user1",
                "User 1",
                "user1@test.com"
        );

        EventParticipationDto.Response firstResponse = pessimisticLockService.participateInEvent(request);
        EventParticipationDto.Response secondResponse = pessimisticLockService.participateInEvent(request);

        assertThat(firstResponse.getStatus()).isEqualTo("CONFIRMED");
        assertThat(secondResponse.getStatus()).isEqualTo("ALREADY_PARTICIPATING");
        assertThat(secondResponse.getMessage()).contains("이미 참여 신청된 이벤트입니다");
    }

    @Test
    @DisplayName("비관적 락 - 순차 처리 검증")
    void sequentialProcessingVerification() throws InterruptedException {
        List<Long> timestamps = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            testEvent.getId(),
                            "user" + userId,
                            "User " + userId,
                            "user" + userId + "@test.com"
                    );
                    pessimisticLockService.participateInEvent(request);
                    long endTime = System.currentTimeMillis();
                    
                    synchronized (timestamps) {
                        timestamps.add(endTime - startTime);
                    }
                } catch (Exception e) {
                    System.err.println("Request failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("각 요청별 처리 시간: " + timestamps);
        
        assertThat(timestamps).hasSize(5);
        assertThat(pessimisticLockService.getLockCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("비관적 락 - 성능 측정")
    void performanceMeasurement() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            testEvent.getId(),
                            "user" + userId,
                            "User " + userId,
                            "user" + userId + "@test.com"
                    );
                    pessimisticLockService.participateInEvent(request);
                } catch (Exception e) {
                    // 예외 무시 (성능 측정이 목적)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        System.out.println("비관적 락 방식 - 처리 시간: " + duration + "ms");
        System.out.println("평균 처리 시간: " + (duration / (double) concurrentUsers) + "ms/request");
        System.out.println("락 사용 횟수: " + pessimisticLockService.getLockCount());

        assertThat(duration).isLessThan(30000); // 30초 이내 완료
        assertThat(pessimisticLockService.getLockCount()).isEqualTo(concurrentUsers);
    }
}