package com.bookerapp.core.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.Scopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Value("${KEYCLOAK_EXTERNAL_URL:http://localhost:8083}/realms/${keycloak.realm}/protocol/openid-connect/token")
    private String tokenUri;
    
    @Value("${KEYCLOAK_EXTERNAL_URL:http://localhost:8083}/realms/${keycloak.realm}/protocol/openid-connect/auth")
    private String authorizationUri;

    @Value("${keycloak.realm:myrealm}")
    private String realm;

    @Value("${keycloak.client-id:springboot-client}")
    private String clientId;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        SecurityScheme securityScheme = new SecurityScheme()
            .name(securitySchemeName)
            .type(SecurityScheme.Type.OAUTH2)
            .flows(new OAuthFlows()
                .authorizationCode(new OAuthFlow()
                    .authorizationUrl(authorizationUri)
                    .tokenUrl(tokenUri)
                    .scopes(new Scopes()
                        .addString("openid", "OpenID Connect")
                        .addString("profile", "Profile information")
                        .addString("email", "Email information"))
                    .refreshUrl(tokenUri)));

        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, securityScheme))
            .info(new Info()
                .title("Booker API")
                .version("1.0")
                .description("Booker REST API Documentation with OAuth2 Authentication\n\n" +
                    "Client ID: " + clientId + "\n" +
                    "Available scopes: openid, profile, email"));
    }
} 