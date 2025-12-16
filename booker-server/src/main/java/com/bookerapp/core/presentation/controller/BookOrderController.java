package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.BookOrderDto;
import com.bookerapp.core.application.service.BookOrderService;
import com.bookerapp.core.domain.model.dto.PageResponse;
import com.bookerapp.core.domain.model.entity.BookOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "BookOrder", description = "도서 주문 요청 관리 APIs")
@RequiredArgsConstructor
public class BookOrderController {

    private static final Logger logger = LoggerFactory.getLogger(BookOrderController.class);

    private final BookOrderService bookOrderService;

    @PostMapping
    @Operation(
        summary = "도서 주문 요청 생성",
        description = "새로운 도서 주문을 요청합니다.\n\n" +
                      "**초기 상태:** PENDING (검토 대기)\n\n" +
                      "**상태 흐름:**\n" +
                      "PENDING → APPROVED (승인) 또는 REJECTED (거부)\n" +
                      "APPROVED → RECEIVED (입고 완료)"
    )
    public ResponseEntity<BookOrderDto.Response> createBookOrder(
            @Valid @RequestBody BookOrderDto.Request requestDto,
            @RequestParam(required = false, defaultValue = "test-user") String userId,
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
        description = "본인이 요청한 도서 주문 목록을 조회합니다. 페이징 및 정렬을 지원합니다.\n\n" +
                      "**페이징 파라미터:**\n" +
                      "- page: 페이지 번호 (0부터 시작)\n" +
                      "- size: 페이지 크기 (기본값: 20)\n" +
                      "- sort: 정렬 기준 (기본값: createdAt,desc)\n\n" +
                      "**예시:** GET /api/v1/book-orders/my?page=0&size=20&sort=createdAt,desc"
    )
    public ResponseEntity<PageResponse<BookOrderDto.Response>> getMyBookOrders(
            @RequestParam(required = false, defaultValue = "test-user") String userId,
            @Parameter(
                description = "페이징 및 정렬",
                example = "{ \"page\": 0, \"size\": 20, \"sort\": [\"createdAt,desc\"] }",
                schema = @io.swagger.v3.oas.annotations.media.Schema(defaultValue = "createdAt,desc")
            )
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
        description = "모든 도서 주문 요청을 조회합니다.\n\n" +
                      "**필터링:**\n" +
                      "- status: 특정 상태의 주문만 필터링 (PENDING, APPROVED, REJECTED, RECEIVED)\n\n" +
                      "**페이징 파라미터:**\n" +
                      "- page: 페이지 번호 (0부터 시작)\n" +
                      "- size: 페이지 크기 (기본값: 20)\n" +
                      "- sort: 정렬 기준 (기본값: createdAt,desc)\n\n" +
                      "**예시:**\n" +
                      "- 모든 주문 조회: GET /api/v1/book-orders?page=0&size=20&sort=createdAt,desc\n" +
                      "- 승인된 주문만 조회: GET /api/v1/book-orders?status=APPROVED&sort=createdAt,desc"
    )
    public ResponseEntity<PageResponse<BookOrderDto.Response>> getAllBookOrders(
            @RequestParam(required = false) BookOrder.BookOrderStatus status,
            @Parameter(
                description = "페이징 및 정렬",
                example = "{ \"page\": 0, \"size\": 20, \"sort\": [\"createdAt,desc\"] }",
                schema = @io.swagger.v3.oas.annotations.media.Schema(defaultValue = "createdAt,desc")
            )
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
        description = "특정 도서 주문 요청의 상세 정보를 조회합니다."
    )
    public ResponseEntity<BookOrderDto.Response> getBookOrder(@PathVariable Long id) {
        logger.info("도서 주문 요청 상세 조회: ID {}", id);
        BookOrderDto.Response order = bookOrderService.getBookOrder(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/approve")
    @Operation(
        summary = "도서 주문 요청 승인",
        description = "도서 주문 요청을 승인합니다. (관리자 전용)\n\n" +
                      "**상태 변경:** PENDING → APPROVED\n\n" +
                      "**필수 조건:**\n" +
                      "- 주문 상태가 PENDING이어야 합니다.\n" +
                      "- 승인 후에는 도서 입고 처리(receive)를 진행할 수 있습니다."
    )
    public ResponseEntity<BookOrderDto.Response> approveBookOrder(
            @PathVariable Long id,
            @Valid @RequestBody BookOrderDto.Action actionDto,
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
        description = "도서 주문 요청을 거부합니다. (관리자 전용)\n\n" +
                      "**상태 변경:** PENDING → REJECTED\n\n" +
                      "**필수 조건:**\n" +
                      "- 주문 상태가 PENDING이어야 합니다.\n" +
                      "- 거부 사유를 comment에 작성해야 합니다."
    )
    public ResponseEntity<BookOrderDto.Response> rejectBookOrder(
            @PathVariable Long id,
            @Valid @RequestBody BookOrderDto.Action actionDto,
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
        description = "승인된 도서의 입고를 처리합니다. (관리자 전용)\n\n" +
                      "**상태 변경:** APPROVED → RECEIVED\n\n" +
                      "**필수 조건:**\n" +
                      "- 주문 상태가 APPROVED여야 합니다.\n" +
                      "- 입고 처리 후 도서 주문 프로세스가 완료됩니다."
    )
    public ResponseEntity<BookOrderDto.Response> markAsReceived(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "admin") String userId
    ) {
        logger.info("도서 입고 처리: ID {}, 관리자 {}", id, userId);

        BookOrderDto.Response response = bookOrderService.markAsReceived(id, userId);

        return ResponseEntity.ok(response);
    }
}
