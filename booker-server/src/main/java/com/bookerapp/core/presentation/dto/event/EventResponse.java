package com.bookerapp.core.presentation.dto.event;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private EventType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int maxParticipants;
    private Member presenter;
    private List<ParticipantResponse> participants;
    private boolean isFullyBooked;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .type(event.getType())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .maxParticipants(event.getMaxParticipants())
                .presenter(event.getPresenter())
                .participants(event.getParticipants().stream()
                        .map(ParticipantResponse::from)
                        .toList())
                .isFullyBooked(event.isFullyBooked())
                .build();
    }
} 