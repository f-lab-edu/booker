package com.bookerapp.core.presentation.aspect;

import com.bookerapp.core.domain.model.UserContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class AuthorizationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationAspect.class);

    @Before("@annotation(requireRoles)")
    public void checkAuthorization(JoinPoint joinPoint, RequireRoles requireRoles) {
        UserContext userContext = extractUserContextFromArgs(joinPoint.getArgs());
        
        if (userContext == null || userContext.getUserId() == null) {
            logger.warn("User not authenticated - Method: {}", joinPoint.getSignature().getName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        
        String[] requiredRoles = requireRoles.value();
        if (requiredRoles.length > 0 && !userContext.hasAnyRole(requiredRoles)) {
            logger.warn("Insufficient permissions - User roles: {}, Required roles: {}, Method: {}", 
                       userContext.getRoles(), java.util.Arrays.toString(requiredRoles), 
                       joinPoint.getSignature().getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
        
        logger.debug("Access granted - User: {}, Roles: {}, Method: {}", 
                    userContext.getUserId(), userContext.getRoles(), 
                    joinPoint.getSignature().getName());
    }
    
    private UserContext extractUserContextFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UserContext) {
                return (UserContext) arg;
            }
        }
        return null;
    }
} 
