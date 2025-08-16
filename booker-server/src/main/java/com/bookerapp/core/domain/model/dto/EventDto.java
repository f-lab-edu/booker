package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
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
    public static class CreateRequest {
        private String title;
        private String description;
        private EventType type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int maxParticipants;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String description;
        private LocalDateTime startTime;
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
