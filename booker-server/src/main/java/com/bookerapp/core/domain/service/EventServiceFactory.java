package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.event.EventType;
import org.springframework.stereotype.Component;

@Component
public class EventServiceFactory {
    
    private final TechTalkEventService techTalkEventService;
    private final DefaultEventService defaultEventService;

    public EventServiceFactory(TechTalkEventService techTalkEventService, DefaultEventService defaultEventService) {
        this.techTalkEventService = techTalkEventService;
        this.defaultEventService = defaultEventService;
    }

    public AbstractEventService getEventService(EventType eventType) {
        return switch (eventType) {
            case TECH_TALK -> techTalkEventService;
            default -> defaultEventService;
        };
    }
} 