package com.bookerapp.core.application.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class EventParticipationDto {

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Request {
        private Long eventId;
        private String memberId;
        private String memberName;
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
