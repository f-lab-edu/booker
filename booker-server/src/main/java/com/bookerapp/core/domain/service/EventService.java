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
import com.bookerapp.core.domain.dto.EventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final GoogleCalendarClient googleCalendarClient;

    @Transactional
    public Event createEvent(EventDto.CreateRequest request, Member presenter) {
        Event event = new Event(
            request.getTitle(),
            request.getDescription(),
            request.getType(),
            request.getStartTime(),
            request.getEndTime(),
            request.getMaxParticipants(),
            presenter
        );
        event = eventRepository.save(event);

        if (request.getType() == EventType.TECH_TALK) {
            String calendarEventId = googleCalendarClient.createEvent(
                    request.getTitle(),
                    request.getDescription(),
                    request.getStartTime(),
                    request.getEndTime()
            );
            event.setCalendarEventId(calendarEventId);
            event = eventRepository.save(event);
        }

        return event;
    }

    @Transactional
    public void updateEvent(Long eventId, EventDto.UpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.updateSchedule(request.getStartTime(), request.getEndTime());
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());

        if (event.getType() == EventType.TECH_TALK && event.getCalendarEventId() != null) {
            googleCalendarClient.updateEvent(
                    event.getCalendarEventId(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getStartTime(),
                    request.getEndTime()
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
    public Page<Event> findAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> findEventsByType(EventType type, Pageable pageable) {
        return eventRepository.findByType(type, pageable);
    }

    @Transactional(readOnly = true)
    public Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }
} 