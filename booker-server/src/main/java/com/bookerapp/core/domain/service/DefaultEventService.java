package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.dto.EventDto;
import org.springframework.stereotype.Service;

@Service
public class DefaultEventService extends AbstractEventService {

    public DefaultEventService(EventRepository eventRepository) {
        super(eventRepository);
    }

    @Override
    protected void handleEventCreation(Event event, EventDto.CreateRequest request) {
        // Basic event creation logic is handled in the abstract class
    }

    @Override
    protected void handleEventUpdate(Event event, EventDto.UpdateRequest request) {
        // Basic event update logic is handled in the abstract class
    }

    @Override
    protected void handleEventDeletion(Event event) {
        // Basic event deletion logic is handled in the abstract class
    }

    @Override
    protected void handleParticipantAddition(Event event, Member member) {
        // Basic participant addition logic is handled in the abstract class
    }

    @Override
    protected void handleParticipantRemoval(Event event, Member member) {
        // Basic participant removal logic is handled in the abstract class
    }
} 