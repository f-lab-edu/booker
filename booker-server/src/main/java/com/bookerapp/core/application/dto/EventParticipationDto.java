package com.bookerapp.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class EventParticipationDto {

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(name = "EventParticipationRequest", description = "이벤트 참여 신청 요청")
    public static class Request {
        @Schema(description = "이벤트 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long eventId;

        @Schema(description = "참여자 ID", example = "member001", requiredMode = Schema.RequiredMode.REQUIRED)
        private String memberId;

        @Schema(description = "참여자 이름", example = "김철수", requiredMode = Schema.RequiredMode.REQUIRED)
        private String memberName;

        @Schema(description = "참여자 이메일", example = "member001@test.com", requiredMode = Schema.RequiredMode.REQUIRED)
        private String memberEmail;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Response {
        private Long participationId;
        private String status;
        private Integer waitingNumber;
        private String message;
    }
}
