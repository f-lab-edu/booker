package com.bookerapp.core.infrastructure.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Google ID Token 검증을 위한 컴포넌트
 */
@Slf4j
@Component
public class GoogleTokenValidator {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenValidator(@Value("${google.oauth.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Google ID Token을 검증하고 payload를 반환합니다.
     *
     * @param idTokenString Google에서 발급한 ID Token
     * @return 검증된 토큰의 payload
     * @throws GeneralSecurityException 토큰 검증 실패
     * @throws IOException 네트워크 오류
     */
    public GoogleIdToken.Payload validateToken(String idTokenString)
            throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            log.warn("Invalid Google ID Token");
            throw new GeneralSecurityException("Invalid ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // 이메일 검증 여부 확인
        if (!payload.getEmailVerified()) {
            log.warn("Email not verified for user: {}", payload.getEmail());
            throw new GeneralSecurityException("Email not verified");
        }

        log.info("Successfully validated Google token for user: {}", payload.getEmail());
        return payload;
    }
}
