package com.bookerapp.core.domain.model.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 인증 응답 DTO
 */
@Getter
@Builder
@Schema(description = "인증 응답")
public class AuthResponse {

    @Schema(description = "사용자 ID")
    private String userId;

    @Schema(description = "이메일")
    private String email;

    @Schema(description = "이름")
    private String name;

    @Schema(description = "프로필 이미지 URL")
    private String picture;

    @Schema(description = "인증 성공 여부")
    private boolean authenticated;
}
