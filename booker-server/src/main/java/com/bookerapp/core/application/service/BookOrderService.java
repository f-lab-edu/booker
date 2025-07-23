package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.BookOrderDto;
import com.bookerapp.core.domain.model.entity.BookOrder;
import com.bookerapp.core.infrastructure.repository.BookOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookerapp.core.presentation.exception.NotPendingStatusException;
import com.bookerapp.core.presentation.exception.NotApprovedStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class BookOrderService {

    private static final Logger logger = LoggerFactory.getLogger(BookOrderService.class);

    private final BookOrderRepository bookOrderRepository;

    public BookOrderDto.Response createBookOrder(BookOrderDto.Request requestDto, String requesterId, String requesterName) {
        BookOrder bookOrder = new BookOrder(
            requestDto.getTitle(),
            requestDto.getAuthor(),
            requestDto.getPublisher(),
            requestDto.getIsbn(),
            requesterId,
            requesterName
        );

        bookOrder.setCreatedBy(requesterId);
        bookOrder.setUpdatedBy(requesterId);

        BookOrder savedOrder = bookOrderRepository.save(bookOrder);

        logger.info("도서 주문 요청이 생성되었습니다. ID: {}, 요청자: {}, 제목: {}",
                   savedOrder.getId(), requesterName, requestDto.getTitle());

        notifyAdminOfNewOrder(savedOrder);

        return new BookOrderDto.Response(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<BookOrderDto.Response> getBookOrdersByUser(String userId, Pageable pageable) {
        return bookOrderRepository.findByRequesterId(userId, pageable)
                                .map(BookOrderDto.Response::new);
    }

    @Transactional(readOnly = true)
    public Page<BookOrderDto.Response> getAllBookOrders(Pageable pageable) {
        return bookOrderRepository.findAllWithPagination(pageable)
                                .map(BookOrderDto.Response::new);
    }

    @Transactional(readOnly = true)
    public Page<BookOrderDto.Response> getBookOrdersByStatus(BookOrder.BookOrderStatus status, Pageable pageable) {
        return bookOrderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                                .map(BookOrderDto.Response::new);
    }

    @Transactional(readOnly = true)
    public BookOrderDto.Response getBookOrder(Long id) {
        BookOrder order = bookOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다. ID: " + id));

        if (order.isDeleted()) {
            throw new IllegalArgumentException("삭제된 도서 주문 요청입니다. ID: " + id);
        }

        return new BookOrderDto.Response(order);
    }

    public BookOrderDto.Response approveBookOrder(Long id, BookOrderDto.Action actionDto, String adminId) {
        BookOrder order = bookOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다. ID: " + id));

        if (order.isDeleted()) {
            throw new IllegalArgumentException("삭제된 도서 주문 요청입니다. ID: " + id);
        }

        if (order.getStatus() != BookOrder.BookOrderStatus.PENDING) {
            throw new NotPendingStatusException();
        }

        order.approve(adminId, actionDto.getComments());
        order.setUpdatedBy(adminId);

        BookOrder savedOrder = bookOrderRepository.save(order);

        logger.info("도서 주문 요청이 승인되었습니다. ID: {}, 관리자: {}", id, adminId);

        notifyUserOfApproval(savedOrder);

        return new BookOrderDto.Response(savedOrder);
    }

    public BookOrderDto.Response rejectBookOrder(Long id, BookOrderDto.Action actionDto, String adminId) {
        BookOrder order = bookOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다. ID: " + id));

        if (order.isDeleted()) {
            throw new IllegalArgumentException("삭제된 도서 주문 요청입니다. ID: " + id);
        }

        if (order.getStatus() != BookOrder.BookOrderStatus.PENDING) {
            throw new NotPendingStatusException();
        }

        order.reject(adminId, actionDto.getComments());
        order.setUpdatedBy(adminId);

        BookOrder savedOrder = bookOrderRepository.save(order);

        logger.info("도서 주문 요청이 거부되었습니다. ID: {}, 관리자: {}", id, adminId);

        notifyUserOfRejection(savedOrder);

        return new BookOrderDto.Response(savedOrder);
    }

    public BookOrderDto.Response markAsReceived(Long id, String adminId) {
        BookOrder order = bookOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다. ID: " + id));

        if (order.isDeleted()) {
            throw new IllegalArgumentException("삭제된 도서 주문 요청입니다. ID: " + id);
        }

        if (order.getStatus() != BookOrder.BookOrderStatus.APPROVED) {
            throw new NotApprovedStatusException();
        }

        order.markAsReceived(adminId);
        order.setUpdatedBy(adminId);

        BookOrder savedOrder = bookOrderRepository.save(order);

        logger.info("도서 주문 요청이 입고 처리되었습니다. ID: {}, 관리자: {}", id, adminId);

        notifyUserOfReceival(savedOrder);

        return new BookOrderDto.Response(savedOrder);
    }

    // TODO: 알림 기능 추가
    private void notifyAdminOfNewOrder(BookOrder order) {
        logger.info("관리자에게 신규 도서 주문 요청 알림을 전송합니다. " +
                   "주문 ID: {}, 제목: {}, 요청자: {}",
                   order.getId(), order.getTitle(), order.getRequesterName());
    }

    private void notifyUserOfApproval(BookOrder order) {
        logger.info("사용자에게 도서 주문 승인 알림을 전송합니다. " +
                   "주문 ID: {}, 제목: {}, 요청자: {}",
                   order.getId(), order.getTitle(), order.getRequesterName());
    }

    private void notifyUserOfRejection(BookOrder order) {
        logger.info("사용자에게 도서 주문 거부 알림을 전송합니다. " +
                   "주문 ID: {}, 제목: {}, 요청자: {}",
                   order.getId(), order.getTitle(), order.getRequesterName());
    }

    private void notifyUserOfReceival(BookOrder order) {
        logger.info("사용자에게 도서 입고 완료 알림을 전송합니다. " +
                   "주문 ID: {}, 제목: {}, 요청자: {}",
                   order.getId(), order.getTitle(), order.getRequesterName());
    }
}
