package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.service.DefaultEventService;
import com.bookerapp.core.domain.service.TechTalkEventService;
import com.bookerapp.core.domain.model.dto.EventDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Event", description = "이벤트 관리 API")
public class EventController {

    private final DefaultEventService defaultEventService;
    private final TechTalkEventService techTalkEventService;

    @PostMapping
    @Operation(summary = "이벤트 생성")
    public ResponseEntity<EventDto.Response> createEvent(
            @Valid @RequestBody EventDto.CreateRequest request,
            @Parameter(hidden = true) UserContext userContext) {
        Member presenter = new Member(userContext.getUserId(), userContext.getUsername(), userContext.getEmail());
        Event event = defaultEventService.createEvent(request, presenter);
        return ResponseEntity.ok(EventDto.Response.from(event));
    }

    @PutMapping("/{id}")
    @Operation(summary = "이벤트 수정")
    public ResponseEntity<Void> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventDto.UpdateRequest request,
            @Parameter(hidden = true) UserContext userContext) {
        defaultEventService.updateEvent(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "이벤트 삭제")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @Parameter(hidden = true) UserContext userContext) {
        defaultEventService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/participants")
    @Operation(summary = "이벤트 참가자 추가")
    public ResponseEntity<Void> addParticipant(
            @PathVariable Long id,
            @RequestParam String memberId,
            @Parameter(hidden = true) UserContext userContext) {
        Member member = new Member(memberId, "Test User", "test@example.com");
        defaultEventService.addParticipant(id, member);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/participants/{memberId}")
    @Operation(summary = "이벤트 참가자 제거")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long id,
            @PathVariable String memberId,
            @Parameter(hidden = true) UserContext userContext) {
        Member member = new Member(memberId, "Test User", "test@example.com");
        defaultEventService.removeParticipant(id, member);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "이벤트 목록 조회")
    public ResponseEntity<Page<EventDto.Response>> getEvents(
            @RequestParam(required = false) EventType type,
            Pageable pageable,
            @Parameter(hidden = true) UserContext userContext) {
        Page<Event> events = type != null ?
            defaultEventService.findEventsByType(type, pageable) :
            defaultEventService.findAllEvents(pageable);
        return ResponseEntity.ok(events.map(EventDto.Response::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "이벤트 상세 조회")
    public ResponseEntity<EventDto.Response> getEvent(
            @PathVariable Long id,
            @Parameter(hidden = true) UserContext userContext) {
        Event event = defaultEventService.findEventById(id);
        return ResponseEntity.ok(EventDto.Response.from(event));
    }
}
