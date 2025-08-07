package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.dto.BookOrderActionDto;
import com.bookerapp.core.application.dto.BookOrderRequestDto;
import com.bookerapp.core.application.dto.BookOrderResponseDto;
import com.bookerapp.core.application.service.BookOrderService;
import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.entity.BookOrder;
import com.bookerapp.core.presentation.argumentresolver.UserContextArgumentResolver;
import com.bookerapp.core.presentation.interceptor.JwtAuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.assertj.core.api.Assertions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
public class BookOrderIntegrationControllerTest {

    private String toUtf8(String input) {
        if (input == null) return null;
        return new String(input.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private UserContextArgumentResolver userContextArgumentResolver;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookOrderService bookOrderService;

    @MockBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_ORDER_ID = 1L;

    private UserContext createUserContext(List<String> roles) {
        return new UserContext(TEST_USER_ID, TEST_USERNAME, TEST_EMAIL, roles);
    }

    private BookOrderRequestDto createBookOrderRequest() {
        return new BookOrderRequestDto("클린 아키텍처", "로버트 마틴", "인사이트", "9788966262472");
    }

    private BookOrderResponseDto createBookOrderResponse() {
        BookOrderResponseDto response = new BookOrderResponseDto();
        response.setId(TEST_ORDER_ID);
        response.setTitle("클린 아키텍처");
        response.setAuthor("로버트 마틴");
        response.setPublisher("인사이트");
        response.setIsbn("9788966262472");
        response.setRequesterId(TEST_USER_ID);
        response.setRequesterName(TEST_USERNAME);
        response.setStatus(BookOrder.BookOrderStatus.PENDING);
        response.setCreatedAt(LocalDateTime.now());
                  System.out.println("JVM file.encoding: " + System.getProperty("file.encoding"));  return response;
    }

    @BeforeEach
    void setUp() throws Exception {
        given(userContextArgumentResolver.supportsParameter(any())).willReturn(true);
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }

    @Nested
    class 도서주문요청_생성_통합테스트 {

        @Test
        void USER_권한으로_도서주문요청_생성_성공() throws Exception {

            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            BookOrderRequestDto request = new BookOrderRequestDto(
                toUtf8("클린 아키텍처"),
                toUtf8("로버트 마틴"),
                toUtf8("인사이트"),
                toUtf8("9788966262472")
            );
            BookOrderResponseDto response = createBookOrderResponse();
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);
            when(bookOrderService.createBookOrder(any(), anyString(), anyString())).thenReturn(response);

            String jsonContent = objectMapper.writeValueAsString(request);
            System.out.println("JSON Content: " + jsonContent); // 디버깅용

            var result = mockMvc.perform(post("/api/book-orders")
                    .contentType("application/json;charset=UTF-8")
                    .content(jsonContent))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
            String content = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(content);
            Assertions.assertThat(json.get("id").asLong()).isEqualTo(TEST_ORDER_ID);
            Assertions.assertThat(json.get("title").asText()).isEqualTo("클린 아키텍처");
            Assertions.assertThat(json.get("status").asText()).isEqualTo("PENDING");
        }

        @Test
        void ADMIN_권한으로_도서주문요청_생성_성공() throws Exception {
            System.out.println("JVM file.encoding: " + System.getProperty("file.encoding"));
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            BookOrderRequestDto request = new BookOrderRequestDto(
                toUtf8("클린 아키텍처"),
                toUtf8("로버트 마틴"),
                toUtf8("인사이트"),
                toUtf8("9788966262472")
            );
            BookOrderResponseDto response = createBookOrderResponse();
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);
            when(bookOrderService.createBookOrder(any(), anyString(), anyString())).thenReturn(response);

            var result = mockMvc.perform(post("/api/book-orders")
                    .contentType("application/json;charset=UTF-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
            String content = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(content);
            Assertions.assertThat(json.get("title").asText()).isEqualTo("클린 아키텍처");
        }

        @Test
        void 권한없는_사용자_도서주문요청_생성_실패() throws Exception {
            System.out.println("JVM file.encoding: " + System.getProperty("file.encoding"));
            UserContext noRoleContext = createUserContext(Collections.emptyList());
            BookOrderRequestDto request = new BookOrderRequestDto(
                toUtf8("클린 아키텍처"),
                toUtf8("로버트 마틴"),
                toUtf8("인사이트"),
                toUtf8("9788966262472")
            );
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(noRoleContext);

            var result = mockMvc.perform(post("/api/book-orders")
                    .contentType("application/json;charset=UTF-8")
                    .content(objectMapper.writeValueAsString(request)))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void 필수값_누락시_도서주문요청_생성_실패() throws Exception {
            System.out.println("JVM file.encoding: " + System.getProperty("file.encoding"));
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            BookOrderRequestDto invalidRequest = new BookOrderRequestDto(
                toUtf8(""),
                toUtf8("로버트 마틴"),
                toUtf8("인사이트"),
                toUtf8("9788966262472")
            );
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            var result = mockMvc.perform(post("/api/book-orders")
                    .contentType("application/json;charset=UTF-8")
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }
    }

    @Nested
    class 도서주문요청_조회_통합테스트 {

        @Test
        void USER_권한으로_내주문목록_조회_성공() throws Exception {
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            List<BookOrderResponseDto> orders = List.of(createBookOrderResponse());
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);
            when(bookOrderService.getBookOrdersByUser(anyString())).thenReturn(orders);

            var result = mockMvc.perform(get("/api/book-orders/my"))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            String content = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(content);
            Assertions.assertThat(json.isArray()).isTrue();
            Assertions.assertThat(json.get(0).get("id").asLong()).isEqualTo(TEST_ORDER_ID);
        }

        @Test
        void ADMIN_권한으로_모든주문목록_조회_성공() throws Exception {
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            List<BookOrderResponseDto> orders = List.of(createBookOrderResponse());
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);
            when(bookOrderService.getAllBookOrders()).thenReturn(orders);

            var result = mockMvc.perform(get("/api/book-orders"))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            String content = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(content);
            Assertions.assertThat(json.isArray()).isTrue();
            Assertions.assertThat(json.get(0).get("id").asLong()).isEqualTo(TEST_ORDER_ID);
        }

        @Test
        void USER_권한으로_모든주문목록_조회_실패() throws Exception {
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            var result = mockMvc.perform(get("/api/book-orders"))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void USER_권한으로_본인주문_상세조회_성공() throws Exception {
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            BookOrderResponseDto response = createBookOrderResponse();
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);
            when(bookOrderService.getBookOrder(anyLong())).thenReturn(response);

            var result = mockMvc.perform(get("/api/book-orders/{id}", TEST_ORDER_ID))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            String content = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(content);
            Assertions.assertThat(json.get("id").asLong()).isEqualTo(TEST_ORDER_ID);
            Assertions.assertThat(json.get("requesterId").asText()).isEqualTo(TEST_USER_ID);
        }

        @Test
        void USER_권한으로_타인주문_상세조회_실패() throws Exception {
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            BookOrderResponseDto response = createBookOrderResponse();
            response.setRequesterId("other-user-id");
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);
            when(bookOrderService.getBookOrder(anyLong())).thenReturn(response);

            var result = mockMvc.perform(get("/api/book-orders/{id}", TEST_ORDER_ID))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    @Nested
    class 도서주문요청_관리자처리_통합테스트 {

        @Test
        void ADMIN_권한으로_주문승인_성공() throws Exception {
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            BookOrderActionDto actionDto = new BookOrderActionDto(toUtf8("승인합니다"));
            BookOrderResponseDto response = createBookOrderResponse();
            response.setStatus(BookOrder.BookOrderStatus.APPROVED);
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);
            when(bookOrderService.approveBookOrder(anyLong(), any(), anyString())).thenReturn(response);

            var result = mockMvc.perform(post("/api/book-orders/{id}/approve", TEST_ORDER_ID)
                    .contentType("application/json;charset=UTF-8")
                    .content(objectMapper.writeValueAsString(actionDto)))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            String content = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(content);
            Assertions.assertThat(json.get("status").asText()).isEqualTo("APPROVED");
        }

        @Test
        void USER_권한으로_주문승인_실패() throws Exception {
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            BookOrderActionDto actionDto = new BookOrderActionDto(toUtf8("승인합니다"));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            var result = mockMvc.perform(post("/api/book-orders/{id}/approve", TEST_ORDER_ID)
                    .contentType("application/json;charset=UTF-8")
                    .content(objectMapper.writeValueAsString(actionDto)))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void ADMIN_권한으로_주문거부_성공() throws Exception {
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            BookOrderActionDto actionDto = new BookOrderActionDto(toUtf8("재고 부족으로 거부합니다"));
            BookOrderResponseDto response = createBookOrderResponse();
            response.setStatus(BookOrder.BookOrderStatus.REJECTED);
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);
            when(bookOrderService.rejectBookOrder(anyLong(), any(), anyString())).thenReturn(response);

            var result = mockMvc.perform(post("/api/book-orders/{id}/reject", TEST_ORDER_ID)
                    .contentType("application/json;charset=UTF-8")
                    .content(objectMapper.writeValueAsString(actionDto)))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            String content = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(content);
            Assertions.assertThat(json.get("status").asText()).isEqualTo("REJECTED");
        }

        @Test
        void ADMIN_권한으로_도서입고처리_성공() throws Exception {
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            BookOrderResponseDto response = createBookOrderResponse();
            response.setStatus(BookOrder.BookOrderStatus.RECEIVED);
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);
            when(bookOrderService.markAsReceived(anyLong(), anyString())).thenReturn(response);

            var result = mockMvc.perform(post("/api/book-orders/{id}/receive", TEST_ORDER_ID))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
            String content = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(content);
            Assertions.assertThat(json.get("status").asText()).isEqualTo("RECEIVED");
        }

        @Test
        void USER_권한으로_도서입고처리_실패() throws Exception {
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            var result = mockMvc.perform(post("/api/book-orders/{id}/receive", TEST_ORDER_ID))
                .andReturn();

            Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }
}
