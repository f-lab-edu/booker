package com.bookerapp.core.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class JwtConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    private final WebClient webClient;

    public JwtConfig(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Claims parseToken(String token) {
        try {
            logger.debug("Parsing JWT token...");
            
            // JWT 헤더에서 kid 추출
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                throw new RuntimeException("Invalid JWT token format");
            }
            
            String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
            logger.debug("JWT Header: {}", headerJson);
            
            // Keycloak의 JWK Set에서 공개키 가져오기
            PublicKey publicKey = getPublicKeyFromKeycloak(token);
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                    
            logger.debug("JWT token parsed successfully for user: {}", claims.getSubject());
            return claims;
        } catch (Exception e) {
            logger.error("Failed to parse JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid JWT token: " + e.getMessage(), e);
        }
    }

    private PublicKey getPublicKeyFromKeycloak(String token) {
        try {
            String kid = extractKidFromToken(token);
            List<Map<String, Object>> jwkKeys = fetchJwkKeysFromKeycloak();
            Map<String, Object> signingKey = findSigningKeyByKid(jwkKeys, kid);
            return createRsaPublicKey(signingKey);
        } catch (Exception e) {
            logger.error("Failed to get public key from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get public key from Keycloak: " + e.getMessage(), e);
        }
    }

    private String extractKidFromToken(String token) {
        String headerEncoded = token.split("\\.")[0];
        String headerJson = new String(Base64.getUrlDecoder().decode(headerEncoded));
        String kid = extractKidFromHeader(headerJson);
        logger.debug("JWT kid: {}", kid);
        return kid;
    }

    private List<Map<String, Object>> fetchJwkKeysFromKeycloak() {
        String jwksUri = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
        logger.debug("Fetching JWK Set from: {}", jwksUri);
        
        Map<String, Object> jwks = webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return (List<Map<String, Object>>) jwks.get("keys");
    }

    private Map<String, Object> findSigningKeyByKid(List<Map<String, Object>> keys, String kid) {
        Map<String, Object> matchingKey = findKeyByKidAndUse(keys, kid, "sig");
        
        if (matchingKey == null) {
            matchingKey = findFirstSigningKey(keys);
            logger.warn("Using first available signing key instead of kid match");
        }
        
        if (matchingKey == null) {
            throw new RuntimeException("No suitable signing key found in JWK Set");
        }
        
        return matchingKey;
    }

    private Map<String, Object> findKeyByKidAndUse(List<Map<String, Object>> keys, String kid, String use) {
        for (Map<String, Object> key : keys) {
            String keyId = (String) key.get("kid");
            String keyUse = (String) key.get("use");
            String alg = (String) key.get("alg");
            
            logger.debug("Available key - kid: {}, use: {}, alg: {}", keyId, keyUse, alg);
            
            if (kid.equals(keyId) && use.equals(keyUse)) {
                return key;
            }
        }
        return null;
    }

    private Map<String, Object> findFirstSigningKey(List<Map<String, Object>> keys) {
        for (Map<String, Object> key : keys) {
            String use = (String) key.get("use");
            if ("sig".equals(use)) {
                return key;
            }
        }
        return null;
    }

    private PublicKey createRsaPublicKey(Map<String, Object> jwkKey) throws Exception {
        String n = (String) jwkKey.get("n");
        String e = (String) jwkKey.get("e");
        
        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);
        
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);
        
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        
        PublicKey publicKey = factory.generatePublic(spec);
        logger.debug("Successfully created public key from JWK");
        
        return publicKey;
    }
    
    private String extractKidFromHeader(String headerJson) {
        // 간단한 JSON 파싱으로 kid 추출
        String[] parts = headerJson.split("\"kid\"\\s*:\\s*\"");
        if (parts.length > 1) {
            String kidPart = parts[1];
            int endIndex = kidPart.indexOf("\"");
            if (endIndex > 0) {
                return kidPart.substring(0, endIndex);
            }
        }
        throw new RuntimeException("Could not extract kid from JWT header");
    }
} 
