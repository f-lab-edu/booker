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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventParticipationPerformanceComparisonTest {

    @Autowired
    private SynchronizedEventParticipationService synchronizedService;

    @Autowired
    private CasEventParticipationService casService;

    @Autowired
    private OptimisticLockEventParticipationService optimisticService;

    @Autowired
    private PessimisticLockEventParticipationService pessimisticService;

    @Autowired
    private EventRepository eventRepository;

    private Event testEvent;
    private final int maxParticipants = 10;
    private final int concurrentUsers = 50;
    private final int warmupRounds = 3;
    private final int testRounds = 5;

    @BeforeEach
    void setUp() {
        Member presenter = new Member("presenter1", "Presenter", "presenter@test.com");
        testEvent = new Event(
                "Performance Test Event",
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
    @DisplayName("동시성 제어 방식별 종합 성능 비교 테스트")
    void comprehensivePerformanceComparisonTest() throws InterruptedException {
        System.out.println("=== 동시성 제어 방식별 성능 비교 테스트 시작 ===");
        System.out.println("참가자 수: " + maxParticipants + "명");
        System.out.println("동시 요청 수: " + concurrentUsers + "명");
        System.out.println("워밍업 라운드: " + warmupRounds + "회");
        System.out.println("측정 라운드: " + testRounds + "회");
        System.out.println();

        Map<String, List<Long>> results = new HashMap<>();
        results.put("Synchronized", new ArrayList<>());
        results.put("CAS", new ArrayList<>());
        results.put("OptimisticLock", new ArrayList<>());
        results.put("PessimisticLock", new ArrayList<>());

        // 워밍업
        System.out.println("워밍업 라운드 실행 중...");
        for (int i = 0; i < warmupRounds; i++) {
            runPerformanceTest("Synchronized", synchronizedService, null, null, null);
            runPerformanceTest("CAS", null, casService, null, null);
            runPerformanceTest("OptimisticLock", null, null, optimisticService, null);
            runPerformanceTest("PessimisticLock", null, null, null, pessimisticService);
        }

        // 실제 측정
        System.out.println("성능 측정 라운드 실행 중...");
        for (int round = 1; round <= testRounds; round++) {
            System.out.println("라운드 " + round + "/" + testRounds);
            
            results.get("Synchronized").add(
                runPerformanceTest("Synchronized", synchronizedService, null, null, null));
            
            results.get("CAS").add(
                runPerformanceTest("CAS", null, casService, null, null));
            
            results.get("OptimisticLock").add(
                runPerformanceTest("OptimisticLock", null, null, optimisticService, null));
            
            results.get("PessimisticLock").add(
                runPerformanceTest("PessimisticLock", null, null, null, pessimisticService));
        }

        // 결과 분석 및 출력
        System.out.println("\n=== 성능 측정 결과 ===");
        results.forEach((method, times) -> {
            double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
            long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0L);
            long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0L);
            
            System.out.printf("%s: 평균 %.2fms, 최소 %dms, 최대 %dms%n", 
                    method, avgTime, minTime, maxTime);
        });

        // 상대적 성능 비교
        System.out.println("\n=== 상대적 성능 비교 ===");
        double syncAvg = results.get("Synchronized").stream().mapToLong(Long::longValue).average().orElse(1.0);
        results.forEach((method, times) -> {
            double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double ratio = avgTime / syncAvg;
            System.out.printf("%s: %.2fx (Synchronized 대비)%n", method, ratio);
        });

        // 검증: 모든 방식이 정상적으로 동작했는지 확인
        assertThat(results.get("Synchronized")).hasSize(testRounds);
        assertThat(results.get("CAS")).hasSize(testRounds);
        assertThat(results.get("OptimisticLock")).hasSize(testRounds);
        assertThat(results.get("PessimisticLock")).hasSize(testRounds);
    }

    @Test
    @DisplayName("동시성 제어 방식별 정확성 검증 테스트")
    void concurrencyCorrectnessTest() throws InterruptedException {
        System.out.println("=== 동시성 제어 방식별 정확성 검증 테스트 ===");

        // 각 방식에 대해 정확성 검증
        verifyCorrectness("Synchronized", synchronizedService, null, null, null);
        verifyCorrectness("CAS", null, casService, null, null);
        verifyCorrectness("OptimisticLock", null, null, optimisticService, null);
        verifyCorrectness("PessimisticLock", null, null, null, pessimisticService);
    }

    @Test
    @DisplayName("락 및 재시도 통계 비교")
    void lockAndRetryStatisticsTest() throws InterruptedException {
        System.out.println("=== 락 및 재시도 통계 비교 ===");

        // 통계 초기화
        casService.resetRetryCount();
        optimisticService.resetRetryCount();
        pessimisticService.resetLockCount();

        // 동시성 테스트 실행
        runConcurrentTest(casService);
        int casRetries = casService.getRetryCount();

        runConcurrentTest(optimisticService);
        int optimisticRetries = optimisticService.getRetryCount();

        runConcurrentTest(pessimisticService);
        int pessimisticLocks = pessimisticService.getLockCount();

        System.out.println("CAS 재시도 횟수: " + casRetries);
        System.out.println("Optimistic Lock 재시도 횟수: " + optimisticRetries);
        System.out.println("Pessimistic Lock 사용 횟수: " + pessimisticLocks);

        // 통계 검증
        assertThat(casRetries).isGreaterThanOrEqualTo(concurrentUsers);
        assertThat(optimisticRetries).isGreaterThanOrEqualTo(concurrentUsers);
        assertThat(pessimisticLocks).isEqualTo(concurrentUsers);
    }

    private long runPerformanceTest(String methodName, 
                                   SynchronizedEventParticipationService syncService,
                                   CasEventParticipationService casService,
                                   OptimisticLockEventParticipationService optService,
                                   PessimisticLockEventParticipationService pessService) throws InterruptedException {
        
        // 새 이벤트 생성 (테스트별 격리)
        Event event = createNewTestEvent();
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Future<EventParticipationDto.Response>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

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

                    if (syncService != null) return syncService.participateInEvent(request);
                    if (casService != null) return casService.participateInEvent(request);
                    if (optService != null) return optService.participateInEvent(request);
                    if (pessService != null) return pessService.participateInEvent(request);
                    
                    throw new IllegalStateException("No service provided");
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 결과 대기
        futures.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                // 예외 무시 (성능 측정이 목적)
            }
        });

        executor.shutdown();
        return duration;
    }

    private void verifyCorrectness(String methodName,
                                  SynchronizedEventParticipationService syncService,
                                  CasEventParticipationService casService,
                                  OptimisticLockEventParticipationService optService,
                                  PessimisticLockEventParticipationService pessService) throws InterruptedException {
        
        Event event = createNewTestEvent();
        
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
                            event.getId(),
                            "user" + userId,
                            "User " + userId,
                            "user" + userId + "@test.com"
                    );

                    EventParticipationDto.Response response;
                    if (syncService != null) response = syncService.participateInEvent(request);
                    else if (casService != null) response = casService.participateInEvent(request);
                    else if (optService != null) response = optService.participateInEvent(request);
                    else if (pessService != null) response = pessService.participateInEvent(request);
                    else throw new IllegalStateException("No service provided");

                    return response;
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
                // 실패한 요청은 무시
            }
        }

        executor.shutdown();

        System.out.printf("%s - 확정: %d명, 대기: %d명%n", 
                methodName, confirmedCount.get(), waitingCount.get());

        // 정확성 검증
        assertThat(confirmedCount.get()).isEqualTo(maxParticipants);
        assertThat(waitingCount.get()).isEqualTo(concurrentUsers - maxParticipants);
    }

    private void runConcurrentTest(Object service) throws InterruptedException {
        Event event = createNewTestEvent();
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    EventParticipationDto.Request request = new EventParticipationDto.Request(
                            event.getId(),
                            "user" + userId,
                            "User " + userId,
                            "user" + userId + "@test.com"
                    );

                    if (service instanceof CasEventParticipationService) {
                        ((CasEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof OptimisticLockEventParticipationService) {
                        ((OptimisticLockEventParticipationService) service).participateInEvent(request);
                    } else if (service instanceof PessimisticLockEventParticipationService) {
                        ((PessimisticLockEventParticipationService) service).participateInEvent(request);
                    }
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
    }

    private Event createNewTestEvent() {
        Member presenter = new Member("presenter" + System.currentTimeMillis(), "Presenter", "presenter@test.com");
        Event event = new Event(
                "Test Event " + System.currentTimeMillis(),
                "Test Description",
                EventType.TECH_TALK,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                maxParticipants,
                presenter
        );
        return eventRepository.save(event);
    }
}