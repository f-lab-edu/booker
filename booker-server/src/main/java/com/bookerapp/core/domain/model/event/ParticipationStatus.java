package com.bookerapp.core.domain.model.event;

public enum ParticipationStatus {
    CONFIRMED("확정"),
    WAITING("대기"),
    CANCELLED("취소");

    private final String description;

    ParticipationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
