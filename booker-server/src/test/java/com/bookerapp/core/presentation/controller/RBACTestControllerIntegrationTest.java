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
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Collections;
import java.util.List;

class RBACTestControllerTest {
    private RBACTestController controller;
    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_BOOK_ID = "1";

    private UserContext createUserContext(List<String> roles) {
        return new UserContext(TEST_USER_ID, TEST_USERNAME, TEST_EMAIL, roles);
    }

    @BeforeEach
    void setUp() {
        controller = new RBACTestController();
    }

    @Nested
    class 도서API_RBAC_성공_단위테스트 {
        @Test
        void ADMIN_권한으로_도서목록_조회_성공() {
            UserContext adminContext = createUserContext(List.of("ADMIN"));
            String result = controller.getAllBooks(adminContext);
            assertThat(result).isEqualTo("List of all books");
        }

        @Test
        void USER_권한으로_도서목록_조회_성공() {
            UserContext userContext = createUserContext(List.of("USER"));
            String result = controller.getAllBooks(userContext);
            assertThat(result).isEqualTo("List of all books");
        }

        @Test
        void ADMIN_권한으로_도서생성_성공() {
            UserContext adminContext = createUserContext(List.of("ADMIN"));
            String result = controller.createBook("test book data", adminContext);
            assertThat(result).isEqualTo("Created book: test book data");
        }

        @Test
        void ADMIN_권한으로_도서수정_성공() {
            UserContext adminContext = createUserContext(List.of("ADMIN"));
            String result = controller.updateBook(TEST_BOOK_ID, "updated book data", adminContext);
            assertThat(result).isEqualTo("Updated book " + TEST_BOOK_ID + " with: updated book data");
        }

        @Test
        void ADMIN_권한으로_도서삭제_성공() {
            UserContext adminContext = createUserContext(List.of("ADMIN"));
            String result = controller.deleteBook(TEST_BOOK_ID, adminContext);
            assertThat(result).isEqualTo("Deleted book: " + TEST_BOOK_ID);
        }
    }

    @Nested
    class 사용자정보_API_단위테스트 {
        @Test
        void 사용자정보_조회_성공() {
            UserContext userContext = createUserContext(List.of("USER"));
            var response = controller.getUserInfo(userContext);
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getRoles().get(0).name()).isEqualTo("USER");
            assertThat(response.isAuthenticated()).isTrue();
        }

        @Test
        void ADMIN_사용자정보_조회_성공() {
            UserContext adminContext = createUserContext(List.of("ADMIN"));
            var response = controller.getUserInfo(adminContext);
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getRoles().get(0).name()).isEqualTo("ADMIN");
            assertThat(response.isAuthenticated()).isTrue();
        }

        @Test
        void 권한없는_사용자정보_조회_성공() {
            UserContext noRoleContext = createUserContext(Collections.emptyList());
            var response = controller.getUserInfo(noRoleContext);
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getRoles()).isEmpty();
            assertThat(response.isAuthenticated()).isTrue();
        }
    }
}
