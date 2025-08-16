package com.bookerapp.core.domain.model.event;

import com.bookerapp.core.domain.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventParticipation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member participant;

    @Enumerated(EnumType.STRING)
    private ParticipationStatus status;

    private LocalDateTime registrationDate;

    private Integer waitingNumber;

    public EventParticipation(Event event, Member participant, ParticipationStatus status) {
        this(event, participant, status, null);
    }

    public EventParticipation(Event event, Member participant, ParticipationStatus status, Integer waitingNumber) {
        this.event = event;
        this.participant = participant;
        this.status = status;
        this.registrationDate = LocalDateTime.now();
        this.waitingNumber = waitingNumber;
    }

    public void changeStatus(ParticipationStatus newStatus) {
        this.status = newStatus;
        notifyStatusChange();
    }

    public void cancelParticipation() {
        this.status = ParticipationStatus.CANCELLED;
        this.waitingNumber = null;
        notifyStatusChange();
    }

    public void promoteToParticipant() {
        this.status = ParticipationStatus.CONFIRMED;
        this.waitingNumber = null;
        notifyStatusChange();
    }

    public void updateWaitingNumber(int newWaitingNumber) {
        if (this.status == ParticipationStatus.WAITING) {
            this.waitingNumber = newWaitingNumber;
        }
    }

    public void notifyStatusChange() {
        // TODO: Implement notification logic
    }
} 