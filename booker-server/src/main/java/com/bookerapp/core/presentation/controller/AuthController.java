package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.auth.AuthResponse;
import com.bookerapp.core.domain.model.dto.auth.GoogleLoginRequest;
import com.bookerapp.core.infrastructure.security.GoogleTokenValidator;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 인증 관련 API 컨트롤러 (클라이언트 측 인증 관리)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 API")
public class AuthController {

    private final GoogleTokenValidator googleTokenValidator;

    /**
     * Google ID Token 검증
     * 클라이언트에서 받은 Google ID Token을 검증하고 사용자 정보를 반환합니다.
     */
    @PostMapping("/google/verify")
    @Operation(summary = "Google Token 검증", description = "Google ID Token을 검증하고 사용자 정보를 반환합니다")
    public ResponseEntity<AuthResponse> verifyGoogleToken(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        try {
            // Google ID Token 검증
            GoogleIdToken.Payload payload = googleTokenValidator.validateToken(request.getIdToken());

            // 사용자 정보 추출
            String userId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            log.info("Google token verified successfully for user: {}", email);

            return ResponseEntity.ok(AuthResponse.builder()
                    .userId(userId)
                    .email(email)
                    .name(name)
                    .picture(picture)
                    .authenticated(true)
                    .build());

        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to validate Google token", e);
            return ResponseEntity.status(401).body(AuthResponse.builder()
                    .authenticated(false)
                    .build());
        }
    }
}
