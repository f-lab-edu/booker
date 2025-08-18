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
class EventParticipationConcurrencyTest {

    @Autowired
    private SynchronizedEventParticipationService synchronizedEventParticipationService;

    @Autowired
    private CasEventParticipationService casEventParticipationService;

    @Autowired
    private EventRepository eventRepository;

    private Event testEvent;
    private final int maxParticipants = 5;
    private final int concurrentUsers = 20;

    @BeforeEach
    void setUp() {
        Member presenter = new Member("presenter1", "Presenter", "presenter@test.com");
        testEvent = new Event(
                "Test Event",
                "Test Description",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                maxParticipants,
                presenter
        );
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    @DisplayName("Synchronized 방식 - 동시 요청 처리 순서 테스트")
    void synchronizedConcurrencyOrderTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

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
                    return synchronizedEventParticipationService.participateInEvent(request);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        for (Future<EventParticipationDto.Response> future : futures) {
            try {
                EventParticipationDto.Response response = future.get();
                if ("CONFIRMED".equals(response.getStatus())) {
                    confirmedCount.incrementAndGet();
                } else if ("WAITING".equals(response.getStatus())) {
                    waitingCount.incrementAndGet();
                }
            } catch (Exception e) {
                throw new RuntimeException("Future execution failed", e);
            }
        }

        executor.shutdown();

        assertThat(confirmedCount.get()).isEqualTo(maxParticipants);
        assertThat(waitingCount.get()).isEqualTo(concurrentUsers - maxParticipants);
        assertThat(endTime - startTime).isLessThan(10000); // 10초 이내 완료

        System.out.println("Synchronized 방식 - 처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("확정 참가자: " + confirmedCount.get() + "명");
        System.out.println("대기자: " + waitingCount.get() + "명");
    }

    @Test
    @DisplayName("CAS 방식 - 동시 요청 처리 순서 테스트")
    void casConcurrencyOrderTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

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
                    return casEventParticipationService.participateInEvent(request);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        for (Future<EventParticipationDto.Response> future : futures) {
            try {
                EventParticipationDto.Response response = future.get();
                if ("CONFIRMED".equals(response.getStatus())) {
                    confirmedCount.incrementAndGet();
                } else if ("WAITING".equals(response.getStatus())) {
                    waitingCount.incrementAndGet();
                }
            } catch (Exception e) {
                throw new RuntimeException("Future execution failed", e);
            }
        }

        executor.shutdown();

        assertThat(confirmedCount.get()).isEqualTo(maxParticipants);
        assertThat(waitingCount.get()).isEqualTo(concurrentUsers - maxParticipants);
        assertThat(endTime - startTime).isLessThan(10000); // 10초 이내 완료

        System.out.println("CAS 방식 - 처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("확정 참가자: " + confirmedCount.get() + "명");
        System.out.println("대기자: " + waitingCount.get() + "명");
        System.out.println("재시도 횟수: " + casEventParticipationService.getRetryCount());
    }

    @Test
    @DisplayName("Synchronized vs CAS 성능 비교 테스트")
    void performanceComparisonTest() throws InterruptedException {
        // Synchronized 방식 성능 측정
        long synchronizedStartTime = System.currentTimeMillis();
        runConcurrentTest(synchronizedEventParticipationService);
        long synchronizedEndTime = System.currentTimeMillis();
        long synchronizedDuration = synchronizedEndTime - synchronizedStartTime;

        // CAS 방식 성능 측정
        casEventParticipationService.resetRetryCount();
        long casStartTime = System.currentTimeMillis();
        runConcurrentTest(casEventParticipationService);
        long casEndTime = System.currentTimeMillis();
        long casDuration = casEndTime - casStartTime;

        System.out.println("=== 성능 비교 결과 ===");
        System.out.println("Synchronized 방식: " + synchronizedDuration + "ms");
        System.out.println("CAS 방식: " + casDuration + "ms");
        System.out.println("CAS 재시도 횟수: " + casEventParticipationService.getRetryCount());

        // CAS 방식이 일반적으로 더 빠를 것으로 예상되지만,
        // 실제 환경에서는 다양한 요인에 따라 결과가 달라질 수 있음
        assertThat(synchronizedDuration).isGreaterThan(0);
        assertThat(casDuration).isGreaterThan(0);
    }

    @Test
    @DisplayName("대기 순번 정확성 테스트")
    void waitingOrderAccuracyTest() throws InterruptedException {
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
                    return synchronizedEventParticipationService.participateInEvent(request);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(10, TimeUnit.SECONDS);

        List<Integer> waitingNumbers = new ArrayList<>();
        for (Future<EventParticipationDto.Response> future : futures) {
            try {
                EventParticipationDto.Response response = future.get();
                if ("WAITING".equals(response.getStatus()) && response.getWaitingNumber() != null) {
                    waitingNumbers.add(response.getWaitingNumber());
                }
            } catch (Exception e) {
                throw new RuntimeException("Future execution failed", e);
            }
        }

        executor.shutdown();

        // 대기 순번이 1부터 시작하고 연속적이어야 함
        assertThat(waitingNumbers).hasSize(concurrentUsers - maxParticipants);
        assertThat(waitingNumbers).containsExactlyInAnyOrder(
                java.util.stream.IntStream.rangeClosed(1, concurrentUsers - maxParticipants)
                        .boxed()
                        .toArray(Integer[]::new)
        );
    }

    private void runConcurrentTest(SynchronizedEventParticipationService service) throws InterruptedException {
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
                    return service.participateInEvent(request);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(10, TimeUnit.SECONDS);

        for (Future<EventParticipationDto.Response> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("Future execution failed", e);
            }
        }

        executor.shutdown();
    }

    private void runConcurrentTest(CasEventParticipationService service) throws InterruptedException {
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
                    return service.participateInEvent(request);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(10, TimeUnit.SECONDS);

        for (Future<EventParticipationDto.Response> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("Future execution failed", e);
            }
        }

        executor.shutdown();
    }
}
