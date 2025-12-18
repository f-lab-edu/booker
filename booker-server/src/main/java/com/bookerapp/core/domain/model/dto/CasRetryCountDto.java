package com.bookerapp.core.domain.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * CAS Retry Count DTO
 * CAS 방식 참여 신청의 재시도 횟수 정보 전송 객체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CasRetryCountResponse", description = "CAS 재시도 횟수 응답")
public class CasRetryCountDto {

    @Schema(
            description = "재시도 횟수 - CAS 방식 참여 신청 시 발생한 총 재시도 횟수",
            example = "42"
    )
    private int retryCount;

    @Schema(
            description = "조회 시점 - 재시도 횟수를 조회한 시간",
            example = "2025-12-18T10:30:00"
    )
    private LocalDateTime queriedAt;

    @Schema(
            description = "메시지 - 추가 정보 (reset 시에만 사용)",
            example = "CAS retry count has been reset"
    )
    private String message;

    /**
     * 조회용 생성자 (message 없음)
     */
    public CasRetryCountDto(int retryCount, LocalDateTime queriedAt) {
        this.retryCount = retryCount;
        this.queriedAt = queriedAt;
    }
}
