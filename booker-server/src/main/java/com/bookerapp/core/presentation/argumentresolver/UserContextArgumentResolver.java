package com.bookerapp.core.presentation.argumentresolver;

import com.bookerapp.core.domain.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class UserContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UserContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        
        // 클라이언트에서 Keycloak 인증 후 전달하는 사용자 정보 헤더
        String userId = request.getHeader("X-User-ID");
        String email = request.getHeader("X-User-Email");
        String username = request.getHeader("X-Username");
        String rolesHeader = request.getHeader("X-Roles");
        
        List<String> roles = rolesHeader != null && !rolesHeader.isEmpty()
            ? Arrays.asList(rolesHeader.split(","))
            : Collections.emptyList();
        
        return new UserContext(userId, username, email, roles);
    }
} 