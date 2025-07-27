package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.dto.EventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractEventService {

    protected final EventRepository eventRepository;

    protected AbstractEventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

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
        
        handleEventCreation(event, request);
        
        return event;
    }

    @Transactional
    public void updateEvent(Long eventId, EventDto.UpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.updateSchedule(request.getStartTime(), request.getEndTime());
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());

        handleEventUpdate(event, request);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        handleEventDeletion(event);
        
        event.cancelEvent();
        eventRepository.delete(event);
    }

    @Transactional
    public void addParticipant(Long eventId, Member member) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.addParticipant(member);
        handleParticipantAddition(event, member);
    }

    @Transactional
    public void removeParticipant(Long eventId, Member member) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.removeParticipant(member);
        handleParticipantRemoval(event, member);
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

    protected abstract void handleEventCreation(Event event, EventDto.CreateRequest request);
    
    protected abstract void handleEventUpdate(Event event, EventDto.UpdateRequest request);
    
    protected abstract void handleEventDeletion(Event event);
    
    protected abstract void handleParticipantAddition(Event event, Member member);
    
    protected abstract void handleParticipantRemoval(Event event, Member member);
} 