package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.BookOrderDto;
import com.bookerapp.core.application.service.BookOrderService;
import com.bookerapp.core.domain.model.dto.PageResponse;
import com.bookerapp.core.domain.model.entity.BookOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/book-orders")
@Tag(name = "3. BookOrder", description = "도서 주문 요청 관리 API")
@RequiredArgsConstructor
public class BookOrderController {

    private static final Logger logger = LoggerFactory.getLogger(BookOrderController.class);

    private final BookOrderService bookOrderService;

    @PostMapping
    @Operation(
        summary = "도서 주문 요청 생성",
        description = """
                ## 개요
                사용자가 도서관에 없는 새로운 도서의 구매를 요청합니다.
                요청된 도서는 관리자의 검토를 거쳐 승인 또는 거부됩니다.

                ## 주요 파라미터
                - `title`: 도서 제목 (필수)
                - `author`: 저자명 (선택)
                - `publisher`: 출판사명 (선택)
                - `isbn`: ISBN 번호 (선택)

                ## 응답 데이터
                생성된 주문 요청의 전체 정보를 반환합니다.
                초기 상태는 PENDING(검토 대기)으로 설정됩니다.

                ## 제약사항
                - 인증된 사용자만 요청 가능 (userId, username 파라미터 필요)
                - 제목은 필수 입력 항목입니다
                - 생성 후 상태 흐름: PENDING → APPROVED/REJECTED → RECEIVED
                """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "도서 주문 요청 생성 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookOrderDto.Response.class),
                examples = @ExampleObject(
                    name = "생성된 도서 주문 요청 예시",
                    value = """
                        {
                          "id": 1,
                          "title": "Effective Java",
                          "author": "Joshua Bloch",
                          "publisher": "Addison-Wesley",
                          "isbn": "9780134685991",
                          "requesterId": "user123",
                          "requesterName": "홍길동",
                          "status": "PENDING",
                          "adminComments": null,
                          "approvedAt": null,
                          "approvedBy": null,
                          "receivedAt": null,
                          "receivedBy": null,
                          "createdAt": "2024-01-10T09:00:00",
                          "updatedAt": "2024-01-10T09:00:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 - 필수 필드 누락 또는 유효성 검증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Validation failed",
                          "details": ["title: 도서명은 필수입니다"]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "422",
            description = "처리할 수 없는 요청 - 비즈니스 규칙 위반",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Unprocessable Entity",
                          "message": "도서명은 30자를 초과할 수 없습니다"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookOrderDto.Response> createBookOrder(
            @RequestBody(
                description = "도서 주문 요청 데이터",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookOrderDto.Request.class),
                    examples = {
                        @ExampleObject(
                            name = "Effective Java 주문 요청",
                            summary = "Java 프로그래밍 서적 주문",
                            description = "Joshua Bloch의 Effective Java 3판 주문 요청 예시",
                            value = """
                                {
                                  "title": "Effective Java",
                                  "author": "Joshua Bloch",
                                  "publisher": "Addison-Wesley",
                                  "isbn": "9780134685991"
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "제목만 포함한 최소 요청",
                            summary = "필수 항목만 입력",
                            description = "제목만 입력한 최소 주문 요청 (저자, 출판사, ISBN 생략 가능)",
                            value = """
                                {
                                  "title": "Design Patterns"
                                }
                                """
                        )
                    }
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody BookOrderDto.Request requestDto,
            @Parameter(description = "요청자 ID", example = "user123")
            @RequestParam(required = false, defaultValue = "test-user") String userId,
            @Parameter(description = "요청자 이름", example = "홍길동")
            @RequestParam(required = false, defaultValue = "Test User") String username
    ) {
        logger.info("도서 주문 요청 생성: 사용자 {}, 제목: {}", username, requestDto.getTitle());

        BookOrderDto.Response response = bookOrderService.createBookOrder(
                requestDto,
                userId,
                username
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(
        summary = "내 도서 주문 요청 목록 조회",
        description = """
                ## 개요
                현재 로그인한 사용자가 요청한 도서 주문 목록을 조회합니다.
                최신 요청순으로 정렬되며 페이징을 지원합니다.

                ## 주요 파라미터
                - `page`: 페이지 번호 (0부터 시작, 기본값: 0)
                - `size`: 페이지 크기 (기본값: 20)
                - `sort`: 정렬 기준 (기본값: createdAt,desc - 최신순)

                ## 응답 데이터
                페이징된 도서 주문 요청 목록을 반환합니다.
                각 요청의 현재 상태(PENDING, APPROVED, REJECTED, RECEIVED)를 확인할 수 있습니다.

                ## 제약사항
                - 인증된 사용자만 조회 가능
                - 본인이 요청한 주문만 조회됩니다
                - 최대 페이지 크기는 100입니다
                """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "내 도서 주문 목록 예시",
                    value = """
                        {
                          "content": [
                            {
                              "id": 2,
                              "title": "Clean Architecture",
                              "author": "Robert C. Martin",
                              "publisher": "Prentice Hall",
                              "isbn": "9780134494166",
                              "requesterId": "user123",
                              "requesterName": "홍길동",
                              "status": "APPROVED",
                              "adminComments": "예산 승인됨",
                              "approvedAt": "2024-01-11T14:30:00",
                              "approvedBy": "admin",
                              "receivedAt": null,
                              "receivedBy": null,
                              "createdAt": "2024-01-11T09:00:00",
                              "updatedAt": "2024-01-11T14:30:00"
                            },
                            {
                              "id": 1,
                              "title": "Effective Java",
                              "author": "Joshua Bloch",
                              "publisher": "Addison-Wesley",
                              "isbn": "9780134685991",
                              "requesterId": "user123",
                              "requesterName": "홍길동",
                              "status": "PENDING",
                              "adminComments": null,
                              "approvedAt": null,
                              "approvedBy": null,
                              "receivedAt": null,
                              "receivedBy": null,
                              "createdAt": "2024-01-10T09:00:00",
                              "updatedAt": "2024-01-10T09:00:00"
                            }
                          ],
                          "page": 0,
                          "totalRows": 2,
                          "currentPageRows": 2
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 로그인 필요"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<PageResponse<BookOrderDto.Response>> getMyBookOrders(
            @Parameter(description = "요청자 ID", example = "user123")
            @RequestParam(required = false, defaultValue = "test-user") String userId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        logger.info("내 도서 주문 요청 목록 조회: 사용자 {}, 페이지: {}", userId, pageable);

        Page<BookOrderDto.Response> orders = bookOrderService.getBookOrdersByUser(userId, pageable);
        return ResponseEntity.ok(PageResponse.of(orders));
    }

    @GetMapping
    @Operation(
        summary = "모든 도서 주문 요청 목록 조회 (관리자용)",
        description = """
                ## 개요
                시스템에 등록된 모든 도서 주문 요청을 조회합니다.
                관리자가 전체 주문 현황을 파악하고 관리하기 위한 API입니다.

                ## 주요 파라미터
                - `status`: 특정 상태로 필터링 (PENDING, APPROVED, REJECTED, RECEIVED)
                - `page`: 페이지 번호 (0부터 시작, 기본값: 0)
                - `size`: 페이지 크기 (기본값: 20)
                - `sort`: 정렬 기준 (기본값: createdAt,desc - 최신순)

                ## 응답 데이터
                페이징된 전체 도서 주문 요청 목록을 반환합니다.
                status 파라미터로 특정 상태의 주문만 필터링할 수 있습니다.

                ## 제약사항
                - 관리자 권한 필요
                - status 미입력 시 모든 상태의 주문을 조회합니다
                - 최대 페이지 크기는 100입니다

                ## 사용 예시
                - 모든 주문 조회: `GET /api/v1/book-orders`
                - 검토 대기 주문만: `GET /api/v1/book-orders?status=PENDING`
                - 승인된 주문만: `GET /api/v1/book-orders?status=APPROVED`
                """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "전체 주문 목록",
                        description = "필터링 없이 모든 상태의 주문을 조회한 결과",
                        value = """
                            {
                              "content": [
                                {
                                  "id": 3,
                                  "title": "Domain-Driven Design",
                                  "author": "Eric Evans",
                                  "publisher": "Addison-Wesley",
                                  "isbn": "9780321125217",
                                  "requesterId": "user456",
                                  "requesterName": "김철수",
                                  "status": "RECEIVED",
                                  "adminComments": "입고 완료",
                                  "approvedAt": "2024-01-12T10:00:00",
                                  "approvedBy": "admin",
                                  "receivedAt": "2024-01-20T15:00:00",
                                  "receivedBy": "admin",
                                  "createdAt": "2024-01-12T09:00:00",
                                  "updatedAt": "2024-01-20T15:00:00"
                                },
                                {
                                  "id": 2,
                                  "title": "Clean Architecture",
                                  "author": "Robert C. Martin",
                                  "publisher": "Prentice Hall",
                                  "isbn": "9780134494166",
                                  "requesterId": "user123",
                                  "requesterName": "홍길동",
                                  "status": "APPROVED",
                                  "adminComments": "예산 승인됨",
                                  "approvedAt": "2024-01-11T14:30:00",
                                  "approvedBy": "admin",
                                  "receivedAt": null,
                                  "receivedBy": null,
                                  "createdAt": "2024-01-11T09:00:00",
                                  "updatedAt": "2024-01-11T14:30:00"
                                }
                              ],
                              "page": 0,
                              "totalRows": 2,
                              "currentPageRows": 2
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "PENDING 상태 필터링",
                        description = "검토 대기 중인 주문만 조회한 결과",
                        value = """
                            {
                              "content": [
                                {
                                  "id": 1,
                                  "title": "Effective Java",
                                  "author": "Joshua Bloch",
                                  "publisher": "Addison-Wesley",
                                  "isbn": "9780134685991",
                                  "requesterId": "user123",
                                  "requesterName": "홍길동",
                                  "status": "PENDING",
                                  "adminComments": null,
                                  "approvedAt": null,
                                  "approvedBy": null,
                                  "receivedAt": null,
                                  "receivedBy": null,
                                  "createdAt": "2024-01-10T09:00:00",
                                  "updatedAt": "2024-01-10T09:00:00"
                                }
                              ],
                              "page": 0,
                              "totalRows": 1,
                              "currentPageRows": 1
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 로그인 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 관리자 전용"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<PageResponse<BookOrderDto.Response>> getAllBookOrders(
            @Parameter(
                description = "주문 상태 필터 - 특정 상태의 주문만 조회 (선택사항)",
                example = "PENDING",
                schema = @Schema(allowableValues = {"PENDING", "APPROVED", "REJECTED", "RECEIVED"})
            )
            @RequestParam(required = false) BookOrder.BookOrderStatus status,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        logger.info("모든 도서 주문 요청 목록 조회: 상태: {}, 페이지: {}", status, pageable);

        Page<BookOrderDto.Response> orders = status != null ?
            bookOrderService.getBookOrdersByStatus(status, pageable) :
            bookOrderService.getAllBookOrders(pageable);

        return ResponseEntity.ok(PageResponse.of(orders));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "도서 주문 요청 상세 조회",
        description = """
                ## 개요
                특정 도서 주문 요청의 상세 정보를 조회합니다.
                주문의 현재 상태, 승인/거부 이력, 입고 정보 등을 확인할 수 있습니다.

                ## 주요 파라미터
                - `id`: 조회할 주문 요청의 고유 ID

                ## 응답 데이터
                주문 요청의 모든 정보를 반환합니다.
                - 기본 정보: 도서명, 저자, 출판사, ISBN
                - 요청자 정보: 요청자 ID, 이름
                - 상태 정보: 현재 상태, 승인/거부/입고 일시 및 처리자

                ## 제약사항
                - 존재하지 않는 ID 조회 시 404 오류 반환
                - 일반 사용자는 본인의 주문만 조회 가능
                - 관리자는 모든 주문 조회 가능
                """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookOrderDto.Response.class),
                examples = @ExampleObject(
                    name = "승인된 주문 상세 정보",
                    value = """
                        {
                          "id": 2,
                          "title": "Clean Architecture",
                          "author": "Robert C. Martin",
                          "publisher": "Prentice Hall",
                          "isbn": "9780134494166",
                          "requesterId": "user123",
                          "requesterName": "홍길동",
                          "status": "APPROVED",
                          "adminComments": "예산 승인됨. 2주 내 입고 예정",
                          "approvedAt": "2024-01-11T14:30:00",
                          "approvedBy": "admin",
                          "receivedAt": null,
                          "receivedBy": null,
                          "createdAt": "2024-01-11T09:00:00",
                          "updatedAt": "2024-01-11T14:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Not Found",
                          "message": "ID 999에 해당하는 도서 주문 요청을 찾을 수 없습니다"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 다른 사용자의 주문 조회 시도"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookOrderDto.Response> getBookOrder(
            @Parameter(description = "조회할 주문 요청 ID", example = "1", required = true)
            @PathVariable Long id
    ) {
        logger.info("도서 주문 요청 상세 조회: ID {}", id);
        BookOrderDto.Response order = bookOrderService.getBookOrder(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/approve")
    @Operation(
        summary = "도서 주문 요청 승인",
        description = """
                ## 개요
                관리자가 도서 주문 요청을 승인합니다.
                승인된 주문은 도서 구매 프로세스로 진행되며, 입고 처리 대기 상태가 됩니다.

                ## 주요 파라미터
                - `id`: 승인할 주문 요청의 ID
                - `comments`: 승인 사유 또는 메모 (선택사항)

                ## 응답 데이터
                상태가 APPROVED로 변경된 주문 정보를 반환합니다.
                approvedAt(승인 일시)과 approvedBy(승인자) 정보가 기록됩니다.

                ## 제약사항
                - 관리자 권한 필요
                - 주문 상태가 PENDING이어야 승인 가능
                - 이미 승인/거부된 주문은 다시 승인 불가
                - 승인 후 상태 흐름: APPROVED → RECEIVED (입고 처리)
                """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "승인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookOrderDto.Response.class),
                examples = @ExampleObject(
                    name = "승인 완료된 주문",
                    value = """
                        {
                          "id": 1,
                          "title": "Effective Java",
                          "author": "Joshua Bloch",
                          "publisher": "Addison-Wesley",
                          "isbn": "9780134685991",
                          "requesterId": "user123",
                          "requesterName": "홍길동",
                          "status": "APPROVED",
                          "adminComments": "예산 승인됨. 2주 내 입고 예정",
                          "approvedAt": "2024-01-15T14:30:00",
                          "approvedBy": "admin",
                          "receivedAt": null,
                          "receivedBy": null,
                          "createdAt": "2024-01-10T09:00:00",
                          "updatedAt": "2024-01-15T14:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 - PENDING 상태가 아님",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Bad Request",
                          "message": "PENDING 상태의 주문만 승인할 수 있습니다. 현재 상태: APPROVED"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 관리자 전용"),
        @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookOrderDto.Response> approveBookOrder(
            @Parameter(description = "승인할 주문 요청 ID", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody(
                description = "승인 처리 데이터 (코멘트 포함)",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookOrderDto.Action.class),
                    examples = {
                        @ExampleObject(
                            name = "코멘트 포함 승인",
                            summary = "승인 사유를 포함한 승인 처리",
                            value = """
                                {
                                  "comments": "예산 승인됨. 2주 내 입고 예정"
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "코멘트 없이 승인",
                            summary = "사유 없이 단순 승인",
                            value = """
                                {
                                  "comments": null
                                }
                                """
                        )
                    }
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody BookOrderDto.Action actionDto,
            @Parameter(description = "승인 처리자 ID (관리자)", example = "admin")
            @RequestParam(required = false, defaultValue = "admin") String userId
    ) {
        logger.info("도서 주문 요청 승인: ID {}, 관리자 {}", id, userId);

        BookOrderDto.Response response = bookOrderService.approveBookOrder(
                id,
                actionDto,
                userId
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @Operation(
        summary = "도서 주문 요청 거부",
        description = """
                ## 개요
                관리자가 도서 주문 요청을 거부합니다.
                거부된 주문은 더 이상 진행되지 않으며, 종료 상태가 됩니다.

                ## 주요 파라미터
                - `id`: 거부할 주문 요청의 ID
                - `comments`: 거부 사유 (필수 권장)

                ## 응답 데이터
                상태가 REJECTED로 변경된 주문 정보를 반환합니다.
                거부 사유는 adminComments 필드에 저장되어 요청자가 확인할 수 있습니다.

                ## 제약사항
                - 관리자 권한 필요
                - 주문 상태가 PENDING이어야 거부 가능
                - 거부 사유를 명확히 기재하는 것을 권장합니다
                - 거부된 주문은 최종 상태로, 이후 상태 변경 불가
                """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "거부 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookOrderDto.Response.class),
                examples = @ExampleObject(
                    name = "거부 완료된 주문",
                    value = """
                        {
                          "id": 1,
                          "title": "Effective Java",
                          "author": "Joshua Bloch",
                          "publisher": "Addison-Wesley",
                          "isbn": "9780134685991",
                          "requesterId": "user123",
                          "requesterName": "홍길동",
                          "status": "REJECTED",
                          "adminComments": "예산 부족으로 다음 분기에 재검토 예정",
                          "approvedAt": null,
                          "approvedBy": null,
                          "receivedAt": null,
                          "receivedBy": null,
                          "createdAt": "2024-01-10T09:00:00",
                          "updatedAt": "2024-01-15T14:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 - PENDING 상태가 아님",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Bad Request",
                          "message": "PENDING 상태의 주문만 거부할 수 있습니다. 현재 상태: REJECTED"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 관리자 전용"),
        @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookOrderDto.Response> rejectBookOrder(
            @Parameter(description = "거부할 주문 요청 ID", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody(
                description = "거부 처리 데이터 (거부 사유 필수 권장)",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookOrderDto.Action.class),
                    examples = {
                        @ExampleObject(
                            name = "예산 부족 사유",
                            summary = "예산 문제로 거부",
                            value = """
                                {
                                  "comments": "예산 부족으로 다음 분기에 재검토 예정"
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "중복 도서 사유",
                            summary = "이미 소장 중인 도서",
                            value = """
                                {
                                  "comments": "이미 도서관에 동일 도서가 소장되어 있습니다"
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "부적합 도서 사유",
                            summary = "도서관 정책 위배",
                            value = """
                                {
                                  "comments": "도서관 장서 정책에 부합하지 않는 도서입니다"
                                }
                                """
                        )
                    }
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody BookOrderDto.Action actionDto,
            @Parameter(description = "거부 처리자 ID (관리자)", example = "admin")
            @RequestParam(required = false, defaultValue = "admin") String userId
    ) {
        logger.info("도서 주문 요청 거부: ID {}, 관리자 {}", id, userId);

        BookOrderDto.Response response = bookOrderService.rejectBookOrder(
                id,
                actionDto,
                userId
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/receive")
    @Operation(
        summary = "도서 입고 처리",
        description = """
                ## 개요
                승인된 도서 주문의 실제 입고를 처리합니다.
                입고 처리는 도서 주문 프로세스의 최종 단계입니다.

                ## 주요 파라미터
                - `id`: 입고 처리할 주문 요청의 ID

                ## 응답 데이터
                상태가 RECEIVED로 변경된 주문 정보를 반환합니다.
                receivedAt(입고 일시)과 receivedBy(입고 처리자) 정보가 기록됩니다.

                ## 제약사항
                - 관리자 권한 필요
                - 주문 상태가 APPROVED여야 입고 처리 가능
                - PENDING 또는 REJECTED 상태에서는 입고 처리 불가
                - 입고 처리 후 주문 프로세스 완료 (최종 상태)

                ## 프로세스 흐름
                1. 사용자 주문 요청 생성 (PENDING)
                2. 관리자 승인 (APPROVED)
                3. **도서 입고 처리 (RECEIVED)** ← 현재 단계
                4. 프로세스 완료
                """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "입고 처리 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BookOrderDto.Response.class),
                examples = @ExampleObject(
                    name = "입고 완료된 주문",
                    value = """
                        {
                          "id": 2,
                          "title": "Clean Architecture",
                          "author": "Robert C. Martin",
                          "publisher": "Prentice Hall",
                          "isbn": "9780134494166",
                          "requesterId": "user123",
                          "requesterName": "홍길동",
                          "status": "RECEIVED",
                          "adminComments": "예산 승인됨",
                          "approvedAt": "2024-01-11T14:30:00",
                          "approvedBy": "admin",
                          "receivedAt": "2024-01-25T10:00:00",
                          "receivedBy": "admin",
                          "createdAt": "2024-01-11T09:00:00",
                          "updatedAt": "2024-01-25T10:00:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 - APPROVED 상태가 아님",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Bad Request",
                          "message": "APPROVED 상태의 주문만 입고 처리할 수 있습니다. 현재 상태: PENDING"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 관리자 전용"),
        @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookOrderDto.Response> markAsReceived(
            @Parameter(description = "입고 처리할 주문 요청 ID", example = "2", required = true)
            @PathVariable Long id,
            @Parameter(description = "입고 처리자 ID (관리자)", example = "admin")
            @RequestParam(required = false, defaultValue = "admin") String userId
    ) {
        logger.info("도서 입고 처리: ID {}, 관리자 {}", id, userId);

        BookOrderDto.Response response = bookOrderService.markAsReceived(id, userId);

        return ResponseEntity.ok(response);
    }
}
