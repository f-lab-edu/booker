package com.bookerapp.core.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate = new RestTemplate();

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
            // JWT 헤더에서 kid 추출
            String headerEncoded = token.split("\\.")[0];
            String headerJson = new String(Base64.getUrlDecoder().decode(headerEncoded));
            
            // 간단한 JSON 파싱으로 kid 추출
            String kid = extractKidFromHeader(headerJson);
            logger.debug("JWT kid: {}", kid);
            
            String jwksUri = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
            logger.debug("Fetching JWK Set from: {}", jwksUri);
            
            Map<String, Object> jwks = restTemplate.getForObject(jwksUri, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            
            // kid가 매칭되는 키 찾기
            Map<String, Object> matchingKey = null;
            for (Map<String, Object> key : keys) {
                String keyId = (String) key.get("kid");
                String use = (String) key.get("use");
                String alg = (String) key.get("alg");
                
                logger.debug("Available key - kid: {}, use: {}, alg: {}", keyId, use, alg);
                
                if (kid.equals(keyId) && "sig".equals(use)) {
                    matchingKey = key;
                    break;
                }
            }
            
            if (matchingKey == null) {
                // kid가 매칭되지 않으면 서명용 첫 번째 키 사용
                for (Map<String, Object> key : keys) {
                    String use = (String) key.get("use");
                    if ("sig".equals(use)) {
                        matchingKey = key;
                        logger.warn("Using first available signing key instead of kid match");
                        break;
                    }
                }
            }
            
            if (matchingKey == null) {
                throw new RuntimeException("No suitable signing key found in JWK Set");
            }
            
            String n = (String) matchingKey.get("n");
            String e = (String) matchingKey.get("e");
            
            byte[] nBytes = Base64.getUrlDecoder().decode(n);
            byte[] eBytes = Base64.getUrlDecoder().decode(e);
            
            BigInteger modulus = new BigInteger(1, nBytes);
            BigInteger exponent = new BigInteger(1, eBytes);
            
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            
            PublicKey publicKey = factory.generatePublic(spec);
            logger.debug("Successfully created public key from JWK");
            
            return publicKey;
        } catch (Exception e) {
            logger.error("Failed to get public key from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get public key from Keycloak: " + e.getMessage(), e);
        }
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