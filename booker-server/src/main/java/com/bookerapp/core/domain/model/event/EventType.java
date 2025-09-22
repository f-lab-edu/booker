package com.bookerapp.core.domain.model.event;

public enum EventType {
    TECH_TALK("기술 세미나"),
    WORKSHOP("워크샵"),
    STUDY_GROUP("스터디 그룹"),
    CONFERENCE("컨퍼런스"),
    MEETUP("밋업");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
