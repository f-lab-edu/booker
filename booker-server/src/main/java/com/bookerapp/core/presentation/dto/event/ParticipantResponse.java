package com.bookerapp.core.presentation.dto.event;

import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ParticipantResponse {
    private Long id;
    private Long memberId;
    private String memberName;
    private ParticipationStatus status;
    private LocalDateTime registrationDate;
    private Integer waitingNumber;

    public static ParticipantResponse from(EventParticipation participation) {
        return ParticipantResponse.builder()
                .id(participation.getId())
                .memberId(participation.getParticipant().getId())
                .memberName(participation.getParticipant().getName())
                .status(participation.getStatus())
                .registrationDate(participation.getRegistrationDate())
                .waitingNumber(participation.getWaitingNumber())
                .build();
    }
} 