package com.bookerapp.core.presentation.aspect;

import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuthorizationAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationAspect.class);

    @Before("@annotation(com.bookerapp.core.presentation.aspect.RequireRoles)")
    public void checkRoles(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireRoles requireRoles = signature.getMethod().getAnnotation(RequireRoles.class);

        UserContext userContext = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg instanceof UserContext)
                .map(arg -> (UserContext) arg)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("UserContext not found in method arguments"));

        List<String> requiredRoles = Arrays.stream(requireRoles.value())
                .map(Role::getValue)
                .collect(Collectors.toList());

        boolean hasRequiredRole = userContext.getRoles().stream()
                .anyMatch(requiredRoles::contains);

        if (!hasRequiredRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User does not have required roles: " + requiredRoles);
        }

        logger.debug("Access granted - User: {}, Roles: {}, Method: {}",
                    userContext.getUserId(), userContext.getRoles(),
                    joinPoint.getSignature().getName());
    }
}
