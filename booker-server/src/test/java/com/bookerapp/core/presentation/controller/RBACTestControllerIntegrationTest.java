package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.presentation.argumentresolver.UserContextArgumentResolver;
import com.bookerapp.core.infrastructure.client.KeycloakClient;
import com.bookerapp.core.infrastructure.jwt.KeycloakJwtParser;
import com.bookerapp.core.infrastructure.jwt.JwtParser;
import com.bookerapp.core.presentation.interceptor.JwtAuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;




@SpringBootTest
@AutoConfigureMockMvc
class RBACTestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserContextArgumentResolver userContextArgumentResolver;

    @MockBean
    private KeycloakClient keycloakClient;

    @MockBean
    private KeycloakJwtParser keycloakJwtParser;

    @MockBean
    private JwtParser jwtParser;

    @MockBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_BOOK_ID = "1";

    private UserContext createUserContext(List<String> roles) {
        return new UserContext(TEST_USER_ID, TEST_USERNAME, TEST_EMAIL, roles);
    }

    @BeforeEach
    void setUp() throws Exception {
        given(userContextArgumentResolver.supportsParameter(any())).willReturn(true);
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }

    @Nested
    class 도서API_RBAC_성공_통합테스트 {
        @Test
        void ADMIN_권한으로_도서목록_조회_성공() throws Exception {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);

            // when & then
            mockMvc.perform(get("/api/test/books")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("List of all books"));
        }

        @Test
        void USER_권한으로_도서목록_조회_성공() throws Exception {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            // when & then
            mockMvc.perform(get("/api/test/books")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("List of all books"));
        }

        @Test
        void ADMIN_권한으로_도서생성_성공() throws Exception {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);

            // when & then
            mockMvc.perform(post("/api/test/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("test book data"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Created book: test book data"));
        }

        @Test
        void ADMIN_권한으로_도서수정_성공() throws Exception {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);

            // when & then
            mockMvc.perform(put("/api/test/books/{id}", TEST_BOOK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("updated book data"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Updated book " + TEST_BOOK_ID + " with: updated book data"));
        }

        @Test
        void ADMIN_권한으로_도서삭제_성공() throws Exception {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);

            // when & then
            mockMvc.perform(delete("/api/test/books/{id}", TEST_BOOK_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Deleted book: " + TEST_BOOK_ID));
        }
    }

    @Nested
    class 도서API_RBAC_실패_통합테스트 {
        @Test
        void 권한없는_사용자_도서목록_조회_실패() throws Exception {
            // given
            UserContext noRoleContext = createUserContext(Collections.emptyList());
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(noRoleContext);

            // when & then
            mockMvc.perform(get("/api/test/books")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        void USER_권한으로_도서생성_실패() throws Exception {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            // when & then
            mockMvc.perform(post("/api/test/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("test book data"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void USER_권한으로_도서수정_실패() throws Exception {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            // when & then
            mockMvc.perform(put("/api/test/books/{id}", TEST_BOOK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("updated book data"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void USER_권한으로_도서삭제_실패() throws Exception {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            // when & then
            mockMvc.perform(delete("/api/test/books/{id}", TEST_BOOK_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        void 잘못된_권한으로_도서생성_실패() throws Exception {
            // given
            UserContext invalidRoleContext = createUserContext(List.of("INVALID_ROLE"));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(invalidRoleContext);

            // when & then
            mockMvc.perform(post("/api/test/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("test book data"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class 사용자정보_API_통합테스트 {
        @Test
        void 사용자정보_조회_성공() throws Exception {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userContext);

            // when & then
            mockMvc.perform(get("/api/test/user/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.roles[0]").value("USER"))
                    .andExpect(jsonPath("$.authenticated").value(true));
        }

        @Test
        void ADMIN_사용자정보_조회_성공() throws Exception {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(adminContext);

            // when & then
            mockMvc.perform(get("/api/test/user/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                    .andExpect(jsonPath("$.authenticated").value(true));
        }

        @Test
        void 권한없는_사용자정보_조회_성공() throws Exception {
            // given
            UserContext noRoleContext = createUserContext(Collections.emptyList());
            given(userContextArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(noRoleContext);

            // when & then
            mockMvc.perform(get("/api/test/user/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.roles").isEmpty())
                    .andExpect(jsonPath("$.authenticated").value(true));
        }
    }


}
