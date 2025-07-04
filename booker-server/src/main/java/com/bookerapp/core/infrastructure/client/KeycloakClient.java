package com.bookerapp.core.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class KeycloakClient {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakClient.class);

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    private final WebClient webClient;

    public KeycloakClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public List<Map<String, Object>> fetchJwkKeys() {
        String jwksUri = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
        logger.debug("Fetching JWK Set from: {}", jwksUri);
        
        Map<String, Object> jwks = webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
                
        return (List<Map<String, Object>>) jwks.get("keys");
    }
} 