package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.application.dto.LoadTestDto;
import com.bookerapp.core.application.service.CasEventParticipationService;
import com.bookerapp.core.application.service.OptimisticLockEventParticipationService;
import com.bookerapp.core.application.service.PessimisticLockEventParticipationService;
import com.bookerapp.core.application.service.SynchronizedEventParticipationService;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.repository.EventRepository;
import com.bookerapp.core.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/load-test")
@RequiredArgsConstructor
@Slf4j
public class LoadTestController {

    private final OptimisticLockEventParticipationService optimisticLockService;
    private final PessimisticLockEventParticipationService pessimisticLockService;
    private final CasEventParticipationService casService;
    private final SynchronizedEventParticipationService synchronizedService;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;

    @PostMapping("/participate/optimistic")
    public ResponseEntity<EventParticipationDto.Response> participateOptimistic(
            @RequestBody LoadTestDto.ParticipationRequest request) {
        log.info("Load test - Optimistic lock participation for event: {}, user: {}", request.getEventId(), request.getUserId());
        EventParticipationDto.Response response = optimisticLockService.participateInEvent(request.toEventParticipationRequest());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/participate/pessimistic")
    public ResponseEntity<EventParticipationDto.Response> participatePessimistic(
            @RequestBody LoadTestDto.ParticipationRequest request) {
        log.info("Load test - Pessimistic lock participation for event: {}, user: {}", request.getEventId(), request.getUserId());
        EventParticipationDto.Response response = pessimisticLockService.participateInEvent(request.toEventParticipationRequest());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/participate/cas")
    public ResponseEntity<EventParticipationDto.Response> participateCas(
            @RequestBody LoadTestDto.ParticipationRequest request) {
        log.info("Load test - CAS participation for event: {}, user: {}", request.getEventId(), request.getUserId());
        EventParticipationDto.Response response = casService.participateInEvent(request.toEventParticipationRequest());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/participate/synchronized")
    public ResponseEntity<EventParticipationDto.Response> participateSynchronized(
            @RequestBody LoadTestDto.ParticipationRequest request) {
        log.info("Load test - Synchronized participation for event: {}, user: {}", request.getEventId(), request.getUserId());
        EventParticipationDto.Response response = synchronizedService.participateInEvent(request.toEventParticipationRequest());
        return ResponseEntity.ok(response);
    }

    // Metrics endpoints commented out - methods not available
    // @GetMapping("/metrics/optimistic")
    // public ResponseEntity<Map<String, Integer>> getOptimisticMetrics() {
    //     return ResponseEntity.ok(Map.of("retryCount", optimisticLockService.getRetryCount()));
    // }

    // @GetMapping("/metrics/cas")
    // public ResponseEntity<Map<String, Integer>> getCasMetrics() {
    //     return ResponseEntity.ok(Map.of("retryCount", casService.getRetryCount()));
    // }

    // @PostMapping("/reset/optimistic")
    // public ResponseEntity<String> resetOptimisticMetrics() {
    //     optimisticLockService.resetRetryCount();
    //     return ResponseEntity.ok("Optimistic lock retry count reset");
    // }

    // @PostMapping("/reset/cas")
    // public ResponseEntity<String> resetCasMetrics() {
    //     casService.resetRetryCount();
    //     return ResponseEntity.ok("CAS retry count reset");
    // }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Load test controller is healthy");
    }

    @PostMapping("/setup")
    public ResponseEntity<LoadTestDto.SetupResponse> setupTestData(
            @RequestBody LoadTestDto.SetupRequest request) {
        log.info("Setting up load test data for event: {} with {} max participants",
                request.getEventId(), request.getMaxParticipants());

        Event event = createTestEventIfNotExists(request);

        return ResponseEntity.ok(new LoadTestDto.SetupResponse(
                event.getId(),
                "Test data setup completed",
                request.getMaxParticipants()
        ));
    }

    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupTestData() {
        log.info("Cleaning up load test data");

        eventRepository.findAll().stream()
                .filter(event -> event.getTitle().startsWith("LoadTest"))
                .forEach(eventRepository::delete);

        return ResponseEntity.ok("Test data cleanup completed");
    }

    private Event createTestEventIfNotExists(LoadTestDto.SetupRequest request) {
        return eventRepository.findById(request.getEventId())
                .orElseGet(() -> {
                    Member presenter = findOrCreatePresenter();

                    Event event = new Event(
                            request.getEventTitle() != null ? request.getEventTitle() : "LoadTest Event",
                            "LoadTest용 이벤트입니다.",
                            EventType.TECH_TALK,
                            java.time.LocalDateTime.now().plusDays(1),
                            java.time.LocalDateTime.now().plusDays(1).plusHours(2),
                            request.getMaxParticipants() != null ? request.getMaxParticipants() : 10,
                            presenter
                    );

                    return eventRepository.save(event);
                });
    }

    private Member findOrCreatePresenter() {
        return memberRepository.findByMemberId("loadtest-presenter")
                .orElseGet(() -> {
                    Member presenter = new Member(
                            "loadtest-presenter",
                            "LoadTest Presenter",
                            "presenter@loadtest.com"
                    );
                    return memberRepository.save(presenter);
                });
    }
}