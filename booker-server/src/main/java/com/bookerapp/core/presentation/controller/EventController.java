package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.service.EventService;
import com.bookerapp.core.presentation.dto.event.EventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<EventDto.Response> createEvent(
            @RequestBody EventDto.CreateRequest request,
            UserContext userContext) {
        Event event = eventService.createEvent(request, userContext.getMember());
        return ResponseEntity.ok(EventDto.Response.from(event));
    }

    @PutMapping("/{eventId}")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<EventDto.Response> updateEvent(
            @PathVariable Long eventId,
            @RequestBody EventDto.UpdateRequest request) {
        eventService.updateEvent(eventId, request);
        Event event = eventService.findEventById(eventId);
        return ResponseEntity.ok(EventDto.Response.from(event));
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
    public ResponseEntity<EventDto.PageResponse> getEvents(
            @RequestParam(required = true) EventType type,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Event> events = eventService.findEventsByType(type, pageable);
        return ResponseEntity.ok(EventDto.PageResponse.from(events));
    }

    @GetMapping("/{eventId}")
    @RequireRoles({Role.ADMIN, Role.USER})
    public ResponseEntity<EventDto.Response> getEvent(@PathVariable Long eventId) {
        Event event = eventService.findEventById(eventId);
        return ResponseEntity.ok(EventDto.Response.from(event));
    }
} 