package com.bookerapp.core.domain.model.event;

import com.bookerapp.core.domain.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private int maxParticipants;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member presenter;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventParticipation> participants = new ArrayList<>();

    @Transient
    private Queue<EventParticipation> waitingList = new LinkedList<>();

    public Event(String title, String description, EventType type, LocalDateTime startTime,
                LocalDateTime endTime, int maxParticipants, Member presenter) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxParticipants = maxParticipants;
        this.presenter = presenter;
    }

    public void addParticipant(Member member) {
        if (isFullyBooked()) {
            EventParticipation participation = new EventParticipation(this, member, ParticipationStatus.WAITING);
            waitingList.offer(participation);
            return;
        }

        EventParticipation participation = new EventParticipation(this, member, ParticipationStatus.CONFIRMED);
        participants.add(participation);
    }

    public void removeParticipant(Member member) {
        participants.removeIf(p -> p.getParticipant().equals(member));
        promoteFromWaitingList();
    }

    public void updateSchedule(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void cancelEvent() {
        participants.forEach(EventParticipation::cancelParticipation);
        participants.clear();
        waitingList.clear();
    }

    public boolean isFullyBooked() {
        return participants.size() >= maxParticipants;
    }

    public void promoteFromWaitingList() {
        if (isFullyBooked() || waitingList.isEmpty()) {
            return;
        }

        EventParticipation nextParticipation = waitingList.poll();
        if (nextParticipation != null) {
            nextParticipation.promoteToParticipant();
            participants.add(nextParticipation);
        }
    }
} 
