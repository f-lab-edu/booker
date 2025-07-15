package com.bookerapp.core.presentation.dto.event;

import com.bookerapp.core.domain.model.event.EventType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CreateEventRequest {
    private String title;
    private String description;
    private EventType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int maxParticipants;
} 