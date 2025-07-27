package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.infrastructure.client.GoogleCalendarClient;
import com.bookerapp.core.domain.dto.EventDto;
import org.springframework.stereotype.Service;

@Service
public class TechTalkEventService extends AbstractEventService {
    
    private final GoogleCalendarClient googleCalendarClient;

    public TechTalkEventService(EventRepository eventRepository, GoogleCalendarClient googleCalendarClient) {
        super(eventRepository);
        this.googleCalendarClient = googleCalendarClient;
    }

    @Override
    protected void handleEventCreation(Event event, EventDto.CreateRequest request) {
        String calendarEventId = googleCalendarClient.createEvent(
            request.getTitle(),
            request.getDescription(),
            request.getStartTime(),
            request.getEndTime()
        );
        event.setCalendarEventId(calendarEventId);
        eventRepository.save(event);
    }

    @Override
    protected void handleEventUpdate(Event event, EventDto.UpdateRequest request) {
        if (event.getCalendarEventId() != null) {
            googleCalendarClient.updateEvent(
                event.getCalendarEventId(),
                request.getTitle(),
                request.getDescription(),
                request.getStartTime(),
                request.getEndTime()
            );
        }
    }

    @Override
    protected void handleEventDeletion(Event event) {
        if (event.getCalendarEventId() != null) {
            googleCalendarClient.deleteEvent(event.getCalendarEventId());
        }
    }

    @Override
    protected void handleParticipantAddition(Event event, Member member) {
        // TechTalk specific participant addition logic if needed
    }

    @Override
    protected void handleParticipantRemoval(Event event, Member member) {
        // TechTalk specific participant removal logic if needed
    }
} 