package com.bookerapp.core.application.service;

import com.bookerapp.core.application.dto.BookOrderDto;
import com.bookerapp.core.domain.model.entity.BookOrder;
import com.bookerapp.core.infrastructure.repository.BookOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookOrderServiceTest {

    @Mock
    private BookOrderRepository bookOrderRepository;

    @InjectMocks
    private BookOrderService bookOrderService;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_ADMIN_ID = "admin-id";
    private static final Long TEST_ORDER_ID = 1L;

    private BookOrder createTestBookOrder() {
        BookOrder order = new BookOrder(
                "클린 아키텍처",
                "로버트 마틴",
                "인사이트",
                "9788966262472",
                TEST_USER_ID,
                TEST_USERNAME
        );
        order.setId(TEST_ORDER_ID);
        order.setCreatedBy(TEST_USER_ID);
        order.setUpdatedBy(TEST_USER_ID);
        return order;
    }

    private BookOrderDto.Request createTestBookOrderRequest() {
        return new BookOrderDto.Request(
                "클린 아키텍처",
                "로버트 마틴",
                "인사이트",
                "9788966262472"
        );
    }

    @Nested
    class 도서주문요청_생성_테스트 {

        @Test
        void 도서주문요청_생성_성공() {
            // given
            BookOrderDto.Request request = createTestBookOrderRequest();
            BookOrder savedOrder = createTestBookOrder();

            given(bookOrderRepository.save(any(BookOrder.class))).willReturn(savedOrder);

            // when
            BookOrderDto.Response result = bookOrderService.createBookOrder(request, TEST_USER_ID, TEST_USERNAME);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("클린 아키텍처");
            assertThat(result.getRequesterId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getRequesterName()).isEqualTo(TEST_USERNAME);
            assertThat(result.getStatus()).isEqualTo(BookOrder.BookOrderStatus.PENDING);

            verify(bookOrderRepository).save(any(BookOrder.class));
        }

        @Test
        void 도서주문요청_생성시_BaseEntity_필드_설정_확인() {
            // given
            BookOrderDto.Request request = createTestBookOrderRequest();
            BookOrder savedOrder = createTestBookOrder();

            given(bookOrderRepository.save(any(BookOrder.class))).willReturn(savedOrder);

            // when
            bookOrderService.createBookOrder(request, TEST_USER_ID, TEST_USERNAME);

            // then
            verify(bookOrderRepository).save(argThat(order ->
                order.getCreatedBy() != null &&
                order.getUpdatedBy() != null &&
                order.getTitle().equals("클린 아키텍처")
            ));
        }
    }

    @Nested
    class 도서주문요청_조회_테스트 {

        @Test
        void 사용자별_도서주문요청_목록_조회_성공() {
            // given
            List<BookOrder> orders = List.of(createTestBookOrder());
            given(bookOrderRepository.findByRequesterIdAndIsDeletedFalse(anyString())).willReturn(orders);

            // when
            List<BookOrderDto.Response> result = bookOrderService.getBookOrdersByUser(TEST_USER_ID);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRequesterId()).isEqualTo(TEST_USER_ID);

            verify(bookOrderRepository).findByRequesterIdAndIsDeletedFalse(TEST_USER_ID);
        }

        @Test
        void 모든_도서주문요청_목록_조회_성공() {
            // given
            List<BookOrder> orders = List.of(createTestBookOrder());
            given(bookOrderRepository.findAllByIsDeletedFalse()).willReturn(orders);

            // when
            List<BookOrderDto.Response> result = bookOrderService.getAllBookOrders();

            // then
            assertThat(result).hasSize(1);
            verify(bookOrderRepository).findAllByIsDeletedFalse();
        }

        @Test
        void 상태별_도서주문요청_목록_조회_성공() {
            // given
            List<BookOrder> orders = List.of(createTestBookOrder());
            given(bookOrderRepository.findByStatusOrderByCreatedAtDesc(any())).willReturn(orders);

            // when
            List<BookOrderDto.Response> result = bookOrderService.getBookOrdersByStatus(BookOrder.BookOrderStatus.PENDING);

            // then
            assertThat(result).hasSize(1);
            verify(bookOrderRepository).findByStatusOrderByCreatedAtDesc(BookOrder.BookOrderStatus.PENDING);
        }

        @Test
        void 도서주문요청_상세조회_성공() {
            // given
            BookOrder order = createTestBookOrder();
            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(order));

            // when
            BookOrderDto.Response result = bookOrderService.getBookOrder(TEST_ORDER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_ORDER_ID);
            verify(bookOrderRepository).findById(TEST_ORDER_ID);
        }

        @Test
        void 존재하지않는_도서주문요청_조회시_예외발생() {
            // given
            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookOrderService.getBookOrder(TEST_ORDER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("도서 주문 요청을 찾을 수 없습니다");

            verify(bookOrderRepository).findById(TEST_ORDER_ID);
        }

        @Test
        void 삭제된_도서주문요청_조회시_예외발생() {
            // given
            BookOrder deletedOrder = createTestBookOrder();
            deletedOrder.markAsDeleted();
            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(deletedOrder));

            // when & then
            assertThatThrownBy(() -> bookOrderService.getBookOrder(TEST_ORDER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("삭제된 도서 주문 요청입니다");
        }
    }

    @Nested
    class 도서주문요청_승인_테스트 {

        @Test
        void 도서주문요청_승인_성공() {
            // given
            BookOrder order = createTestBookOrder();
            BookOrderDto.Action actionDto = new BookOrderDto.Action("승인합니다");

            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(order));
            given(bookOrderRepository.save(any(BookOrder.class))).willReturn(order);

            // when
            BookOrderDto.Response result = bookOrderService.approveBookOrder(TEST_ORDER_ID, actionDto, TEST_ADMIN_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(order.getStatus()).isEqualTo(BookOrder.BookOrderStatus.APPROVED);
            assertThat(order.getApprovedBy()).isEqualTo(TEST_ADMIN_ID);
            assertThat(order.getAdminComments()).isEqualTo("승인합니다");
            assertThat(order.getApprovedAt()).isNotNull();

            verify(bookOrderRepository).findById(TEST_ORDER_ID);
            verify(bookOrderRepository).save(order);
        }

        @Test
        void 이미_처리된_도서주문요청_승인시_예외발생() {
            // given
            BookOrder approvedOrder = createTestBookOrder();
            approvedOrder.approve(TEST_ADMIN_ID, "이미 승인됨");
            BookOrderDto.Action actionDto = new BookOrderDto.Action("승인합니다");

            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(approvedOrder));

            // when & then
            assertThatThrownBy(() -> bookOrderService.approveBookOrder(TEST_ORDER_ID, actionDto, TEST_ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("승인 대기 중인 요청만 승인할 수 있습니다");
        }

        @Test
        void 존재하지않는_도서주문요청_승인시_예외발생() {
            // given
            BookOrderDto.Action actionDto = new BookOrderDto.Action("승인합니다");
            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookOrderService.approveBookOrder(TEST_ORDER_ID, actionDto, TEST_ADMIN_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("도서 주문 요청을 찾을 수 없습니다");
        }
    }

    @Nested
    class 도서주문요청_거부_테스트 {

        @Test
        void 도서주문요청_거부_성공() {
            // given
            BookOrder order = createTestBookOrder();
            BookOrderDto.Action actionDto = new BookOrderDto.Action("재고 부족으로 거부합니다");

            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(order));
            given(bookOrderRepository.save(any(BookOrder.class))).willReturn(order);

            // when
            BookOrderDto.Response result = bookOrderService.rejectBookOrder(TEST_ORDER_ID, actionDto, TEST_ADMIN_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(order.getStatus()).isEqualTo(BookOrder.BookOrderStatus.REJECTED);
            assertThat(order.getApprovedBy()).isEqualTo(TEST_ADMIN_ID);
            assertThat(order.getAdminComments()).isEqualTo("재고 부족으로 거부합니다");
            assertThat(order.getApprovedAt()).isNotNull();

            verify(bookOrderRepository).findById(TEST_ORDER_ID);
            verify(bookOrderRepository).save(order);
        }

        @Test
        void 이미_처리된_도서주문요청_거부시_예외발생() {
            // given
            BookOrder rejectedOrder = createTestBookOrder();
            rejectedOrder.reject(TEST_ADMIN_ID, "이미 거부됨");
            BookOrderDto.Action actionDto = new BookOrderDto.Action("거부합니다");

            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(rejectedOrder));

            // when & then
            assertThatThrownBy(() -> bookOrderService.rejectBookOrder(TEST_ORDER_ID, actionDto, TEST_ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("승인 대기 중인 요청만 거부할 수 있습니다");
        }
    }

    @Nested
    class 도서입고처리_테스트 {

        @Test
        void 도서입고처리_성공() {
            // given
            BookOrder approvedOrder = createTestBookOrder();
            approvedOrder.approve(TEST_ADMIN_ID, "승인됨");

            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(approvedOrder));
            given(bookOrderRepository.save(any(BookOrder.class))).willReturn(approvedOrder);

            // when
            BookOrderDto.Response result = bookOrderService.markAsReceived(TEST_ORDER_ID, TEST_ADMIN_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(approvedOrder.getStatus()).isEqualTo(BookOrder.BookOrderStatus.RECEIVED);
            assertThat(approvedOrder.getReceivedBy()).isEqualTo(TEST_ADMIN_ID);
            assertThat(approvedOrder.getReceivedAt()).isNotNull();

            verify(bookOrderRepository).findById(TEST_ORDER_ID);
            verify(bookOrderRepository).save(approvedOrder);
        }

        @Test
        void 승인되지않은_도서주문요청_입고처리시_예외발생() {
            // given
            BookOrder pendingOrder = createTestBookOrder();
            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(pendingOrder));

            // when & then
            assertThatThrownBy(() -> bookOrderService.markAsReceived(TEST_ORDER_ID, TEST_ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("승인된 요청만 입고 처리할 수 있습니다");
        }

        @Test
        void 거부된_도서주문요청_입고처리시_예외발생() {
            // given
            BookOrder rejectedOrder = createTestBookOrder();
            rejectedOrder.reject(TEST_ADMIN_ID, "거부됨");
            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(rejectedOrder));

            // when & then
            assertThatThrownBy(() -> bookOrderService.markAsReceived(TEST_ORDER_ID, TEST_ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("승인된 요청만 입고 처리할 수 있습니다");
        }

        @Test
        void 존재하지않는_도서주문요청_입고처리시_예외발생() {
            // given
            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookOrderService.markAsReceived(TEST_ORDER_ID, TEST_ADMIN_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("도서 주문 요청을 찾을 수 없습니다");
        }

        @Test
        void 삭제된_도서주문요청_입고처리시_예외발생() {
            // given
            BookOrder deletedOrder = createTestBookOrder();
            deletedOrder.markAsDeleted();
            given(bookOrderRepository.findById(anyLong())).willReturn(Optional.of(deletedOrder));

            // when & then
            assertThatThrownBy(() -> bookOrderService.markAsReceived(TEST_ORDER_ID, TEST_ADMIN_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("삭제된 도서 주문 요청입니다");
        }
    }
}
