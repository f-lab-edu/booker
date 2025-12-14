package com.bookerapp.core.application.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class LoadTestDto {

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class ParticipationRequest {
        private Long eventId;
        private Long userId;

        public EventParticipationDto.Request toEventParticipationRequest() {
            return new EventParticipationDto.Request(
                    this.eventId,
                    "loadtest-user-" + this.userId,
                    "LoadTest User " + this.userId,
                    "loadtest-user-" + this.userId + "@example.com"
            );
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class SetupRequest {
        private Long eventId;
        private String eventTitle;
        private Integer maxParticipants;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class SetupResponse {
        private Long eventId;
        private String message;
        private Integer maxParticipants;
    }
}