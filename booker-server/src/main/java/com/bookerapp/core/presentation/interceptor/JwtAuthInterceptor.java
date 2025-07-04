package com.bookerapp.core.presentation.interceptor;

import com.bookerapp.core.infrastructure.jwt.KeycloakJwtParser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final KeycloakJwtParser keycloakJwtParser;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // OPTIONS 요청은 통과
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // Swagger UI 관련 경로는 통과
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/swagger-ui") || 
            requestURI.startsWith("/v3/api-docs") || 
            requestURI.equals("/swagger-ui.html")) {
            return true;
        }

        // Authorization 헤더 확인
        String token = extractToken(request);
        if (token == null) {
            logger.warn("Missing or invalid Authorization header for request: {}", requestURI);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return false;
        }

        try {
            Claims claims = keycloakJwtParser.parseToken(token);
            
            logger.debug("JWT token validated successfully for user: {}", claims.getSubject());
            request.setAttribute("claims", claims);
            return true;
        } catch (Exception e) {
            logger.warn("JWT token validation failed for request: {} - {}", requestURI, e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid JWT token\"}");
            return false;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
} 