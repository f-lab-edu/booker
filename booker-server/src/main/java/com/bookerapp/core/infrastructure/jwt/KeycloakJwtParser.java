package com.bookerapp.core.infrastructure.jwt;

import com.bookerapp.core.infrastructure.client.KeycloakClient;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.KeyType;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakJwtParser {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakJwtParser.class);

    private static final String JWK_KEY_ID = "kid";
    private static final String JWK_KEY_USE = "use";
    private static final String JWK_KEY_ALGORITHM = "alg";
    
    private static final String KEY_USE_SIGNATURE = "sig";

    private final KeycloakClient keycloakClient;
    private final JwtParser jwtParser;

    public KeycloakJwtParser(KeycloakClient keycloakClient, JwtParser jwtParser) {
        this.keycloakClient = keycloakClient;
        this.jwtParser = jwtParser;
    }

    public Claims parseToken(String token) {
        try {
            PublicKey publicKey = getPublicKeyFromKeycloak(token);
            return jwtParser.parseToken(token, publicKey);
        } catch (Exception e) {
            logger.error("Failed to parse Keycloak JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid Keycloak JWT token: " + e.getMessage(), e);
        }
    }

    private PublicKey getPublicKeyFromKeycloak(String token) {
        try {
            String kid = jwtParser.extractKidFromToken(token);
            List<Map<String, Object>> jwkKeys = keycloakClient.fetchJwkKeys();
            Map<String, Object> signingKey = findSigningKeyByKid(jwkKeys, kid);
            return createPublicKeyFromJwk(signingKey);
        } catch (Exception e) {
            logger.error("Failed to get public key from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get public key from Keycloak: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> findSigningKeyByKid(List<Map<String, Object>> keys, String kid) {
        Map<String, Object> matchingKey = findKeyByKidAndUse(keys, kid, KEY_USE_SIGNATURE);
        
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
            String keyId = (String) key.get(JWK_KEY_ID);
            String keyUse = (String) key.get(JWK_KEY_USE);
            String alg = (String) key.get(JWK_KEY_ALGORITHM);
            
            logger.debug("Available key - kid: {}, use: {}, alg: {}", keyId, keyUse, alg);
            
            if (kid.equals(keyId) && use.equals(keyUse)) {
                return key;
            }
        }
        return null;
    }

    private Map<String, Object> findFirstSigningKey(List<Map<String, Object>> keys) {
        for (Map<String, Object> key : keys) {
            String use = (String) key.get(JWK_KEY_USE);
            if (KEY_USE_SIGNATURE.equals(use)) {
                return key;
            }
        }
        return null;
    }

    private PublicKey createPublicKeyFromJwk(Map<String, Object> jwkKey) throws Exception {
        try {
            JWK jwk = JWK.parse(jwkKey);
            
            return switch (jwk.getKeyType().getValue()) {
                case "RSA" -> {
                    logger.debug("Creating RSA public key from JWK");
                    yield ((RSAKey) jwk).toPublicKey();
                }
                default -> throw new RuntimeException("Unsupported key type: " + jwk.getKeyType());
            };
            
        } catch (Exception e) {
            logger.error("Failed to parse JWK using Nimbus library: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create public key from JWK: " + e.getMessage(), e);
        }
    }
} 
