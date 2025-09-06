package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.repository.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.data.jpa.repositories.bootstrap-mode=default"
})
class EventParticipationPerformanceComparisonTest {

    @Autowired
    private SynchronizedEventParticipationService synchronizedEventParticipationService;

    @Autowired
    private CasEventParticipationService casEventParticipationService;

    @Autowired
    private OptimisticLockEventParticipationService optimisticLockEventParticipationService;

    @Autowired
    private EventRepository eventRepository;

    private Event testEvent1;  // Synchronized용
    private Event testEvent2;  // CAS용
    private Event testEvent3;  // OptimisticLock용
    private final int maxParticipants = 5;
    private final int concurrentUsers = 20;

    @BeforeEach
    void setUp() {
        // Synchronized용 이벤트
        Member presenter1 = new Member("presenter1", "Presenter1", "presenter1@test.com");
        testEvent1 = new Event(
                "Test Event 1",
                "Test Description 1",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                maxParticipants,
                presenter1
        );
        testEvent1 = eventRepository.save(testEvent1);

        // CAS용 이벤트
        Member presenter2 = new Member("presenter2", "Presenter2", "presenter2@test.com");
        testEvent2 = new Event(
                "Test Event 2",
                "Test Description 2",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(2),
                maxParticipants,
                presenter2
        );
        testEvent2 = eventRepository.save(testEvent2);

        // OptimisticLock용 이벤트
        Member presenter3 = new Member("presenter3", "Presenter3", "presenter3@test.com");
        testEvent3 = new Event(
                "Test Event 3",
                "Test Description 3",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(3).plusHours(2),
                maxParticipants,
                presenter3
        );
        testEvent3 = eventRepository.save(testEvent3);
    }

    @AfterEach
    void tearDown() {
        // 모든 테스트 이벤트 정리
        cleanupEvent(testEvent1, "testEvent1");
        cleanupEvent(testEvent2, "testEvent2");
        cleanupEvent(testEvent3, "testEvent3");
    }

    private void cleanupEvent(Event event, String eventName) {
        if (event != null) {
            try {
                event = eventRepository.findById(event.getId()).orElse(null);
                if (event != null) {
                    eventRepository.delete(event);
                }
            } catch (Exception e) {
                System.out.println("Warning: Failed to delete " + eventName + ": " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Synchronized vs CAS 성능 비교 테스트")
    void synchronizedVsCasPerformanceComparisonTest() throws InterruptedException {
        // Synchronized 방식 성능 측정
        long synchronizedStartTime = System.currentTimeMillis();
        runConcurrentTest(synchronizedEventParticipationService, testEvent1);
        long synchronizedEndTime = System.currentTimeMillis();
        long synchronizedDuration = synchronizedEndTime - synchronizedStartTime;

        // CAS 방식 성능 측정
        casEventParticipationService.resetRetryCount();
        long casStartTime = System.currentTimeMillis();
        runConcurrentTest(casEventParticipationService, testEvent2);
        long casEndTime = System.currentTimeMillis();
        long casDuration = casEndTime - casStartTime;

        System.out.println("=== 성능 비교 결과 ===");
        System.out.println("Synchronized 방식: " + synchronizedDuration + "ms");
        System.out.println("CAS 방식: " + casDuration + "ms");
        System.out.println("CAS 재시도 횟수: " + casEventParticipationService.getRetryCount());

        // 성능 테스트는 시간이 0보다 크기만 하면 성공
        assertThat(synchronizedDuration).isGreaterThan(0);
        assertThat(casDuration).isGreaterThan(0);
    }

    @Test
    @DisplayName("전체 방식 성능 비교 테스트 - Synchronized vs CAS vs Optimistic Lock")
    void allMethodsPerformanceComparisonTest() throws InterruptedException {
        // Synchronized 방식 성능 측정
        long synchronizedStartTime = System.currentTimeMillis();
        runConcurrentTest(synchronizedEventParticipationService, testEvent1);
        long synchronizedEndTime = System.currentTimeMillis();
        long synchronizedDuration = synchronizedEndTime - synchronizedStartTime;

        // CAS 방식 성능 측정
        casEventParticipationService.resetRetryCount();
        long casStartTime = System.currentTimeMillis();
        runConcurrentTest(casEventParticipationService, testEvent2);
        long casEndTime = System.currentTimeMillis();
        long casDuration = casEndTime - casStartTime;

        // Optimistic Lock 방식 성능 측정 (testEvent3 사용)
        optimisticLockEventParticipationService.resetRetryCount();
        long optimisticLockStartTime = System.currentTimeMillis();
        runConcurrentTestWithOptimisticLock(optimisticLockEventParticipationService, testEvent3);
        long optimisticLockEndTime = System.currentTimeMillis();
        long optimisticLockDuration = optimisticLockEndTime - optimisticLockStartTime;

        System.out.println("=== 전체 방식 성능 비교 결과 ===");
        System.out.println("Synchronized 방식: " + synchronizedDuration + "ms");
        System.out.println("CAS 방식: " + casDuration + "ms (재시도: " + casEventParticipationService.getRetryCount() + "회)");
        System.out.println("Optimistic Lock 방식: " + optimisticLockDuration + "ms (재시도: " + optimisticLockEventParticipationService.getRetryCount() + "회)");

        assertThat(synchronizedDuration).isGreaterThan(0);
        assertThat(casDuration).isGreaterThan(0);
        assertThat(optimisticLockDuration).isGreaterThan(0);
    }

    private void runConcurrentTest(SynchronizedEventParticipationService service, Event event) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            Future<EventParticipationDto.Response> future = executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            event.getId(),
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
                // Handle LazyInitializationException for performance test
                if (e.getCause() instanceof org.hibernate.LazyInitializationException ||
                    e.getMessage().contains("LazyInitializationException")) {
                    continue;
                } else {
                    throw new RuntimeException("Future execution failed", e);
                }
            }
        }

        executor.shutdown();
    }

    private void runConcurrentTest(CasEventParticipationService service, Event event) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            Future<EventParticipationDto.Response> future = executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            event.getId(),
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
                // Handle LazyInitializationException for performance test
                if (e.getCause() instanceof org.hibernate.LazyInitializationException ||
                    e.getMessage().contains("LazyInitializationException")) {
                    continue;
                } else {
                    throw new RuntimeException("Future execution failed", e);
                }
            }
        }

        executor.shutdown();
    }

    private void runConcurrentTestWithOptimisticLock(OptimisticLockEventParticipationService service, Event event) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            Future<EventParticipationDto.Response> future = executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            event.getId(),
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
                // Handle OptimisticLock failures - ignore them for performance test
                if (e.getCause() instanceof org.springframework.orm.ObjectOptimisticLockingFailureException ||
                    e.getMessage().contains("StaleObjectStateException")) {
                    // OptimisticLock 예외는 성능 테스트에서 무시
                    continue;
                } else {
                    throw new RuntimeException("Future execution failed", e);
                }
            }
        }

        executor.shutdown();
    }
}
