package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.BookOrderActionDto;
import com.bookerapp.core.application.dto.BookOrderRequestDto;
import com.bookerapp.core.application.dto.BookOrderResponseDto;
import com.bookerapp.core.domain.model.entity.BookOrder;
import com.bookerapp.core.infrastructure.repository.BookOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookOrderService {

    private static final Logger logger = LoggerFactory.getLogger(BookOrderService.class);

    private final BookOrderRepository bookOrderRepository;

    public BookOrderResponseDto createBookOrder(BookOrderRequestDto requestDto, String requesterId, String requesterName) {
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

        return new BookOrderResponseDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<BookOrderResponseDto> getBookOrdersByUser(String userId) {
        List<BookOrder> orders = bookOrderRepository.findByRequesterIdAndIsDeletedFalse(userId);
        return orders.stream()
                    .map(BookOrderResponseDto::new)
                    .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookOrderResponseDto> getAllBookOrders() {
        List<BookOrder> orders = bookOrderRepository.findAllByIsDeletedFalse();
        return orders.stream()
                    .map(BookOrderResponseDto::new)
                    .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookOrderResponseDto> getBookOrdersByStatus(BookOrder.BookOrderStatus status) {
        List<BookOrder> orders = bookOrderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orders.stream()
                    .map(BookOrderResponseDto::new)
                    .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookOrderResponseDto getBookOrder(Long id) {
        BookOrder order = bookOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다. ID: " + id));

        if (order.isDeleted()) {
            throw new IllegalArgumentException("삭제된 도서 주문 요청입니다. ID: " + id);
        }

        return new BookOrderResponseDto(order);
    }

    public BookOrderResponseDto approveBookOrder(Long id, BookOrderActionDto actionDto, String adminId) {
        BookOrder order = bookOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다. ID: " + id));

        if (order.isDeleted()) {
            throw new IllegalArgumentException("삭제된 도서 주문 요청입니다. ID: " + id);
        }

        if (order.getStatus() != BookOrder.BookOrderStatus.PENDING) {
            throw new IllegalStateException("승인 대기 중인 요청만 승인할 수 있습니다.");
        }

        order.approve(adminId, actionDto.getComments());
        order.setUpdatedBy(adminId);

        BookOrder savedOrder = bookOrderRepository.save(order);

        logger.info("도서 주문 요청이 승인되었습니다. ID: {}, 관리자: {}", id, adminId);

        notifyUserOfApproval(savedOrder);

        return new BookOrderResponseDto(savedOrder);
    }

    public BookOrderResponseDto rejectBookOrder(Long id, BookOrderActionDto actionDto, String adminId) {
        BookOrder order = bookOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다. ID: " + id));

        if (order.isDeleted()) {
            throw new IllegalArgumentException("삭제된 도서 주문 요청입니다. ID: " + id);
        }

        if (order.getStatus() != BookOrder.BookOrderStatus.PENDING) {
            throw new IllegalStateException("승인 대기 중인 요청만 거부할 수 있습니다.");
        }

        order.reject(adminId, actionDto.getComments());
        order.setUpdatedBy(adminId);

        BookOrder savedOrder = bookOrderRepository.save(order);

        logger.info("도서 주문 요청이 거부되었습니다. ID: {}, 관리자: {}", id, adminId);

        notifyUserOfRejection(savedOrder);

        return new BookOrderResponseDto(savedOrder);
    }

    public BookOrderResponseDto markAsReceived(Long id, String adminId) {
        BookOrder order = bookOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다. ID: " + id));

        if (order.isDeleted()) {
            throw new IllegalArgumentException("삭제된 도서 주문 요청입니다. ID: " + id);
        }

        if (order.getStatus() != BookOrder.BookOrderStatus.APPROVED) {
            throw new IllegalStateException("승인된 요청만 입고 처리할 수 있습니다.");
        }

        order.markAsReceived(adminId);
        order.setUpdatedBy(adminId);

        BookOrder savedOrder = bookOrderRepository.save(order);

        logger.info("도서 주문 요청이 입고 처리되었습니다. ID: {}, 관리자: {}", id, adminId);

        notifyUserOfReceival(savedOrder);

        return new BookOrderResponseDto(savedOrder);
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
