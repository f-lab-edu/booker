package com.bookerapp.core.infrastructure.jwt;

import com.bookerapp.core.infrastructure.client.KeycloakClient;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakJwtParser {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakJwtParser.class);

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
            return createRsaPublicKey(signingKey);
        } catch (Exception e) {
            logger.error("Failed to get public key from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get public key from Keycloak: " + e.getMessage(), e);
        }
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
} 