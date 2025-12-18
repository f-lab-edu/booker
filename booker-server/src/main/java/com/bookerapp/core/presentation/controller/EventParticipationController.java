package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.CasRetryCountDto;
import com.bookerapp.core.domain.model.dto.EventParticipationDto;
import com.bookerapp.core.domain.service.EventParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Event Participation Controller
 * 이벤트 참여 신청 API - 동시성 제어 학습용
 *
 * 제공 기능:
 * 1. Synchronized 방식 참여 신청 (Pessimistic Lock)
 * 2. CAS 방식 참여 신청 (Optimistic Lock + Retry)
 * 3. CAS 재시도 횟수 조회
 * 4. CAS 재시도 횟수 초기화
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/participations")
@RequiredArgsConstructor
@Tag(name = "5. Event Participation", description = "이벤트 참여 신청 API - 동시성 제어 학습용")
public class EventParticipationController {

    private final EventParticipationService participationService;

    /**
     * POST /api/v1/events/{eventId}/participations/synchronized
     * Synchronized 방식 이벤트 참여 신청
     */
    @PostMapping("/synchronized")
    @Operation(summary = "이벤트 참여 신청 (Synchronized)", description = """
            ## 개요
            Synchronized 방식으로 이벤트 참여를 신청합니다.
            이벤트 정원이 가득 찬 경우 자동으로 WAITING(대기) 상태로 등록되고,
            정원이 남은 경우 즉시 CONFIRMED(확정) 상태로 등록됩니다.

            ## 동시성 제어 방식
            - **Synchronized 블록**: 메서드 레벨에서 동기화 처리
            - **Pessimistic Lock**: 데이터베이스 행 수준 잠금 사용
            - **특징**: 확실한 동시성 제어, 성능은 상대적으로 낮음
            - **사용 사례**: 데이터 일관성이 절대적으로 중요한 경우

            ## 주요 파라미터
            - `eventId`: 참여할 이벤트 ID (Path Parameter, 필수)
            - `memberId`: 참여할 회원 ID (Request Body, 필수)

            ## 응답 데이터
            - `id`: 참여 신청 ID
            - `status`: CONFIRMED (즉시 확정) 또는 WAITING (대기 목록)
            - `waitingNumber`: 대기 순서 (WAITING인 경우에만 표시)
            - `strategy`: 사용된 전략 (SYNCHRONIZED)

            ## 제약사항
            - 인증 필요: Bearer Token (추후 구현 예정, 현재는 memberId로 대체)
            - 중복 참여 불가: 동일 이벤트에 이미 참여한 경우 400 에러
            - 최대 참여자 수 초과 시 자동으로 대기 목록 등록
            - Pessimistic Lock 사용으로 동시 접근 시 대기 발생 가능
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "참여 신청 성공 - Location 헤더에 생성된 리소스 URL 포함",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EventParticipationDto.Response.class),
                            examples = {
                                    @ExampleObject(name = "즉시 확정", summary = "정원이 남아 즉시 CONFIRMED 상태로 등록됨", value = """
                                            {
                                              "id": 1,
                                              "eventId": 1,
                                              "eventTitle": "Spring Boot 실전 가이드",
                                              "memberId": "test-member-001",
                                              "memberName": "홍길동",
                                              "status": "CONFIRMED",
                                              "registrationDate": "2025-12-18T10:30:00",
                                              "waitingNumber": null,
                                              "strategy": "SYNCHRONIZED"
                                            }
                                            """),
                                    @ExampleObject(name = "대기 목록", summary = "정원 초과로 WAITING 상태로 등록됨", value = """
                                            {
                                              "id": 2,
                                              "eventId": 1,
                                              "eventTitle": "Spring Boot 실전 가이드",
                                              "memberId": "test-member-002",
                                              "memberName": "김철수",
                                              "status": "WAITING",
                                              "registrationDate": "2025-12-18T10:35:00",
                                              "waitingNumber": 1,
                                              "strategy": "SYNCHRONIZED"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 이미 참여한 이벤트",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Bad Request\", \"message\": \"이미 참여한 이벤트입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "이벤트 또는 회원을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "이벤트 없음", value = "{\"error\": \"Not Found\", \"message\": \"이벤트를 찾을 수 없습니다: 999\"}"),
                                    @ExampleObject(name = "회원 없음", value = "{\"error\": \"Not Found\", \"message\": \"회원을 찾을 수 없습니다: invalid-member\"}")
                            })),
            @ApiResponse(responseCode = "422", description = "유효성 검증 실패",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "Validation Failed",
                                      "details": [
                                        {
                                          "field": "memberId",
                                          "message": "회원 ID는 필수입니다"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<EventParticipationDto.Response> participateWithSynchronized(
            @Parameter(description = "참여할 이벤트의 고유 ID - 실제 존재하는 이벤트 ID를 입력하세요",
                    example = "1", required = true)
            @PathVariable Long eventId,

            @RequestBody(description = "이벤트 참여 신청 요청 데이터", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventParticipationDto.Request.class),
                            examples = @ExampleObject(
                                    name = "참여 신청 예시",
                                    summary = "회원 ID로 이벤트 참여 신청",
                                    description = "실제 DB에 존재하는 회원 ID로 참여를 신청합니다",
                                    value = """
                                            {
                                              "memberId": "test-member-001"
                                            }
                                            """
                            )
                    ))
            @Valid @org.springframework.web.bind.annotation.RequestBody
            EventParticipationDto.Request request) {

        EventParticipationDto.Response response =
                participationService.participateWithSynchronized(eventId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/../{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * POST /api/v1/events/{eventId}/participations/cas
     * CAS 방식 이벤트 참여 신청
     */
    @PostMapping("/cas")
    @Operation(summary = "이벤트 참여 신청 (CAS)", description = """
            ## 개요
            CAS(Compare-And-Swap) 방식으로 이벤트 참여를 신청합니다.
            낙관적 잠금(Optimistic Lock)을 사용하여 버전 충돌 발생 시 최대 10회까지 재시도합니다.

            ## 동시성 제어 방식
            - **Optimistic Lock**: 버전 기반 충돌 감지 (@Version 필드 활용)
            - **Retry Mechanism**: 충돌 발생 시 자동 재시도 (최대 10회)
            - **Exponential Backoff**: 재시도 간 대기 시간 점진적 증가 (50ms * attempt)
            - **특징**: 높은 처리량, 낮은 경합 상황에서 효율적
            - **사용 사례**: 동시 접근이 많지만 충돌이 적은 경우

            ## 주요 파라미터
            - `eventId`: 참여할 이벤트 ID (Path Parameter, 필수)
            - `memberId`: 참여할 회원 ID (Request Body, 필수)

            ## 응답 데이터
            - `id`: 참여 신청 ID
            - `status`: CONFIRMED (즉시 확정) 또는 WAITING (대기 목록)
            - `waitingNumber`: 대기 순서 (WAITING인 경우에만 표시)
            - `strategy`: 사용된 전략 (CAS)

            ## 제약사항
            - 최대 재시도 횟수: 10회 (초과 시 409 에러)
            - 중복 참여 불가: 동일 이벤트에 이미 참여한 경우 400 에러
            - 재시도 횟수는 서버 전역으로 누적되며 `/cas/retry-count` 엔드포인트로 조회 가능
            - 높은 경합 상황에서는 실패 가능성 존재
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "참여 신청 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EventParticipationDto.Response.class),
                            examples = {
                                    @ExampleObject(name = "즉시 확정", summary = "정원이 남아 즉시 CONFIRMED 상태로 등록됨", value = """
                                            {
                                              "id": 3,
                                              "eventId": 1,
                                              "eventTitle": "Spring Boot 실전 가이드",
                                              "memberId": "test-member-003",
                                              "memberName": "이영희",
                                              "status": "CONFIRMED",
                                              "registrationDate": "2025-12-18T10:40:00",
                                              "waitingNumber": null,
                                              "strategy": "CAS"
                                            }
                                            """),
                                    @ExampleObject(name = "대기 목록", summary = "정원 초과로 WAITING 상태로 등록됨", value = """
                                            {
                                              "id": 4,
                                              "eventId": 1,
                                              "eventTitle": "Spring Boot 실전 가이드",
                                              "memberId": "test-member-004",
                                              "memberName": "박민수",
                                              "status": "WAITING",
                                              "registrationDate": "2025-12-18T10:45:00",
                                              "waitingNumber": 2,
                                              "strategy": "CAS"
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 이미 참여한 이벤트",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Bad Request\", \"message\": \"이미 참여한 이벤트입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "이벤트 또는 회원을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "이벤트 없음", value = "{\"error\": \"Not Found\", \"message\": \"이벤트를 찾을 수 없습니다: 999\"}"),
                                    @ExampleObject(name = "회원 없음", value = "{\"error\": \"Not Found\", \"message\": \"회원을 찾을 수 없습니다: invalid-member\"}")
                            })),
            @ApiResponse(responseCode = "409", description = "최대 재시도 횟수 초과",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Conflict\", \"message\": \"CAS 재시도 횟수 초과: 10회. 나중에 다시 시도해주세요.\"}"))),
            @ApiResponse(responseCode = "422", description = "유효성 검증 실패",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "Validation Failed",
                                      "details": [
                                        {
                                          "field": "memberId",
                                          "message": "회원 ID는 필수입니다"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<EventParticipationDto.Response> participateWithCAS(
            @Parameter(description = "참여할 이벤트의 고유 ID - 실제 존재하는 이벤트 ID를 입력하세요",
                    example = "1", required = true)
            @PathVariable Long eventId,

            @RequestBody(description = "이벤트 참여 신청 요청 데이터", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EventParticipationDto.Request.class),
                            examples = @ExampleObject(
                                    name = "참여 신청 예시",
                                    summary = "회원 ID로 이벤트 참여 신청",
                                    description = "실제 DB에 존재하는 회원 ID로 참여를 신청합니다",
                                    value = """
                                            {
                                              "memberId": "test-member-003"
                                            }
                                            """
                            )
                    ))
            @Valid @org.springframework.web.bind.annotation.RequestBody
            EventParticipationDto.Request request) {

        EventParticipationDto.Response response =
                participationService.participateWithCAS(eventId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/../{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * GET /api/v1/events/{eventId}/participations/cas/retry-count
     * CAS 재시도 횟수 조회
     */
    @GetMapping("/cas/retry-count")
    @Operation(summary = "CAS 재시도 횟수 조회", description = """
            ## 개요
            CAS 방식 참여 신청 시 발생한 총 재시도 횟수를 조회합니다.
            이 값은 서버 전역으로 누적되며, 동시성 테스트 및 모니터링 용도로 사용됩니다.

            ## 주요 파라미터
            - `eventId`: 이벤트 ID (Path Parameter) - 현재 구현에서는 전역 카운터이므로 어떤 ID든 동일한 결과 반환

            ## 응답 데이터
            - `retryCount`: 총 재시도 횟수 (서버 시작 또는 마지막 reset 이후)
            - `queriedAt`: 조회 시점

            ## 제약사항
            - 서버 재시작 시 0으로 초기화됨
            - `/cas/reset-retry-count` 엔드포인트로 수동 초기화 가능
            - 동시성 테스트 전후로 조회하여 재시도 발생 여부 확인 가능
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CasRetryCountDto.class),
                            examples = @ExampleObject(name = "재시도 횟수", value = """
                                    {
                                      "retryCount": 42,
                                      "queriedAt": "2025-12-18T10:50:00",
                                      "message": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CasRetryCountDto> getCasRetryCount(
            @Parameter(description = "이벤트 ID - 현재 구현에서는 전역 카운터이므로 어떤 ID든 가능",
                    example = "1")
            @PathVariable Long eventId) {

        return ResponseEntity.ok(participationService.getCasRetryCount());
    }

    /**
     * POST /api/v1/events/{eventId}/participations/cas/reset-retry-count
     * CAS 재시도 횟수 초기화
     */
    @PostMapping("/cas/reset-retry-count")
    @Operation(summary = "CAS 재시도 횟수 초기화", description = """
            ## 개요
            CAS 방식 참여 신청의 재시도 횟수를 0으로 초기화합니다.
            동시성 테스트 전에 카운터를 리셋하여 정확한 측정을 할 수 있습니다.

            ## 주요 파라미터
            - `eventId`: 이벤트 ID (Path Parameter) - 현재 구현에서는 전역 카운터이므로 어떤 ID든 가능

            ## 응답 데이터
            - `retryCount`: 초기화된 횟수 (0)
            - `queriedAt`: 초기화 시점
            - `message`: 초기화 완료 메시지

            ## 제약사항
            - 테스트 및 모니터링 용도로만 사용
            - 프로덕션 환경에서는 권한 제한 필요 (추후 구현)
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초기화 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CasRetryCountDto.class),
                            examples = @ExampleObject(name = "초기화 완료", value = """
                                    {
                                      "retryCount": 0,
                                      "queriedAt": "2025-12-18T10:55:00",
                                      "message": "CAS retry count has been reset"
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CasRetryCountDto> resetCasRetryCount(
            @Parameter(description = "이벤트 ID - 현재 구현에서는 전역 카운터이므로 어떤 ID든 가능",
                    example = "1")
            @PathVariable Long eventId) {

        return ResponseEntity.ok(participationService.resetCasRetryCount());
    }
}
