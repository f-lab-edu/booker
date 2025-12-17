package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.PageResponse;
import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.repository.MemberRepository;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "4. Event", description = "이벤트 관리 API")
public class EventController {

    private final DefaultEventService defaultEventService;
    private final TechTalkEventService techTalkEventService;
    private final MemberRepository memberRepository;

    @PostMapping
    @Operation(
        summary = "이벤트 생성",
        description = "새로운 이벤트를 생성합니다.\n\n" +
                      "**이벤트 유형:**\n" +
                      "- STUDY_GROUP: 스터디 그룹\n" +
                      "- MEETUP: 밋업/모임\n" +
                      "- CONFERENCE: 컨퍼런스\n" +
                      "- TECH_TALK: 기술 발표\n" +
                      "- WORKSHOP: 워크샵\n\n" +
                      "**주의사항:**\n" +
                      "- 시작 시간은 종료 시간보다 빨라야 합니다.\n" +
                      "- 최대 참여자 수는 1 이상이어야 합니다."
    )
    public ResponseEntity<EventDto.Response> createEvent(
            @Valid @RequestBody EventDto.CreateRequest request,
            @RequestParam(required = false, defaultValue = "test-user") String userId,
            @RequestParam(required = false, defaultValue = "Test User") String username,
            @RequestParam(required = false, defaultValue = "test@example.com") String email) {
        // 기존 Member가 있으면 재사용, 없으면 새로 생성
        Member presenter = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(new Member(userId, username, email)));

        Event event = defaultEventService.createEvent(request, presenter);
        return ResponseEntity.ok(EventDto.Response.from(event));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "이벤트 수정",
        description = "기존 이벤트의 정보를 수정합니다.\n\n" +
                      "**수정 가능한 항목:**\n" +
                      "- 제목 (title)\n" +
                      "- 설명 (description)\n" +
                      "- 시작 시간 (startTime)\n" +
                      "- 종료 시간 (endTime)\n\n" +
                      "**주의사항:**\n" +
                      "- 이벤트 유형과 최대 참여자 수는 수정할 수 없습니다."
    )
    public ResponseEntity<Void> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventDto.UpdateRequest request) {
        defaultEventService.updateEvent(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "이벤트 삭제",
        description = "이벤트를 삭제합니다.\n\n" +
                      "**주의사항:**\n" +
                      "- 이미 참여자가 있는 이벤트는 삭제할 수 없습니다.\n" +
                      "- 삭제된 이벤트는 복구할 수 없습니다."
    )
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        defaultEventService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/participants")
    @Operation(
        summary = "이벤트 참가자 추가",
        description = "이벤트에 참가자를 추가합니다.\n\n" +
                      "**제약 조건:**\n" +
                      "- 최대 참여자 수를 초과할 수 없습니다.\n" +
                      "- 동일한 참가자는 중복으로 추가할 수 없습니다.\n\n" +
                      "**주의사항:**\n" +
                      "- 이 API는 관리자용입니다. 일반 사용자는 Event Participation API를 사용하세요."
    )
    public ResponseEntity<Void> addParticipant(
            @PathVariable Long id,
            @RequestParam String memberId,
            @RequestParam(required = false, defaultValue = "Test User") String memberName,
            @RequestParam(required = false, defaultValue = "test@example.com") String memberEmail) {
        // 기존 Member가 있으면 재사용, 없으면 새로 생성
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseGet(() -> memberRepository.save(new Member(memberId, memberName, memberEmail)));

        defaultEventService.addParticipant(id, member);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/participants/{memberId}")
    @Operation(
        summary = "이벤트 참가자 제거",
        description = "이벤트에서 참가자를 제거합니다.\n\n" +
                      "**주의사항:**\n" +
                      "- 이 API는 관리자용입니다."
    )
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long id,
            @PathVariable String memberId,
            @RequestParam(required = false, defaultValue = "Test User") String memberName,
            @RequestParam(required = false, defaultValue = "test@example.com") String memberEmail) {
        Member member = new Member(memberId, memberName, memberEmail);
        defaultEventService.removeParticipant(id, member);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(
        summary = "이벤트 목록 조회",
        description = "이벤트 목록을 조회합니다. 페이징 및 필터링을 지원합니다.\n\n" +
                      "**필터링:**\n" +
                      "- type: 이벤트 유형으로 필터링 (STUDY_GROUP, MEETUP, CONFERENCE, TECH_TALK, WORKSHOP)\n\n" +
                      "**페이징 파라미터:**\n" +
                      "- page: 페이지 번호 (0부터 시작)\n" +
                      "- size: 페이지 크기 (기본값: 20)\n" +
                      "- sort: 정렬 기준 (예: startTime,desc)\n\n" +
                      "**정렬 가능 필드:**\n" +
                      "- startTime: 시작 시간\n" +
                      "- endTime: 종료 시간\n" +
                      "- title: 제목\n" +
                      "- createdAt: 생성일\n\n" +
                      "**예시:**\n" +
                      "- 최신순 조회: GET /api/v1/events?page=0&size=10&sort=startTime,desc\n" +
                      "- WORKSHOP만 조회: GET /api/v1/events?type=WORKSHOP&sort=startTime,desc"
    )
    public ResponseEntity<PageResponse<EventDto.Response>> getEvents(
            @RequestParam(required = false) EventType type,
            @Parameter(
                description = "페이징 및 정렬",
                example = "{ \"page\": 0, \"size\": 20, \"sort\": [\"startTime,desc\"] }",
                schema = @io.swagger.v3.oas.annotations.media.Schema(defaultValue = "startTime,desc")
            )
            @PageableDefault(size = 20, sort = "startTime", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        Page<Event> events = type != null ?
            defaultEventService.findEventsByType(type, pageable) :
            defaultEventService.findAllEvents(pageable);
        return ResponseEntity.ok(PageResponse.of(events.map(EventDto.Response::from)));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "이벤트 상세 조회",
        description = "특정 이벤트의 상세 정보를 조회합니다.\n\n" +
                      "**응답 정보:**\n" +
                      "- 이벤트 기본 정보 (제목, 설명, 유형, 시간)\n" +
                      "- 발표자 정보\n" +
                      "- 참여자 목록"
    )
    public ResponseEntity<EventDto.Response> getEvent(@PathVariable Long id) {
        Event event = defaultEventService.findEventById(id);
        return ResponseEntity.ok(EventDto.Response.from(event));
    }
}
