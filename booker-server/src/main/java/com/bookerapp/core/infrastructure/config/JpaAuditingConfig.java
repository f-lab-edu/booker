package com.bookerapp.core.infrastructure.config;

import com.bookerapp.core.domain.model.auth.UserContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            if (RequestContextHolder.getRequestAttributes() == null) {
                return Optional.of("system");
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.of("system");
            }

            UserContext userContext = (UserContext) attributes.getRequest().getAttribute("userContext");
            return Optional.ofNullable(userContext)
                    .map(UserContext::getUserId)
                    .or(() -> Optional.of("system"));
        };
    }
} 