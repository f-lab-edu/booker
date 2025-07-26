package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.BookOrderDto;
import com.bookerapp.core.application.service.BookOrderService;
import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.entity.BookOrder;
import com.bookerapp.core.presentation.argumentresolver.UserContextArgumentResolver;
import com.bookerapp.core.presentation.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookOrderService bookOrderService;

    @Mock
    private UserContextArgumentResolver userContextArgumentResolver;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "/api/book-orders";
    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_ADMIN_ID = "admin-id";
    private static final String TEST_ADMIN_USERNAME = "admin-user";
    private static final Long TEST_ORDER_ID = 1L;

    private UserContext createUserContext() {
        return new UserContext(TEST_USER_ID, TEST_USERNAME, "test@example.com", List.of(Role.USER.name()));
    }

    private UserContext createAdminContext() {
        return new UserContext(TEST_ADMIN_ID, TEST_ADMIN_USERNAME, "admin@example.com", List.of(Role.ADMIN.name()));
    }

    private BookOrderDto.Request createValidRequest() {
        return new BookOrderDto.Request(
                "클린 아키텍처",
                "로버트 마틴",
                "인사이트",
                "9788966262472"
        );
    }

    private BookOrderDto.Response createResponse() {
        BookOrder order = new BookOrder(
                "클린 아키텍처",
                "로버트 마틴",
                "인사이트",
                "9788966262472",
                TEST_USER_ID,
                TEST_USERNAME
        );
        order.setId(TEST_ORDER_ID);
        return new BookOrderDto.Response(order);
    }

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new BookOrderController(bookOrderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        userContextArgumentResolver,
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();

        given(userContextArgumentResolver.supportsParameter(any())).willAnswer(invocation -> {
            MethodParameter parameter = invocation.getArgument(0);
            return parameter.getParameterType().equals(UserContext.class);
        });
    }

    @Nested
    class 도서주문요청_생성_테스트 {

        @Test
        void 도서주문요청_생성_성공() throws Exception {
            // given
            BookOrderDto.Request request = createValidRequest();
            BookOrderDto.Response response = createResponse();
            UserContext userContext = createUserContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(userContext);
            given(bookOrderService.createBookOrder(any(BookOrderDto.Request.class), anyString(), anyString()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(TEST_ORDER_ID))
                    .andExpect(jsonPath("$.title").value("클린 아키텍처"))
                    .andExpect(jsonPath("$.requesterId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.requesterName").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(bookOrderService).createBookOrder(any(BookOrderDto.Request.class), eq(TEST_USER_ID), eq(TEST_USERNAME));
        }

        @Test
        void 제목없이_도서주문요청_생성시_실패() throws Exception {
            // given
            BookOrderDto.Request request = new BookOrderDto.Request(
                    null,
                    "로버트 마틴",
                    "인사이트",
                    "9788966262472"
            );

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void 제목길이초과시_도서주문요청_생성_실패() throws Exception {
            // given
            String longTitle = "a".repeat(31);
            BookOrderDto.Request request = new BookOrderDto.Request(
                    longTitle,
                    "로버트 마틴",
                    "인사이트",
                    "9788966262472"
            );

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    class 내_도서주문요청_조회_테스트 {

        @Test
        void 내_도서주문요청_목록조회_성공() throws Exception {
            // given
            List<BookOrderDto.Response> responses = List.of(createResponse());
            Page<BookOrderDto.Response> page = new PageImpl<>(responses, PageRequest.of(0, 20), 1);
            UserContext userContext = createUserContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(userContext);
            given(bookOrderService.getBookOrdersByUser(anyString(), any(Pageable.class)))
                    .willReturn(page);

            // when & then
            mockMvc.perform(get(BASE_URL + "/my")
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(TEST_ORDER_ID))
                    .andExpect(jsonPath("$.content[0].requesterId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(bookOrderService).getBookOrdersByUser(eq(TEST_USER_ID), any(Pageable.class));
        }
    }

    @Nested
    class 모든_도서주문요청_조회_테스트 {

        @Test
        void 관리자가_모든_도서주문요청_조회_성공() throws Exception {
            // given
            List<BookOrderDto.Response> responses = List.of(createResponse());
            Page<BookOrderDto.Response> page = new PageImpl<>(responses, PageRequest.of(0, 20), 1);
            UserContext adminContext = createAdminContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(adminContext);
            given(bookOrderService.getAllBookOrders(any(Pageable.class)))
                    .willReturn(page);

            // when & then
            String result = mockMvc.perform(get(BASE_URL)
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(TEST_ORDER_ID))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andReturn().getResponse().getContentAsString();

            System.out.println("API 응답 결과: " + result);

            verify(bookOrderService).getAllBookOrders(any(Pageable.class));
        }

        @Test
        void 관리자가_상태별_도서주문요청_조회_성공() throws Exception {
            // given
            List<BookOrderDto.Response> responses = List.of(createResponse());
            Page<BookOrderDto.Response> page = new PageImpl<>(responses, PageRequest.of(0, 20), 1);
            UserContext adminContext = createAdminContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(adminContext);
            given(bookOrderService.getBookOrdersByStatus(any(BookOrder.BookOrderStatus.class), any(Pageable.class)))
                    .willReturn(page);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .param("status", "PENDING")
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(bookOrderService).getBookOrdersByStatus(eq(BookOrder.BookOrderStatus.PENDING), any(Pageable.class));
        }
    }

    @Nested
    class 도서주문요청_상세조회_테스트 {

        @Test
        void 본인_도서주문요청_상세조회_성공() throws Exception {
            // given
            BookOrderDto.Response response = createResponse();
            UserContext userContext = createUserContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(userContext);
            given(bookOrderService.getBookOrder(anyLong()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + TEST_ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_ORDER_ID))
                    .andExpect(jsonPath("$.title").value("클린 아키텍처"))
                    .andExpect(jsonPath("$.requesterId").value(TEST_USER_ID));

            verify(bookOrderService).getBookOrder(TEST_ORDER_ID);
        }

        @Test
        void 관리자가_도서주문요청_상세조회_성공() throws Exception {
            // given
            BookOrderDto.Response response = createResponse();
            UserContext adminContext = createAdminContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(adminContext);
            given(bookOrderService.getBookOrder(anyLong()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + TEST_ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_ORDER_ID))
                    .andExpect(jsonPath("$.requesterId").value(TEST_USER_ID));

            verify(bookOrderService).getBookOrder(TEST_ORDER_ID);
        }

        @Test
        void 타인의_도서주문요청_상세조회시_권한없음_응답() throws Exception {
            // given
            BookOrderDto.Response otherUserResponse = createResponse();
            otherUserResponse.setRequesterId("other-user-id");
            UserContext userContext = createUserContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(userContext);
            given(bookOrderService.getBookOrder(anyLong()))
                    .willReturn(otherUserResponse);

            // when & then
            mockMvc.perform(get(BASE_URL + "/" + TEST_ORDER_ID))
                    .andExpect(status().isForbidden());

            verify(bookOrderService).getBookOrder(TEST_ORDER_ID);
        }
    }

    @Nested
    class 도서주문요청_승인_테스트 {

        @Test
        void 관리자가_도서주문요청_승인_성공() throws Exception {
            // given
            BookOrderDto.Action actionDto = new BookOrderDto.Action("승인합니다");
            BookOrderDto.Response response = createResponse();
            response.setStatus(BookOrder.BookOrderStatus.APPROVED);
            UserContext adminContext = createAdminContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(adminContext);
            given(bookOrderService.approveBookOrder(anyLong(), any(BookOrderDto.Action.class), anyString()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + TEST_ORDER_ID + "/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_ORDER_ID))
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(bookOrderService).approveBookOrder(eq(TEST_ORDER_ID), any(BookOrderDto.Action.class), eq(TEST_ADMIN_ID));
        }
    }

    @Nested
    class 도서주문요청_거부_테스트 {

        @Test
        void 관리자가_도서주문요청_거부_성공() throws Exception {
            // given
            BookOrderDto.Action actionDto = new BookOrderDto.Action("재고 부족으로 거부합니다");
            BookOrderDto.Response response = createResponse();
            response.setStatus(BookOrder.BookOrderStatus.REJECTED);
            UserContext adminContext = createAdminContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(adminContext);
            given(bookOrderService.rejectBookOrder(anyLong(), any(BookOrderDto.Action.class), anyString()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + TEST_ORDER_ID + "/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_ORDER_ID))
                    .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(bookOrderService).rejectBookOrder(eq(TEST_ORDER_ID), any(BookOrderDto.Action.class), eq(TEST_ADMIN_ID));
        }
    }

    @Nested
    class 도서입고처리_테스트 {

        @Test
        void 관리자가_도서입고처리_성공() throws Exception {
            // given
            BookOrderDto.Response response = createResponse();
            response.setStatus(BookOrder.BookOrderStatus.RECEIVED);
            UserContext adminContext = createAdminContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(adminContext);
            given(bookOrderService.markAsReceived(anyLong(), anyString()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + TEST_ORDER_ID + "/receive"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_ORDER_ID))
                    .andExpect(jsonPath("$.status").value("RECEIVED"));

            verify(bookOrderService).markAsReceived(TEST_ORDER_ID, TEST_ADMIN_ID);
        }

        @Test
        void 존재하지않는_도서주문요청_입고처리시_실패() throws Exception {
            // given
            UserContext adminContext = createAdminContext();

            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any()))
                    .willReturn(adminContext);
            given(bookOrderService.markAsReceived(anyLong(), anyString()))
                    .willThrow(new IllegalArgumentException("도서 주문 요청을 찾을 수 없습니다"));

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + TEST_ORDER_ID + "/receive"))
                    .andExpect(status().isInternalServerError());

            verify(bookOrderService).markAsReceived(TEST_ORDER_ID, TEST_ADMIN_ID);
        }
    }
} 