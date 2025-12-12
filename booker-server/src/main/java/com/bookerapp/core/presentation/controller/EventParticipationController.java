package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.application.service.CasEventParticipationService;
import com.bookerapp.core.application.service.SynchronizedEventParticipationService;
import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/participation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event Participation", description = "이벤트 참여 신청 API")
public class EventParticipationController {

    private final SynchronizedEventParticipationService synchronizedEventParticipationService;
    private final CasEventParticipationService casEventParticipationService;

    @PostMapping("/synchronized")
    @Operation(summary = "이벤트 참여 신청 (Synchronized)")
    public ResponseEntity<EventParticipationDto.Response> participateWithSynchronized(
            @RequestBody EventParticipationDto.Request request,
            @Parameter(hidden = true) UserContext userContext) {

        log.info("Synchronized participation request from user: {}", userContext.getUserId());
        EventParticipationDto.Response response = synchronizedEventParticipationService.participateInEvent(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cas")
    @Operation(summary = "이벤트 참여 신청 (CAS)")
    public ResponseEntity<EventParticipationDto.Response> participateWithCas(
            @RequestBody EventParticipationDto.Request request,
            @Parameter(hidden = true) UserContext userContext) {

        log.info("CAS participation request from user: {}", userContext.getUserId());
        EventParticipationDto.Response response = casEventParticipationService.participateInEvent(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cas/retry-count")
    @Operation(summary = "CAS 재시도 횟수 조회")
    public ResponseEntity<Integer> getCasRetryCount() {
        return ResponseEntity.ok(casEventParticipationService.getRetryCount());
    }

    @PostMapping("/cas/reset-retry-count")
    @Operation(summary = "CAS 재시도 횟수 초기화")
    public ResponseEntity<Void> resetCasRetryCount() {
        casEventParticipationService.resetRetryCount();
        return ResponseEntity.ok().build();
    }
}
