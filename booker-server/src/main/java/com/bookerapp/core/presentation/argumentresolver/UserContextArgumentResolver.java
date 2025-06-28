package com.bookerapp.core.presentation.argumentresolver;

import com.bookerapp.core.domain.model.UserContext;
import com.bookerapp.core.infrastructure.config.JwtConfig;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class UserContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private JwtConfig jwtConfig;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UserContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        
        // Authorization 헤더에서 JWT 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization token is required");
        }
        
        String token = authHeader.substring(7);
        
        try {
            Claims claims = jwtConfig.parseToken(token);
            
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String username = claims.get("preferred_username", String.class);
            
            // realm_access.roles에서 역할 정보 추출
            Map<String, Object> realmAccess = claims.get("realm_access", Map.class);
            List<String> roles = Collections.emptyList();
            if (realmAccess != null && realmAccess.get("roles") != null) {
                roles = (List<String>) realmAccess.get("roles");
            }
            
            return new UserContext(userId, username, email, roles);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }
} 
