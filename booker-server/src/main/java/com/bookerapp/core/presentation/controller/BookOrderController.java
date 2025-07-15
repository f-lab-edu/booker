package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.BookOrderActionDto;
import com.bookerapp.core.application.dto.BookOrderRequestDto;
import com.bookerapp.core.application.dto.BookOrderResponseDto;
import com.bookerapp.core.application.service.BookOrderService;
import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.entity.BookOrder;
import com.bookerapp.core.presentation.aspect.RequireRoles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/book-orders")
@Tag(name = "BookOrder", description = "도서 주문 요청 관리 APIs")
@RequiredArgsConstructor
public class BookOrderController {

    private static final Logger logger = LoggerFactory.getLogger(BookOrderController.class);

    private final BookOrderService bookOrderService;

    @PostMapping
    @Operation(summary = "도서 주문 요청 생성")
    @RequireRoles({Role.USER, Role.ADMIN})
    public ResponseEntity<BookOrderResponseDto> createBookOrder(
            @Valid @RequestBody BookOrderRequestDto requestDto,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 주문 요청 생성: 사용자 {}, 제목: {}", userContext.getUsername(), requestDto.getTitle());

        BookOrderResponseDto response = bookOrderService.createBookOrder(
                requestDto,
                userContext.getUserId(),
                userContext.getUsername()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "내 도서 주문 요청 목록 조회")
    @RequireRoles({Role.USER, Role.ADMIN})
    public ResponseEntity<List<BookOrderResponseDto>> getMyBookOrders(
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("내 도서 주문 요청 목록 조회: 사용자 {}", userContext.getUsername());

        List<BookOrderResponseDto> orders = bookOrderService.getBookOrdersByUser(userContext.getUserId());
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    @Operation(summary = "모든 도서 주문 요청 목록 조회 (관리자용)")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<List<BookOrderResponseDto>> getAllBookOrders(
            @RequestParam(required = false) BookOrder.BookOrderStatus status,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("모든 도서 주문 요청 목록 조회: 관리자 {}, 상태: {}", userContext.getUsername(), status);

        List<BookOrderResponseDto> orders;
        if (status != null) {
            orders = bookOrderService.getBookOrdersByStatus(status);
        } else {
            orders = bookOrderService.getAllBookOrders();
        }

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "도서 주문 요청 상세 조회")
    @RequireRoles({Role.USER, Role.ADMIN})
    public ResponseEntity<BookOrderResponseDto> getBookOrder(
            @PathVariable Long id,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 주문 요청 상세 조회: ID {}, 사용자 {}", id, userContext.getUsername());

        BookOrderResponseDto order = bookOrderService.getBookOrder(id);

        if (!userContext.getRoles().contains(Role.ADMIN.name()) &&
            !order.getRequesterId().equals(userContext.getUserId())) {
            logger.warn("권한 없는 도서 주문 요청 조회 시도: ID {}, 사용자 {}", id, userContext.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "도서 주문 요청 승인")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<BookOrderResponseDto> approveBookOrder(
            @PathVariable Long id,
            @Valid @RequestBody BookOrderActionDto actionDto,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 주문 요청 승인: ID {}, 관리자 {}", id, userContext.getUsername());

        BookOrderResponseDto response = bookOrderService.approveBookOrder(
                id,
                actionDto,
                userContext.getUserId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "도서 주문 요청 거부")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<BookOrderResponseDto> rejectBookOrder(
            @PathVariable Long id,
            @Valid @RequestBody BookOrderActionDto actionDto,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 주문 요청 거부: ID {}, 관리자 {}", id, userContext.getUsername());

        BookOrderResponseDto response = bookOrderService.rejectBookOrder(
                id,
                actionDto,
                userContext.getUserId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "도서 입고 처리")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<BookOrderResponseDto> markAsReceived(
            @PathVariable Long id,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 입고 처리: ID {}, 관리자 {}", id, userContext.getUsername());

        BookOrderResponseDto response = bookOrderService.markAsReceived(
                id,
                userContext.getUserId()
        );

        return ResponseEntity.ok(response);
    }
}
