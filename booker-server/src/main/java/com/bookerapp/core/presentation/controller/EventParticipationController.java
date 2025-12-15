package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.EventParticipationDto;
import com.bookerapp.core.application.service.CasEventParticipationService;
import com.bookerapp.core.application.service.SynchronizedEventParticipationService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(
        summary = "이벤트 참여 신청 (Synchronized)",
        description = "Synchronized 방식으로 이벤트 참여를 신청합니다.\n\n" +
                      "**동시성 제어 방식:**\n" +
                      "- Java의 synchronized 키워드를 사용하여 동시성 제어\n" +
                      "- 한 번에 하나의 스레드만 참여 처리 가능\n\n" +
                      "**응답 상태:**\n" +
                      "- CONFIRMED: 참여 확정\n" +
                      "- WAITING: 대기 중 (최대 인원 초과 시)\n\n" +
                      "**제약 조건:**\n" +
                      "- 동일한 참가자는 중복으로 신청할 수 없습니다.\n" +
                      "- 최대 참여자 수를 초과하면 대기 상태로 등록됩니다."
    )
    public ResponseEntity<EventParticipationDto.Response> participateWithSynchronized(
            @RequestBody EventParticipationDto.Request request,
            @RequestParam(required = false, defaultValue = "test-user") String userId) {

        log.info("Synchronized participation request from user: {}", userId);
        EventParticipationDto.Response response = synchronizedEventParticipationService.participateInEvent(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cas")
    @Operation(
        summary = "이벤트 참여 신청 (CAS)",
        description = "CAS (Compare-And-Swap) 방식으로 이벤트 참여를 신청합니다.\n\n" +
                      "**동시성 제어 방식:**\n" +
                      "- Optimistic Lock을 사용한 낙관적 동시성 제어\n" +
                      "- 충돌 발생 시 자동으로 재시도 (최대 10회)\n" +
                      "- 높은 처리량을 요구하는 환경에 적합\n\n" +
                      "**응답 상태:**\n" +
                      "- CONFIRMED: 참여 확정\n" +
                      "- WAITING: 대기 중 (최대 인원 초과 시)\n\n" +
                      "**성능 특징:**\n" +
                      "- Synchronized 방식보다 높은 동시 처리 성능\n" +
                      "- 충돌이 적은 환경에서 최적의 성능 발휘\n\n" +
                      "**제약 조건:**\n" +
                      "- 동일한 참가자는 중복으로 신청할 수 없습니다.\n" +
                      "- 최대 참여자 수를 초과하면 대기 상태로 등록됩니다."
    )
    public ResponseEntity<EventParticipationDto.Response> participateWithCas(
            @RequestBody EventParticipationDto.Request request,
            @RequestParam(required = false, defaultValue = "test-user") String userId) {

        log.info("CAS participation request from user: {}", userId);
        EventParticipationDto.Response response = casEventParticipationService.participateInEvent(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cas/retry-count")
    @Operation(
        summary = "CAS 재시도 횟수 조회",
        description = "CAS 방식 처리 중 발생한 총 재시도 횟수를 조회합니다.\n\n" +
                      "**용도:**\n" +
                      "- 동시성 제어 성능 모니터링\n" +
                      "- 충돌 빈도 분석\n" +
                      "- 부하 테스트 결과 확인"
    )
    public ResponseEntity<Integer> getCasRetryCount() {
        return ResponseEntity.ok(casEventParticipationService.getRetryCount());
    }

    @PostMapping("/cas/reset-retry-count")
    @Operation(
        summary = "CAS 재시도 횟수 초기화",
        description = "CAS 재시도 횟수를 0으로 초기화합니다.\n\n" +
                      "**용도:**\n" +
                      "- 새로운 테스트 시작 전 카운터 리셋\n" +
                      "- 성능 측정 구간 구분"
    )
    public ResponseEntity<Void> resetCasRetryCount() {
        casEventParticipationService.resetRetryCount();
        return ResponseEntity.ok().build();
    }
}
