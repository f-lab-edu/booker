package com.bookerapp.core.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${http://localhost:8083}")
    private String keycloakUrl;

    @Value("${keycloak.realm:myrealm}")
    private String realm;

    @Value("${keycloak.client-id:springboot-client}")
    private String clientId;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Booker API")
                .version("1.0")
                .description("Booker REST API Documentation\n\n" +
                    "Authentication is handled by Keycloak.\n" +
                    "Click 'Authorize' to login via Keycloak (supports Google OAuth)."))
            .addSecurityItem(new SecurityRequirement().addList("Keycloak"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("Keycloak", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("Keycloak OAuth2")
                        .flows(new OAuthFlows()
                            .authorizationCode(new OAuthFlow()
                                .authorizationUrl(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/auth")
                                .tokenUrl(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                                .refreshUrl(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                            )
                        )
                )
            );
    }
} 