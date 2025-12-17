package com.bookerapp.core.domain.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google 로그인 요청 DTO
 */
@Getter
@NoArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "ID token is required")
    private String idToken;
}
