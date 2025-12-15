package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class EventDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "EventCreateRequest", description = "이벤트 생성 요청")
    public static class CreateRequest {
        @Schema(description = "이벤트 제목", example = "신년 독서 이벤트", requiredMode = Schema.RequiredMode.REQUIRED)
        private String title;

        @Schema(description = "이벤트 설명", example = "2025년 신년 맞이 독서 토론회", requiredMode = Schema.RequiredMode.REQUIRED)
        private String description;

        @Schema(description = "이벤트 유형", example = "MEETUP",
                allowableValues = {"STUDY_GROUP", "MEETUP", "CONFERENCE", "TECH_TALK", "WORKSHOP"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private EventType type;

        @Schema(description = "시작 시간 (ISO-8601 형식)", example = "2025-01-15T14:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime startTime;

        @Schema(description = "종료 시간 (ISO-8601 형식)", example = "2025-01-15T16:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime endTime;

        @Schema(description = "최대 참여자 수", example = "100", defaultValue = "50")
        private int maxParticipants = 50;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "EventUpdateRequest", description = "이벤트 수정 요청")
    public static class UpdateRequest {
        @Schema(description = "이벤트 제목", example = "신년 독서 이벤트 (수정됨)")
        private String title;

        @Schema(description = "이벤트 설명", example = "2025년 신년 맞이 독서 토론회 - 참여 인원 증가")
        private String description;

        @Schema(description = "시작 시간 (ISO-8601 형식)", example = "2025-01-15T15:00:00")
        private LocalDateTime startTime;

        @Schema(description = "종료 시간 (ISO-8601 형식)", example = "2025-01-15T17:00:00")
        private LocalDateTime endTime;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private EventType type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int maxParticipants;
        private Member presenter;
        private List<EventParticipation> participants;

        public static Response from(Event event) {
            return Response.builder()
                    .id(event.getId())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .type(event.getType())
                    .startTime(event.getStartTime())
                    .endTime(event.getEndTime())
                    .maxParticipants(event.getMaxParticipants())
                    .presenter(event.getPresenter())
                    .participants(event.getParticipants())
                    .build();
        }
    }
}
