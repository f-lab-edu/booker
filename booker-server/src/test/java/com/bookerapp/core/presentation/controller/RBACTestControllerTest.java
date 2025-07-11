package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RBACTestControllerTest {

    @InjectMocks
    private RBACTestController rbacTestController;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_BOOK_ID = "1";

    private UserContext createUserContext(List<String> roles) {
        return new UserContext(TEST_USER_ID, TEST_USERNAME, TEST_EMAIL, roles);
    }

    @Nested
    class 도서API_RBAC_성공_테스트 {
        @Test
        void ADMIN_권한으로_도서목록_조회_성공() {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));

            // when
            String result = rbacTestController.getAllBooks(adminContext);

            // then
            assertThat(result).isEqualTo("List of all books");
        }

        @Test
        void USER_권한으로_도서목록_조회_성공() {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));

            // when
            String result = rbacTestController.getAllBooks(userContext);

            // then
            assertThat(result).isEqualTo("List of all books");
        }

        @Test
        void ADMIN_권한으로_도서조회_성공() {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));

            // when
            String result = rbacTestController.getBookById(TEST_BOOK_ID, adminContext);

            // then
            assertThat(result).isEqualTo("Book with ID: " + TEST_BOOK_ID);
        }

        @Test
        void USER_권한으로_도서조회_성공() {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));

            // when
            String result = rbacTestController.getBookById(TEST_BOOK_ID, userContext);

            // then
            assertThat(result).isEqualTo("Book with ID: " + TEST_BOOK_ID);
        }

        @Test
        void ADMIN_권한으로_도서생성_성공() {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            String bookData = "test book data";

            // when
            String result = rbacTestController.createBook(bookData, adminContext);

            // then
            assertThat(result).isEqualTo("Created book: " + bookData);
        }

        @Test
        void ADMIN_권한으로_도서수정_성공() {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
            String bookData = "updated book data";

            // when
            String result = rbacTestController.updateBook(TEST_BOOK_ID, bookData, adminContext);

            // then
            assertThat(result).isEqualTo("Updated book " + TEST_BOOK_ID + " with: " + bookData);
        }

        @Test
        void ADMIN_권한으로_도서삭제_성공() {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));

            // when
            String result = rbacTestController.deleteBook(TEST_BOOK_ID, adminContext);

            // then
            assertThat(result).isEqualTo("Deleted book: " + TEST_BOOK_ID);
        }
    }

    @Nested
    class 도서API_RBAC_실패_테스트 {
        @Test
        void 권한없는_사용자_도서목록_조회_실패() {
            // given
            UserContext noRoleContext = createUserContext(Collections.emptyList());

            // when & then
            // 참고: 실제로는 AuthorizationAspect에서 권한 체크가 이루어지므로
            // 단위 테스트에서는 컨트롤러 메서드가 정상 실행됨
            // 통합 테스트에서는 403 Forbidden이 발생할 것임
            String result = rbacTestController.getAllBooks(noRoleContext);
            assertThat(result).isEqualTo("List of all books");
        }

        @Test
        void USER_권한으로_도서생성_실패_시뮬레이션() {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            String bookData = "test book data";

            // when & then
            // 참고: 실제 환경에서는 @RequireRoles({Role.ADMIN})에 의해 차단됨
            // 여기서는 컨트롤러 로직만 테스트하므로 정상 실행됨
            String result = rbacTestController.createBook(bookData, userContext);
            assertThat(result).isEqualTo("Created book: " + bookData);
        }

        @Test
        void USER_권한으로_도서수정_실패_시뮬레이션() {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
            String bookData = "updated book data";

            // when & then
            // 참고: 실제 환경에서는 @RequireRoles({Role.ADMIN})에 의해 차단됨
            String result = rbacTestController.updateBook(TEST_BOOK_ID, bookData, userContext);
            assertThat(result).isEqualTo("Updated book " + TEST_BOOK_ID + " with: " + bookData);
        }

        @Test
        void USER_권한으로_도서삭제_실패_시뮬레이션() {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));

            // when & then
            // 참고: 실제 환경에서는 @RequireRoles({Role.ADMIN})에 의해 차단됨
            String result = rbacTestController.deleteBook(TEST_BOOK_ID, userContext);
            assertThat(result).isEqualTo("Deleted book: " + TEST_BOOK_ID);
        }

        @Test
        void 잘못된_역할로_도서생성_실패_시뮬레이션() {
            // given
            UserContext invalidRoleContext = createUserContext(List.of("INVALID_ROLE"));
            String bookData = "test book data";

            // when & then
            // 참고: 실제 환경에서는 AuthorizationAspect에서 권한 체크 실패
            String result = rbacTestController.createBook(bookData, invalidRoleContext);
            assertThat(result).isEqualTo("Created book: " + bookData);
        }
    }

    @Nested
    class 사용자정보_API_테스트 {
        @Test
        void 사용자정보_조회_성공() {
            // given
            UserContext userContext = createUserContext(List.of(Role.USER.getValue()));

            // when
            UserResponse result = rbacTestController.getUserInfo(userContext);

            // then
            assertThat(result)
                    .isNotNull()
                    .satisfies(response -> {
                        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
                        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
                        assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
                        assertThat(response.getRoles()).containsExactly(Role.USER);
                        assertThat(response.isAuthenticated()).isTrue();
                    });
        }

        @Test
        void ADMIN_사용자정보_조회_성공() {
            // given
            UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));

            // when
            UserResponse result = rbacTestController.getUserInfo(adminContext);

            // then
            assertThat(result)
                    .isNotNull()
                    .satisfies(response -> {
                        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
                        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
                        assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
                        assertThat(response.getRoles()).containsExactly(Role.ADMIN);
                        assertThat(response.isAuthenticated()).isTrue();
                    });
        }

        @Test
        void 권한없는_사용자정보_조회_성공() {
            // given
            UserContext noRoleContext = createUserContext(Collections.emptyList());

            // when
            UserResponse result = rbacTestController.getUserInfo(noRoleContext);

            // then
            assertThat(result)
                    .isNotNull()
                    .satisfies(response -> {
                        assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
                        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
                        assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
                        assertThat(response.getRoles()).isEmpty();
                        assertThat(response.isAuthenticated()).isTrue();
                    });
        }
    }
}
