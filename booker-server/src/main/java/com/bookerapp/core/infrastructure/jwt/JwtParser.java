package com.bookerapp.core.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.util.Base64;

@Component
public class JwtParser {
    private static final Logger logger = LoggerFactory.getLogger(JwtParser.class);

    public Claims parseToken(String token, PublicKey publicKey) {
        try {
            logger.debug("Parsing JWT token...");

            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Failed to parse JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid JWT token: " + e.getMessage(), e);
        }
    }

    public String extractKidFromToken(String token) {
        String headerEncoded = token.split("\\.")[0];
        String headerJson = new String(Base64.getUrlDecoder().decode(headerEncoded));
        return extractKidFromHeader(headerJson);
    }

    private String extractKidFromHeader(String headerJson) {
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
