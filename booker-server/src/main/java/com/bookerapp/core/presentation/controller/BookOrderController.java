package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.BookOrderDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<BookOrderDto.Response> createBookOrder(
            @Valid @RequestBody BookOrderDto.Request requestDto,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 주문 요청 생성: 사용자 {}, 제목: {}", userContext.getUsername(), requestDto.getTitle());

        BookOrderDto.Response response = bookOrderService.createBookOrder(
                requestDto,
                userContext.getUserId(),
                userContext.getUsername()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "내 도서 주문 요청 목록 조회")
    @RequireRoles({Role.USER, Role.ADMIN})
    public ResponseEntity<Page<BookOrderDto.Response>> getMyBookOrders(
            @Parameter(hidden = true) UserContext userContext,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable
    ) {
        logger.info("내 도서 주문 요청 목록 조회: 사용자 {}, 페이지: {}", userContext.getUsername(), pageable);

        Page<BookOrderDto.Response> orders = bookOrderService.getBookOrdersByUser(userContext.getUserId(), pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    @Operation(summary = "모든 도서 주문 요청 목록 조회 (관리자용)")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<Page<BookOrderDto.Response>> getAllBookOrders(
            @RequestParam(required = false) BookOrder.BookOrderStatus status,
            @Parameter(hidden = true) UserContext userContext,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable
    ) {
        logger.info("모든 도서 주문 요청 목록 조회: 관리자 {}, 상태: {}, 페이지: {}",
                   userContext.getUsername(), status, pageable);

        Page<BookOrderDto.Response> orders = status != null ?
            bookOrderService.getBookOrdersByStatus(status, pageable) :
            bookOrderService.getAllBookOrders(pageable);

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "도서 주문 요청 상세 조회")
    @RequireRoles({Role.USER, Role.ADMIN})
    public ResponseEntity<BookOrderDto.Response> getBookOrder(
            @PathVariable Long id,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 주문 요청 상세 조회: ID {}, 사용자 {}", id, userContext.getUsername());

        BookOrderDto.Response order = bookOrderService.getBookOrder(id);

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
    public ResponseEntity<BookOrderDto.Response> approveBookOrder(
            @PathVariable Long id,
            @Valid @RequestBody BookOrderDto.Action actionDto,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 주문 요청 승인: ID {}, 관리자 {}", id, userContext.getUsername());

        BookOrderDto.Response response = bookOrderService.approveBookOrder(
                id,
                actionDto,
                userContext.getUserId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "도서 주문 요청 거부")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<BookOrderDto.Response> rejectBookOrder(
            @PathVariable Long id,
            @Valid @RequestBody BookOrderDto.Action actionDto,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 주문 요청 거부: ID {}, 관리자 {}", id, userContext.getUsername());

        BookOrderDto.Response response = bookOrderService.rejectBookOrder(
                id,
                actionDto,
                userContext.getUserId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "도서 입고 처리")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<BookOrderDto.Response> markAsReceived(
            @PathVariable Long id,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("도서 입고 처리: ID {}, 관리자 {}", id, userContext.getUsername());

        BookOrderDto.Response response = bookOrderService.markAsReceived(
                id,
                userContext.getUserId()
        );

        return ResponseEntity.ok(response);
    }
}
