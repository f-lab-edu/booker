package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
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
    
    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String tokenUri;
    
    @Value("${spring.security.oauth2.client.provider.keycloak.authorization-uri}")
    private String authorizationUri;
    
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        SecurityScheme securityScheme = new SecurityScheme()
            .name(securitySchemeName)
            .type(SecurityScheme.Type.OAUTH2)
            .flows(new OAuthFlows()
                .password(new OAuthFlow()
                    .tokenUrl(tokenUri))
                .authorizationCode(new OAuthFlow()
                    .authorizationUrl(authorizationUri)
                    .tokenUrl(tokenUri)));

        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, securityScheme))
            .info(new Info()
                .title("Booker API")
                .version("1.0")
                .description("Booker REST API Documentation with OAuth2 Authentication\n\n" +
                    "Available roles:\n" +
                    "- USER: Can read books\n" +
                    "- ADMIN: Can read, create, update, and delete books"));
    }
} 