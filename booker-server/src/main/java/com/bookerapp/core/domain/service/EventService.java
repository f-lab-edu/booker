package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.infrastructure.client.GoogleCalendarClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final GoogleCalendarClient googleCalendarClient;

    @Transactional
    public Event createEvent(String title, String description, EventType type,
                           LocalDateTime startTime, LocalDateTime endTime,
                           int maxParticipants, Member presenter) {
        Event event = new Event(title, description, type, startTime, endTime, maxParticipants, presenter);
        event = eventRepository.save(event);

        if (type == EventType.TECH_TALK) {
            String calendarEventId = googleCalendarClient.createEvent(
                    title,
                    description,
                    startTime,
                    endTime
            );
            event.setCalendarEventId(calendarEventId);
            event = eventRepository.save(event);
        }

        return event;
    }

    @Transactional
    public void updateEvent(Long eventId, String title, String description,
                          LocalDateTime startTime, LocalDateTime endTime) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.updateSchedule(startTime, endTime);
        event.setTitle(title);
        event.setDescription(description);

        if (event.getType() == EventType.TECH_TALK && event.getCalendarEventId() != null) {
            googleCalendarClient.updateEvent(
                    event.getCalendarEventId(),
                    title,
                    description,
                    startTime,
                    endTime
            );
        }
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getType() == EventType.TECH_TALK && event.getCalendarEventId() != null) {
            googleCalendarClient.deleteEvent(event.getCalendarEventId());
        }

        event.cancelEvent();
        eventRepository.delete(event);
    }

    @Transactional
    public void addParticipant(Long eventId, Member member) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.addParticipant(member);
    }

    @Transactional
    public void removeParticipant(Long eventId, Member member) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.removeParticipant(member);
    }

    @Transactional(readOnly = true)
    public List<Event> findAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Event> findEventsByType(EventType type) {
        return eventRepository.findByType(type);
    }

    @Transactional(readOnly = true)
    public Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }
} 