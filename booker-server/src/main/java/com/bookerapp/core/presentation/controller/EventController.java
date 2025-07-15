package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.service.EventService;
import com.bookerapp.core.presentation.dto.event.CreateEventRequest;
import com.bookerapp.core.presentation.dto.event.EventResponse;
import com.bookerapp.core.presentation.dto.event.UpdateEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @RequireRoles({Role.ADMIN, Role.USER})
    public ResponseEntity<EventResponse> createEvent(
            @RequestBody CreateEventRequest request,
            UserContext userContext) {
        Event event = eventService.createEvent(
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getStartTime(),
                request.getEndTime(),
                request.getMaxParticipants(),
                userContext.getMember()
        );
        return ResponseEntity.ok(EventResponse.from(event));
    }

    @PutMapping("/{eventId}")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @RequestBody UpdateEventRequest request) {
        eventService.updateEvent(
                eventId,
                request.getTitle(),
                request.getDescription(),
                request.getStartTime(),
                request.getEndTime()
        );
        Event event = eventService.findEventById(eventId);
        return ResponseEntity.ok(EventResponse.from(event));
    }

    @DeleteMapping("/{eventId}")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{eventId}/participants")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<Void> addParticipant(
            @PathVariable Long eventId,
            UserContext userContext) {
        eventService.addParticipant(eventId, userContext.getMember());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}/participants")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long eventId,
            UserContext userContext) {
        eventService.removeParticipant(eventId, userContext.getMember());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @RequireRoles({Role.ADMIN, Role.USER})
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @RequestParam(required = false) EventType type) {
        List<Event> events = type != null ?
                eventService.findEventsByType(type) :
                eventService.findAllEvents();
        return ResponseEntity.ok(events.stream()
                .map(EventResponse::from)
                .toList());
    }

    @GetMapping("/{eventId}")''
    @RequireRoles({Role.ADMIN, Role.USER})
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        Event event = eventService.findEventById(eventId);
        return ResponseEntity.ok(EventResponse.from(event));
    }
} 