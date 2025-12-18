package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.PageResponse;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.repository.MemberRepository;
import com.bookerapp.core.domain.service.DefaultEventService;
import com.bookerapp.core.domain.service.TechTalkEventService;
import com.bookerapp.core.domain.model.dto.EventDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "4. Event", description = "이벤트 관리 API")
public class EventController {

    private final DefaultEventService defaultEventService;
    private final TechTalkEventService techTalkEventService;
    private final MemberRepository memberRepository;

    @PostMapping
    @Operation(
        summary = "이벤트 생성",
        description = """
            ## 개요
            새로운 이벤트를 생성합니다. 스터디 그룹, 밋업, 컨퍼런스, 기술 발표, 워크샵 등
            다양한 유형의 이벤트를 만들 수 있습니다.

            ## 주요 파라미터
            - `title`: 이벤트 제목 (필수, 1-200자)
            - `description`: 이벤트 상세 설명 (선택, 최대 2000자)
            - `type`: 이벤트 유형 (필수) - STUDY_GROUP, MEETUP, CONFERENCE, TECH_TALK, WORKSHOP
            - `startTime`: 시작 시간 (필수, ISO-8601 형식)
            - `endTime`: 종료 시간 (필수, ISO-8601 형식)
            - `maxParticipants`: 최대 참여자 수 (기본값: 50명)

            ## 응답 데이터
            생성된 이벤트의 모든 정보를 포함합니다:
            - 이벤트 기본 정보 (ID, 제목, 설명, 유형, 시간)
            - 발표자/진행자 정보
            - 참여 가능 여부 및 정원 정보

            ## 제약사항
            - 인증 필요: 현재는 test-user로 테스트 중 (추후 Bearer Token 인증 적용 예정)
            - 시작 시간은 종료 시간보다 빨라야 합니다
            - 시작/종료 시간은 미래 시간이어야 합니다
            - 최대 참여자 수는 1-1000명 사이여야 합니다
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "이벤트 생성 성공 - Location 헤더에 생성된 리소스 URL 포함",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EventDto.Response.class),
                examples = @ExampleObject(
                    name = "이벤트 생성 성공",
                    summary = "MEETUP 유형 이벤트 생성",
                    value = """
                        {
                          "id": 1,
                          "title": "2025 신년 독서 토론회",
                          "description": "2025년을 맞이하여 올해의 독서 목표를 공유하고 추천 도서를 토론하는 시간입니다.",
                          "type": "MEETUP",
                          "startTime": "2025-01-20T14:00:00",
                          "endTime": "2025-01-20T16:00:00",
                          "maxParticipants": 50,
                          "presenter": {
                            "id": 1,
                            "memberId": "test-user",
                            "name": "Test User",
                            "email": "test@example.com",
                            "department": null,
                            "position": null
                          },
                          "participants": [],
                          "isFullyBooked": false,
                          "confirmedCount": 0,
                          "available": true
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 - 시간 검증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Bad Request",
                      "message": "시작 시간은 종료 시간보다 빨라야 합니다"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "422",
            description = "유효성 검증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Validation Failed",
                      "details": [
                        {
                          "field": "title",
                          "message": "이벤트 제목은 필수입니다"
                        },
                        {
                          "field": "startTime",
                          "message": "시작 시간은 미래여야 합니다"
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<EventDto.Response> createEvent(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "이벤트 생성 요청 데이터",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EventDto.CreateRequest.class),
                    examples = {
                        @ExampleObject(
                            name = "밋업 이벤트",
                            summary = "독서 토론회 생성",
                            description = "정기 독서 토론 모임을 위한 밋업 이벤트",
                            value = """
                                {
                                  "title": "2025 신년 독서 토론회",
                                  "description": "2025년을 맞이하여 올해의 독서 목표를 공유하고 추천 도서를 토론하는 시간입니다.",
                                  "type": "MEETUP",
                                  "startTime": "2025-01-20T14:00:00",
                                  "endTime": "2025-01-20T16:00:00",
                                  "maxParticipants": 50
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "기술 발표",
                            summary = "TECH_TALK 이벤트 생성",
                            description = "기술 주제 발표를 위한 이벤트",
                            value = """
                                {
                                  "title": "FastAPI와 Spring Boot 비교",
                                  "description": "Python FastAPI와 Java Spring Boot의 성능 및 개발 경험 비교 발표",
                                  "type": "TECH_TALK",
                                  "startTime": "2025-02-01T19:00:00",
                                  "endTime": "2025-02-01T21:00:00",
                                  "maxParticipants": 100
                                }
                                """
                        )
                    }
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody EventDto.CreateRequest request,
            @Parameter(description = "사용자 ID (현재는 테스트용 기본값 사용, 추후 인증 시스템 연동 예정)", example = "test-user")
            @RequestParam(required = false, defaultValue = "test-user") String userId,
            @Parameter(description = "사용자 이름", example = "Test User")
            @RequestParam(required = false, defaultValue = "Test User") String username,
            @Parameter(description = "사용자 이메일", example = "test@example.com")
            @RequestParam(required = false, defaultValue = "test@example.com") String email) {
        // 기존 Member가 있으면 재사용, 없으면 새로 생성
        Member presenter = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(new Member(userId, username, email)));

        Event event = defaultEventService.createEvent(request, presenter);
        EventDto.Response response = EventDto.Response.from(event);

        // Build Location header
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(event.getId())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "이벤트 수정",
        description = """
            ## 개요
            기존 이벤트의 정보를 수정합니다.
            제목, 설명, 시작/종료 시간을 변경할 수 있습니다.

            ## 주요 파라미터
            - `id`: 수정할 이벤트 ID (Path Parameter)
            - `title`: 새로운 제목 (선택)
            - `description`: 새로운 설명 (선택)
            - `startTime`: 새로운 시작 시간 (선택)
            - `endTime`: 새로운 종료 시간 (선택)

            ## 응답 데이터
            - 200 OK: 수정 성공, 응답 본문 없음

            ## 제약사항
            - 이벤트 유형(type)과 최대 참여자 수(maxParticipants)는 수정할 수 없습니다
            - 시작 시간은 종료 시간보다 빨라야 합니다
            - 존재하지 않는 이벤트 ID인 경우 404 오류 발생
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Bad Request",
                      "message": "시작 시간은 종료 시간보다 빨라야 합니다"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "이벤트를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Not Found",
                      "message": "이벤트를 찾을 수 없습니다: 999"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "422",
            description = "유효성 검증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Validation Failed",
                      "details": [
                        {
                          "field": "title",
                          "message": "제목은 1-200자 이내여야 합니다"
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> updateEvent(
            @Parameter(description = "수정할 이벤트의 고유 ID", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "이벤트 수정 요청 데이터",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EventDto.UpdateRequest.class),
                    examples = @ExampleObject(
                        name = "이벤트 수정",
                        summary = "제목과 시간 변경",
                        value = """
                            {
                              "title": "2025 신년 독서 토론회 (시간 변경)",
                              "description": "일정 변경으로 시간이 조정되었습니다.",
                              "startTime": "2025-01-21T14:00:00",
                              "endTime": "2025-01-21T16:00:00"
                            }
                            """
                    )
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody EventDto.UpdateRequest request) {
        defaultEventService.updateEvent(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "이벤트 삭제",
        description = """
            ## 개요
            이벤트를 삭제합니다.
            참여자가 없는 이벤트만 삭제 가능합니다.

            ## 주요 파라미터
            - `id`: 삭제할 이벤트 ID (Path Parameter)

            ## 응답 데이터
            - 200 OK: 삭제 성공, 응답 본문 없음

            ## 제약사항
            - 참여자가 있는 이벤트는 삭제할 수 없습니다
            - 삭제된 이벤트는 복구할 수 없습니다
            - 존재하지 않는 이벤트 ID인 경우 404 오류 발생
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "삭제 불가 - 참여자가 있음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Bad Request",
                      "message": "참여자가 있는 이벤트는 삭제할 수 없습니다"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "이벤트를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Not Found",
                      "message": "이벤트를 찾을 수 없습니다: 999"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "삭제할 이벤트의 고유 ID", example = "1", required = true)
            @PathVariable Long id) {
        defaultEventService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/participants")
    @Operation(
        summary = "이벤트 참가자 추가",
        description = """
            ## 개요
            이벤트에 참가자를 추가합니다.
            정원이 초과된 경우 자동으로 대기 목록에 추가됩니다.

            ## 주요 파라미터
            - `id`: 이벤트 ID (Path Parameter)
            - `memberId`: 참가자 ID (Query Parameter)
            - `memberName`: 참가자 이름 (Query Parameter, 선택)
            - `memberEmail`: 참가자 이메일 (Query Parameter, 선택)

            ## 응답 데이터
            - 200 OK: 참가자 추가 성공, 응답 본문 없음

            ## 제약사항
            - 이 API는 관리자용입니다. 일반 사용자는 Event Participation API를 사용하세요
            - 동일한 참가자는 중복으로 추가할 수 없습니다
            - 정원 초과 시 자동으로 대기 목록에 추가됩니다
            - 존재하지 않는 이벤트 ID인 경우 404 오류 발생
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "참가자 추가 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 - 중복 참가자",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Bad Request",
                      "message": "이미 참가 중인 회원입니다"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "이벤트를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Not Found",
                      "message": "이벤트를 찾을 수 없습니다: 999"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> addParticipant(
            @Parameter(description = "이벤트 ID", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(description = "참가자 ID", example = "user123", required = true)
            @RequestParam String memberId,
            @Parameter(description = "참가자 이름", example = "Test User")
            @RequestParam(required = false, defaultValue = "Test User") String memberName,
            @Parameter(description = "참가자 이메일", example = "test@example.com")
            @RequestParam(required = false, defaultValue = "test@example.com") String memberEmail) {
        // 기존 Member가 있으면 재사용, 없으면 새로 생성
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseGet(() -> memberRepository.save(new Member(memberId, memberName, memberEmail)));

        defaultEventService.addParticipant(id, member);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/participants/{memberId}")
    @Operation(
        summary = "이벤트 참가자 제거",
        description = """
            ## 개요
            이벤트에서 참가자를 제거합니다.
            참가자 제거 시 대기자가 있으면 자동으로 승격됩니다.

            ## 주요 파라미터
            - `id`: 이벤트 ID (Path Parameter)
            - `memberId`: 제거할 참가자 ID (Path Parameter)
            - `memberName`: 참가자 이름 (Query Parameter, 선택)
            - `memberEmail`: 참가자 이메일 (Query Parameter, 선택)

            ## 응답 데이터
            - 200 OK: 참가자 제거 성공, 응답 본문 없음

            ## 제약사항
            - 이 API는 관리자용입니다
            - 존재하지 않는 이벤트 또는 참가자인 경우 404 오류 발생
            - 참가자 제거 시 대기 목록의 첫 번째 참가자가 자동으로 확정됩니다
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "참가자 제거 성공"),
        @ApiResponse(
            responseCode = "404",
            description = "이벤트 또는 참가자를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "이벤트 없음",
                        value = """
                            {
                              "error": "Not Found",
                              "message": "이벤트를 찾을 수 없습니다: 999"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "참가자 없음",
                        value = """
                            {
                              "error": "Not Found",
                              "message": "참가자를 찾을 수 없습니다"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> removeParticipant(
            @Parameter(description = "이벤트 ID", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(description = "제거할 참가자 ID", example = "user123", required = true)
            @PathVariable String memberId,
            @Parameter(description = "참가자 이름", example = "Test User")
            @RequestParam(required = false, defaultValue = "Test User") String memberName,
            @Parameter(description = "참가자 이메일", example = "test@example.com")
            @RequestParam(required = false, defaultValue = "test@example.com") String memberEmail) {
        Member member = new Member(memberId, memberName, memberEmail);
        defaultEventService.removeParticipant(id, member);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(
        summary = "이벤트 목록 조회",
        description = """
            ## 개요
            이벤트 목록을 조회합니다. 페이징 및 이벤트 유형별 필터링을 지원합니다.

            ## 사용 예시
            - **전체 이벤트 조회**: `/api/v1/events?page=0&size=20`
            - **밋업만 조회**: `/api/v1/events?type=MEETUP&page=0&size=10`
            - **기술 발표 최신순**: `/api/v1/events?type=TECH_TALK&sort=startTime,desc`

            ## 주요 파라미터
            - `type`: 이벤트 유형 필터 (선택) - STUDY_GROUP, MEETUP, CONFERENCE, TECH_TALK, WORKSHOP
            - `page`: 페이지 번호, 0부터 시작 (기본값: 0)
            - `size`: 페이지 크기 (기본값: 20)
            - `sort`: 정렬 기준 (기본값: startTime,desc)

            ## 응답 데이터
            페이지네이션된 이벤트 목록과 메타데이터를 반환합니다:
            - `content`: 이벤트 목록 배열
            - `totalElements`: 전체 이벤트 수
            - `totalPages`: 전체 페이지 수
            - `page`: 현재 페이지 번호
            - `size`: 페이지 크기
            - `first`: 첫 페이지 여부
            - `last`: 마지막 페이지 여부

            ## 제약사항
            - 과거 이벤트와 미래 이벤트 모두 조회됩니다
            - 정렬 가능 필드: startTime, endTime, title, createdAt
            - 최대 페이지 크기: 100 (추후 구현 예정)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(
                    name = "이벤트 목록",
                    value = """
                        {
                          "content": [
                            {
                              "id": 1,
                              "title": "2025 신년 독서 토론회",
                              "description": "2025년을 맞이하여...",
                              "type": "MEETUP",
                              "startTime": "2025-01-20T14:00:00",
                              "endTime": "2025-01-20T16:00:00",
                              "maxParticipants": 50,
                              "presenter": {
                                "id": 1,
                                "memberId": "test-user",
                                "name": "Test User",
                                "email": "test@example.com",
                                "department": "개발팀",
                                "position": "시니어 개발자"
                              },
                              "participants": [
                                {
                                  "id": 1,
                                  "member": {
                                    "id": 2,
                                    "memberId": "user001",
                                    "name": "김철수",
                                    "email": "kim@example.com",
                                    "department": null,
                                    "position": null
                                  },
                                  "status": "CONFIRMED",
                                  "registrationDate": "2025-01-15T10:30:00",
                                  "waitingNumber": null
                                }
                              ],
                              "isFullyBooked": false,
                              "confirmedCount": 1,
                              "available": true
                            }
                          ],
                          "page": 0,
                          "size": 20,
                          "totalElements": 1,
                          "totalPages": 1,
                          "first": true,
                          "last": true
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 검색 파라미터",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Invalid parameter",
                      "message": "Page size must not exceed 100"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<PageResponse<EventDto.Response>> getEvents(
            @Parameter(
                description = "이벤트 유형 필터 (선택)\n" +
                              "- STUDY_GROUP: 정기 스터디\n" +
                              "- MEETUP: 비정기 모임\n" +
                              "- CONFERENCE: 컨퍼런스\n" +
                              "- TECH_TALK: 기술 발표\n" +
                              "- WORKSHOP: 워크샵",
                example = "MEETUP"
            )
            @RequestParam(required = false) EventType type,
            @Parameter(
                description = "페이징 및 정렬 정보\n" +
                              "- page: 페이지 번호 (0부터 시작)\n" +
                              "- size: 페이지 크기 (기본 20)\n" +
                              "- sort: 정렬 (예: startTime,desc)",
                example = "{ \"page\": 0, \"size\": 20, \"sort\": [\"startTime,desc\"] }"
            )
            @PageableDefault(size = 20, sort = "startTime", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        Page<Event> events = type != null ?
            defaultEventService.findEventsByType(type, pageable) :
            defaultEventService.findAllEvents(pageable);
        return ResponseEntity.ok(PageResponse.of(events.map(EventDto.Response::from)));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "이벤트 상세 조회",
        description = """
            ## 개요
            특정 이벤트의 상세 정보를 조회합니다.
            이벤트 ID를 통해 기본 정보, 발표자, 참여자 목록 등 모든 정보를 확인할 수 있습니다.

            ## 주요 파라미터
            - `id`: 이벤트 ID (Long 타입, Path Parameter)

            ## 응답 데이터
            이벤트의 모든 정보를 포함한 상세 데이터를 반환합니다:
            - 이벤트 기본 정보: ID, 제목, 설명, 유형, 시간
            - 발표자/진행자 정보
            - 참여자 목록 (확정/대기 상태 포함)
            - 참가 가능 여부 및 정원 정보

            ## 제약사항
            - 존재하지 않는 ID 조회 시 404 오류 발생
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EventDto.Response.class),
                examples = @ExampleObject(
                    name = "이벤트 상세 정보",
                    value = """
                        {
                          "id": 1,
                          "title": "2025 신년 독서 토론회",
                          "description": "2025년을 맞이하여 올해의 독서 목표를 공유하고 추천 도서를 토론하는 시간입니다.",
                          "type": "MEETUP",
                          "startTime": "2025-01-20T14:00:00",
                          "endTime": "2025-01-20T16:00:00",
                          "maxParticipants": 50,
                          "presenter": {
                            "id": 1,
                            "memberId": "test-user",
                            "name": "Test User",
                            "email": "test@example.com",
                            "department": "개발팀",
                            "position": "시니어 개발자"
                          },
                          "participants": [
                            {
                              "id": 1,
                              "member": {
                                "id": 2,
                                "memberId": "user001",
                                "name": "김철수",
                                "email": "kim@example.com",
                                "department": null,
                                "position": null
                              },
                              "status": "CONFIRMED",
                              "registrationDate": "2025-01-15T10:30:00",
                              "waitingNumber": null
                            },
                            {
                              "id": 2,
                              "member": {
                                "id": 3,
                                "memberId": "user002",
                                "name": "이영희",
                                "email": "lee@example.com",
                                "department": "마케팅팀",
                                "position": "대리"
                              },
                              "status": "WAITING",
                              "registrationDate": "2025-01-16T14:20:00",
                              "waitingNumber": 1
                            }
                          ],
                          "isFullyBooked": true,
                          "confirmedCount": 50,
                          "available": false
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "이벤트를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Not Found",
                      "message": "이벤트를 찾을 수 없습니다: 999"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<EventDto.Response> getEvent(
            @Parameter(description = "조회할 이벤트의 고유 ID", example = "1", required = true)
            @PathVariable Long id) {
        Event event = defaultEventService.findEventById(id);
        return ResponseEntity.ok(EventDto.Response.from(event));
    }
}
