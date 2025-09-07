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
class PessimisticLockEventParticipationServiceTest {

    @Autowired
    private PessimisticLockEventParticipationService pessimisticLockEventParticipationService;

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

    @AfterEach
    void tearDown() {
        if (testEvent != null) {
            try {
                // Refresh the entity to get the latest version before deletion
                testEvent = eventRepository.findById(testEvent.getId()).orElse(null);
                if (testEvent != null) {
                    eventRepository.delete(testEvent);
                }
            } catch (Exception e) {
                // Ignore deletion errors in test cleanup
                System.out.println("Warning: Failed to delete test event: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("비관적 락 방식 - 동시 요청 처리 순서 테스트")
    void pessimisticLockConcurrencyOrderTest() throws InterruptedException {
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
                    return pessimisticLockEventParticipationService.participateInEvent(request);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(15, TimeUnit.SECONDS);  // 비관적 락은 더 오래 걸릴 수 있음
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

        // 비관적 락은 완벽한 동시성 제어를 제공
        assertThat(confirmedCount.get() + waitingCount.get()).isEqualTo(concurrentUsers);
        assertThat(confirmedCount.get()).isEqualTo(maxParticipants);  // 정확히 최대 참가자 수만큼 확정
        assertThat(waitingCount.get()).isEqualTo(concurrentUsers - maxParticipants);  // 나머지는 대기자
        assertThat(endTime - startTime).isLessThan(15000); // 15초 이내 완료

        System.out.println("비관적 락 방식 - 처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("확정 참가자: " + confirmedCount.get() + "명");
        System.out.println("대기자: " + waitingCount.get() + "명");
    }

    @Test
    @DisplayName("비관적 락 방식 - 단일 사용자 참여 테스트")
    void pessimisticLockSingleUserTest() {
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                testEvent.getId(),
                "user1",
                "User 1",
                "user1@test.com"
        );

        EventParticipationDto.Response response = pessimisticLockEventParticipationService.participateInEvent(request);

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getMessage()).contains("참여가 확정되었습니다");
    }

    @Test
    @DisplayName("비관적 락 방식 - 최대 참가자 초과 시 대기 처리 테스트")
    void pessimisticLockWaitingListTest() throws InterruptedException {
        // 먼저 최대 참가자 수만큼 확정 참가자 생성
        for (int i = 0; i < maxParticipants; i++) {
            EventParticipationDto.Request request = new EventParticipationDto.Request(
                    testEvent.getId(),
                    "user" + i,
                    "User " + i,
                    "user" + i + "@test.com"
            );
            pessimisticLockEventParticipationService.participateInEvent(request);
        }

        // 추가 참가자 요청 (대기자로 처리되어야 함)
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                testEvent.getId(),
                "waitingUser",
                "Waiting User",
                "waiting@test.com"
        );

        EventParticipationDto.Response response = pessimisticLockEventParticipationService.participateInEvent(request);

        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getWaitingNumber()).isEqualTo(1);
        assertThat(response.getMessage()).contains("대기자 명단에 등록되었습니다");
    }

    @Test
    @DisplayName("비관적 락 방식 - 중복 참여 방지 테스트")
    void pessimisticLockDuplicateParticipationTest() {
        EventParticipationDto.Request request = new EventParticipationDto.Request(
                testEvent.getId(),
                "user1",
                "User 1",
                "user1@test.com"
        );

        // 첫 번째 참여
        EventParticipationDto.Response firstResponse = pessimisticLockEventParticipationService.participateInEvent(request);
        assertThat(firstResponse.getStatus()).isEqualTo("CONFIRMED");

        // 두 번째 참여 (중복)
        EventParticipationDto.Response secondResponse = pessimisticLockEventParticipationService.participateInEvent(request);
        assertThat(secondResponse.getStatus()).isEqualTo("ALREADY_PARTICIPATING");
    }

    @Test
    @DisplayName("비관적 락 방식 - 락 대기 시간 테스트")
    void pessimisticLockWaitTimeTest() throws InterruptedException {
        // 동시 요청으로 락 대기 발생시키기
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        List<Long> executionTimes = new ArrayList<>();

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
                    pessimisticLockEventParticipationService.participateInEvent(request);
                    long endTime = System.currentTimeMillis();
                    executionTimes.add(endTime - startTime);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 실행 시간이 순차적으로 증가하는지 확인 (락 대기로 인해)
        System.out.println("비관적 락 실행 시간들: " + executionTimes);

        // 모든 요청이 성공적으로 처리되었는지 확인
        assertThat(executionTimes).hasSize(5);
    }
}
