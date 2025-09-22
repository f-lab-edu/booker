// package com.bookerapp.core.presentation.aspect;

// import com.bookerapp.core.domain.model.auth.Role;
// import com.bookerapp.core.domain.model.auth.UserContext;
// import org.aspectj.lang.JoinPoint;
// import org.aspectj.lang.reflect.MethodSignature;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.lang.reflect.Method;
// import java.util.Collections;
// import java.util.List;

// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.assertj.core.api.Assertions.assertThatCode;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// class AuthorizationAspectTest {

//     @InjectMocks
//     private AuthorizationAspect authorizationAspect;

//     @Mock
//     private JoinPoint joinPoint;

//     @Mock
//     private MethodSignature methodSignature;

//     private static final String TEST_USER_ID = "test-user-id";
//     private static final String TEST_USERNAME = "test-user";
//     private static final String TEST_EMAIL = "test@example.com";

//     private UserContext createUserContext(List<String> roles) {
//         return new UserContext(TEST_USER_ID, TEST_USERNAME, TEST_EMAIL, roles);
//     }

//     private Method createMockMethod(Role... requiredRoles) throws NoSuchMethodException {
//         // 실제 컨트롤러 메서드를 사용하여 @RequireRoles 어노테이션을 가져옴
//         if (requiredRoles.length == 1 && requiredRoles[0] == Role.ADMIN) {
//             return MockController.class.getMethod("adminOnlyMethod", UserContext.class);
//         } else {
//             return MockController.class.getMethod("userAndAdminMethod", UserContext.class);
//         }
//     }

//     // 테스트용 Mock Controller
//     private static class MockController {
//         @RequireRoles({Role.ADMIN})
//         public void adminOnlyMethod(UserContext userContext) {}

//         @RequireRoles({Role.USER, Role.ADMIN})
//         public void userAndAdminMethod(UserContext userContext) {}
//     }

//     @BeforeEach
//     void setUp() {
//         when(joinPoint.getSignature()).thenReturn(methodSignature);
//     }

//     @Nested
//     class 권한체크_성공_테스트 {
//         @Test
//         void ADMIN_권한으로_ADMIN_전용_메서드_접근_성공() throws Exception {
//             // given
//             UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
//             Method adminMethod = createMockMethod(Role.ADMIN);

//             when(joinPoint.getArgs()).thenReturn(new Object[]{adminContext});
//             when(methodSignature.getMethod()).thenReturn(adminMethod);

//             // when & then
//             assertThatCode(() -> authorizationAspect.checkRoles(joinPoint))
//                     .doesNotThrowAnyException();
//         }

//         @Test
//         void USER_권한으로_USER_허용_메서드_접근_성공() throws Exception {
//             // given
//             UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
//             Method userMethod = createMockMethod(Role.USER, Role.ADMIN);

//             when(joinPoint.getArgs()).thenReturn(new Object[]{userContext});
//             when(methodSignature.getMethod()).thenReturn(userMethod);

//             // when & then
//             assertThatCode(() -> authorizationAspect.checkRoles(joinPoint))
//                     .doesNotThrowAnyException();
//         }

//         @Test
//         void ADMIN_권한으로_USER_허용_메서드_접근_성공() throws Exception {
//             // given
//             UserContext adminContext = createUserContext(List.of(Role.ADMIN.getValue()));
//             Method userMethod = createMockMethod(Role.USER, Role.ADMIN);

//             when(joinPoint.getArgs()).thenReturn(new Object[]{adminContext});
//             when(methodSignature.getMethod()).thenReturn(userMethod);

//             // when & then
//             assertThatCode(() -> authorizationAspect.checkRoles(joinPoint))
//                     .doesNotThrowAnyException();
//         }
//     }

//     @Nested
//     class 권한체크_실패_테스트 {
//         @Test
//         void USER_권한으로_ADMIN_전용_메서드_접근_실패() throws Exception {
//             // given
//             UserContext userContext = createUserContext(List.of(Role.USER.getValue()));
//             Method adminMethod = createMockMethod(Role.ADMIN);

//             when(joinPoint.getArgs()).thenReturn(new Object[]{userContext});
//             when(methodSignature.getMethod()).thenReturn(adminMethod);

//             // when & then
//             assertThatThrownBy(() -> authorizationAspect.checkRoles(joinPoint))
//                     .isInstanceOf(IllegalStateException.class)
//                     .hasMessageContaining("User does not have required roles")
//                     .hasMessageContaining("[ADMIN]");
//         }

//         @Test
//         void 권한없는_사용자_ADMIN_전용_메서드_접근_실패() throws Exception {
//             // given
//             UserContext noRoleContext = createUserContext(Collections.emptyList());
//             Method adminMethod = createMockMethod(Role.ADMIN);

//             when(joinPoint.getArgs()).thenReturn(new Object[]{noRoleContext});
//             when(methodSignature.getMethod()).thenReturn(adminMethod);

//             // when & then
//             assertThatThrownBy(() -> authorizationAspect.checkRoles(joinPoint))
//                     .isInstanceOf(IllegalStateException.class)
//                     .hasMessageContaining("User does not have required roles")
//                     .hasMessageContaining("[ADMIN]");
//         }

//         @Test
//         void 권한없는_사용자_USER_허용_메서드_접근_실패() throws Exception {
//             // given
//             UserContext noRoleContext = createUserContext(Collections.emptyList());
//             Method userMethod = createMockMethod(Role.USER, Role.ADMIN);

//             when(joinPoint.getArgs()).thenReturn(new Object[]{noRoleContext});
//             when(methodSignature.getMethod()).thenReturn(userMethod);

//             // when & then
//             assertThatThrownBy(() -> authorizationAspect.checkRoles(joinPoint))
//                     .isInstanceOf(IllegalStateException.class)
//                     .hasMessageContaining("User does not have required roles")
//                     .hasMessageContaining("[USER, ADMIN]");
//         }

//         @Test
//         void 잘못된_권한으로_ADMIN_전용_메서드_접근_실패() throws Exception {
//             // given
//             UserContext invalidRoleContext = createUserContext(List.of("INVALID_ROLE"));
//             Method adminMethod = createMockMethod(Role.ADMIN);

//             when(joinPoint.getArgs()).thenReturn(new Object[]{invalidRoleContext});
//             when(methodSignature.getMethod()).thenReturn(adminMethod);

//             // when & then
//             assertThatThrownBy(() -> authorizationAspect.checkRoles(joinPoint))
//                     .isInstanceOf(IllegalStateException.class)
//                     .hasMessageContaining("User does not have required roles")
//                     .hasMessageContaining("[ADMIN]");
//         }
//     }

//     @Nested
//     class UserContext_없는_경우_테스트 {
//         @Test
//         void UserContext_없는_메서드_호출_실패() throws Exception {
//             // given
//             Method adminMethod = createMockMethod(Role.ADMIN);
//             when(joinPoint.getArgs()).thenReturn(new Object[]{"not_user_context"});
//             when(methodSignature.getMethod()).thenReturn(adminMethod);

//             // when & then
//             assertThatThrownBy(() -> authorizationAspect.checkRoles(joinPoint))
//                     .isInstanceOf(IllegalStateException.class)
//                     .hasMessageContaining("UserContext not found in method arguments");
//         }
//     }
// }
