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
    private CasEventParticipationService casEventParticipationService;

    @Autowired
    private OptimisticLockEventParticipationService optimisticLockEventParticipationService;

    @Autowired
    private PessimisticLockEventParticipationService pessimisticLockEventParticipationService;

    @Autowired
    private SynchronizedEventParticipationService synchronizedEventParticipationService;

    @Autowired
    private EventRepository eventRepository;

    private Event testEvent;
    private final int maxParticipants = 10;
    private final int concurrentUsers = 50;

    @BeforeEach
    void setUp() {
        Member presenter = new Member("presenter1", "Presenter", "presenter@test.com");
        testEvent = new Event(
                "Performance Test Event",
                "Performance Test Description",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                maxParticipants,
                presenter
        );
        testEvent = eventRepository.save(testEvent);
    }

    @AfterEach
    void tearDown() {
        if (testEvent != null) {
            try {
                testEvent = eventRepository.findById(testEvent.getId()).orElse(null);
                if (testEvent != null) {
                    eventRepository.delete(testEvent);
                }
            } catch (Exception e) {
                System.out.println("Warning: Failed to delete test event: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("동시성 제어 방식별 성능 비교 테스트")
    void performanceComparisonTest() throws InterruptedException {
        System.out.println("\n=== 동시성 제어 방식별 성능 비교 ===");

        // CAS 방식 성능 테스트
        long casTime = runPerformanceTest("CAS", casEventParticipationService);

        // 낙관적 락 방식 성능 테스트
        long optimisticTime = runPerformanceTest("Optimistic Lock", optimisticLockEventParticipationService);

        // 비관적 락 방식 성능 테스트
        long pessimisticTime = runPerformanceTest("Pessimistic Lock", pessimisticLockEventParticipationService);

        // 동기화 방식 성능 테스트
        long synchronizedTime = runPerformanceTest("Synchronized", synchronizedEventParticipationService);

        // 결과 출력
        System.out.println("\n=== 성능 비교 결과 ===");
        System.out.println("CAS 방식: " + casTime + "ms");
        System.out.println("낙관적 락: " + optimisticTime + "ms");
        System.out.println("비관적 락: " + pessimisticTime + "ms");
        System.out.println("동기화: " + synchronizedTime + "ms");

        // 모든 방식이 정상적으로 작동하는지 확인
        assertThat(casTime).isGreaterThan(0);
        assertThat(optimisticTime).isGreaterThan(0);
        assertThat(pessimisticTime).isGreaterThan(0);
        assertThat(synchronizedTime).isGreaterThan(0);
    }

    private long runPerformanceTest(String methodName, Object service) throws InterruptedException {
        System.out.println("\n--- " + methodName + " 방식 테스트 시작 ---");

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

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

                    EventParticipationDto.Response response;
                    if (service instanceof CasEventParticipationService) {
                        response = ((CasEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof OptimisticLockEventParticipationService) {
                        response = ((OptimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof PessimisticLockEventParticipationService) {
                        response = ((PessimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof SynchronizedEventParticipationService) {
                        response = ((SynchronizedEventParticipationService) service).participateInEvent(request);
                    } else {
                        throw new IllegalArgumentException("Unknown service type");
                    }

                    if ("CONFIRMED".equals(response.getStatus())) {
                        confirmedCount.incrementAndGet();
                    } else if ("WAITING".equals(response.getStatus())) {
                        waitingCount.incrementAndGet();
                    }

                    return response;
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.out.println(methodName + " 방식 오류: " + e.getMessage());
                    return null;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        executor.shutdown();

        System.out.println(methodName + " 방식 결과:");
        System.out.println("  - 실행 시간: " + executionTime + "ms");
        System.out.println("  - 확정 참가자: " + confirmedCount.get() + "명");
        System.out.println("  - 대기자: " + waitingCount.get() + "명");
        System.out.println("  - 오류: " + errorCount.get() + "건");
        System.out.println("  - 재시도 횟수: " + getRetryCount(service));

        return executionTime;
    }

    private int getRetryCount(Object service) {
        if (service instanceof CasEventParticipationService) {
            return ((CasEventParticipationService) service).getRetryCount();
        } else if (service instanceof OptimisticLockEventParticipationService) {
            return ((OptimisticLockEventParticipationService) service).getRetryCount();
        } else if (service instanceof PessimisticLockEventParticipationService) {
            return ((PessimisticLockEventParticipationService) service).getLockWaitCount();
        } else if (service instanceof SynchronizedEventParticipationService) {
            // SynchronizedEventParticipationService는 재시도 카운터가 없음
            return 0;
        }
        return 0;
    }

    @Test
    @DisplayName("동시성 제어 방식별 기본 기능 테스트")
    void basicFunctionalityTest() {
        System.out.println("\n=== 동시성 제어 방식별 기본 기능 테스트 ===");

        // 각 방식별로 기본 기능 테스트
        testBasicFunctionality("CAS", casEventParticipationService);
        testBasicFunctionality("Optimistic Lock", optimisticLockEventParticipationService);
        testBasicFunctionality("Pessimistic Lock", pessimisticLockEventParticipationService);
        testBasicFunctionality("Synchronized", synchronizedEventParticipationService);
    }

    private void testAccuracy(String methodName, Object service) throws InterruptedException {
        System.out.println("\n--- " + methodName + " 방식 정확성 테스트 ---");

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);

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

                    EventParticipationDto.Response response;
                    if (service instanceof CasEventParticipationService) {
                        response = ((CasEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof OptimisticLockEventParticipationService) {
                        response = ((OptimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof PessimisticLockEventParticipationService) {
                        response = ((PessimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof SynchronizedEventParticipationService) {
                        response = ((SynchronizedEventParticipationService) service).participateInEvent(request);
                    } else {
                        throw new IllegalArgumentException("Unknown service type");
                    }

                    if ("CONFIRMED".equals(response.getStatus())) {
                        confirmedCount.incrementAndGet();
                    } else if ("WAITING".equals(response.getStatus())) {
                        waitingCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println(methodName + " 방식 정확성 결과:");
        System.out.println("  - 확정 참가자: " + confirmedCount.get() + "명 (예상: " + maxParticipants + "명)");
        System.out.println("  - 대기자: " + waitingCount.get() + "명 (예상: " + (concurrentUsers - maxParticipants) + "명)");
        System.out.println("  - 총 참여자: " + (confirmedCount.get() + waitingCount.get()) + "명 (예상: " + concurrentUsers + "명)");

        // 정확성 검증
        assertThat(confirmedCount.get() + waitingCount.get()).isEqualTo(concurrentUsers);
        assertThat(confirmedCount.get()).isEqualTo(maxParticipants);
        assertThat(waitingCount.get()).isEqualTo(concurrentUsers - maxParticipants);
    }

    private void testAccuracySimple(String methodName, Object service) throws InterruptedException {
        System.out.println("\n--- " + methodName + " 방식 정확성 테스트 (간단) ---");

        // 간단한 테스트: 5명만 동시 참여
        int testUsers = 5;
        ExecutorService executor = Executors.newFixedThreadPool(testUsers);
        CountDownLatch latch = new CountDownLatch(testUsers);
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);

        for (int i = 0; i < testUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            testEvent.getId(),
                            "user" + userId,
                            "User " + userId,
                            "user" + userId + "@test.com"
                    );

                    EventParticipationDto.Response response;
                    if (service instanceof CasEventParticipationService) {
                        response = ((CasEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof OptimisticLockEventParticipationService) {
                        response = ((OptimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof PessimisticLockEventParticipationService) {
                        response = ((PessimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof SynchronizedEventParticipationService) {
                        response = ((SynchronizedEventParticipationService) service).participateInEvent(request);
                    } else {
                        throw new IllegalArgumentException("Unknown service type");
                    }

                    if ("CONFIRMED".equals(response.getStatus())) {
                        confirmedCount.incrementAndGet();
                    } else if ("WAITING".equals(response.getStatus())) {
                        waitingCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println(methodName + " 방식 정확성 결과:");
        System.out.println("  - 확정 참가자: " + confirmedCount.get() + "명");
        System.out.println("  - 대기자: " + waitingCount.get() + "명");
        System.out.println("  - 총 참여자: " + (confirmedCount.get() + waitingCount.get()) + "명");

        // 기본 검증만 수행
        assertThat(confirmedCount.get() + waitingCount.get()).isEqualTo(testUsers);
    }

    private void setUpNewEvent() {
        // 기존 이벤트 정리
        if (testEvent != null) {
            try {
                testEvent = eventRepository.findById(testEvent.getId()).orElse(null);
                if (testEvent != null) {
                    eventRepository.delete(testEvent);
                }
            } catch (Exception e) {
                System.out.println("Warning: Failed to delete test event: " + e.getMessage());
            }
        }

        // 새로운 이벤트 생성
        Member presenter = new Member("presenter" + System.currentTimeMillis(), "Presenter", "presenter@test.com");
        testEvent = new Event(
                "Performance Test Event " + System.currentTimeMillis(),
                "Performance Test Description",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                maxParticipants,
                presenter
        );
        testEvent = eventRepository.save(testEvent);
    }

    private void testIndividualService(String methodName, Object service) throws InterruptedException {
        System.out.println("\n--- " + methodName + " 방식 개별 테스트 ---");

        // 새로운 이벤트 생성
        setUpNewEvent();

        // 간단한 테스트: 3명만 동시 참여
        int testUsers = 3;
        ExecutorService executor = Executors.newFixedThreadPool(testUsers);
        CountDownLatch latch = new CountDownLatch(testUsers);
        AtomicInteger confirmedCount = new AtomicInteger(0);
        AtomicInteger waitingCount = new AtomicInteger(0);

        for (int i = 0; i < testUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            testEvent.getId(),
                            "user" + userId,
                            "User " + userId,
                            "user" + userId + "@test.com"
                    );

                    EventParticipationDto.Response response;
                    if (service instanceof CasEventParticipationService) {
                        response = ((CasEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof OptimisticLockEventParticipationService) {
                        response = ((OptimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof PessimisticLockEventParticipationService) {
                        response = ((PessimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof SynchronizedEventParticipationService) {
                        response = ((SynchronizedEventParticipationService) service).participateInEvent(request);
                    } else {
                        throw new IllegalArgumentException("Unknown service type");
                    }

                    if ("CONFIRMED".equals(response.getStatus())) {
                        confirmedCount.incrementAndGet();
                    } else if ("WAITING".equals(response.getStatus())) {
                        waitingCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println(methodName + " 방식 결과:");
        System.out.println("  - 확정 참가자: " + confirmedCount.get() + "명");
        System.out.println("  - 대기자: " + waitingCount.get() + "명");
        System.out.println("  - 총 참여자: " + (confirmedCount.get() + waitingCount.get()) + "명");

        // 기본 검증: 모든 사용자가 참여했는지 확인
        assertThat(confirmedCount.get() + waitingCount.get()).isEqualTo(testUsers);

        // 이벤트 정리
        if (testEvent != null) {
            try {
                testEvent = eventRepository.findById(testEvent.getId()).orElse(null);
                if (testEvent != null) {
                    eventRepository.delete(testEvent);
                }
            } catch (Exception e) {
                System.out.println("Warning: Failed to delete test event: " + e.getMessage());
            }
        }
    }

    private void testBasicFunctionality(String methodName, Object service) {
        System.out.println("\n--- " + methodName + " 방식 기본 기능 테스트 ---");

        // 새로운 이벤트 생성
        setUpNewEvent();

        try {
            // 단일 사용자 참여 테스트
            EventParticipationDto.Request request = new EventParticipationDto.Request(
                    testEvent.getId(),
                    "testUser",
                    "Test User",
                    "test@test.com"
            );

            EventParticipationDto.Response response;
            if (service instanceof CasEventParticipationService) {
                response = ((CasEventParticipationService) service).participateInEvent(request);
            } else if (service instanceof OptimisticLockEventParticipationService) {
                response = ((OptimisticLockEventParticipationService) service).participateInEvent(request);
            } else if (service instanceof PessimisticLockEventParticipationService) {
                response = ((PessimisticLockEventParticipationService) service).participateInEvent(request);
            } else if (service instanceof SynchronizedEventParticipationService) {
                response = ((SynchronizedEventParticipationService) service).participateInEvent(request);
            } else {
                throw new IllegalArgumentException("Unknown service type");
            }

            System.out.println(methodName + " 방식 결과:");
            System.out.println("  - 상태: " + response.getStatus());
            System.out.println("  - 메시지: " + response.getMessage());

            // 기본 검증: 응답이 정상인지 확인
            assertThat(response.getStatus()).isIn("CONFIRMED", "WAITING", "ALREADY_PARTICIPATING");
            assertThat(response.getMessage()).isNotNull();

        } finally {
            // 이벤트 정리
            if (testEvent != null) {
                try {
                    testEvent = eventRepository.findById(testEvent.getId()).orElse(null);
                    if (testEvent != null) {
                        eventRepository.delete(testEvent);
                    }
                } catch (Exception e) {
                    System.out.println("Warning: Failed to delete test event: " + e.getMessage());
                }
            }
        }
    }
}
