package com.bookerapp.core.presentation.dto.event;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public class EventDto {

    @Getter
    @NoArgsConstructor
    @Schema(name = "EventCreateRequest", description = "이벤트 생성 요청")
    public static class CreateRequest {

        @Schema(
            description = "이벤트 제목 (필수, 1-200자)",
            example = "2025 신년 독서 토론회",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 200
        )
        @NotBlank(message = "이벤트 제목은 필수입니다")
        @Size(min = 1, max = 200, message = "제목은 1-200자 이내여야 합니다")
        private String title;

        @Schema(
            description = "이벤트 상세 설명 (선택, 최대 2000자)",
            example = "2025년을 맞이하여 올해의 독서 목표를 공유하고 추천 도서를 토론하는 시간입니다.",
            maxLength = 2000
        )
        @Size(max = 2000, message = "설명은 2000자 이내여야 합니다")
        private String description;

        @Schema(
            description = "이벤트 유형 (필수)\n" +
                          "- STUDY_GROUP: 정기적인 스터디 모임\n" +
                          "- MEETUP: 비정기적 모임/네트워킹\n" +
                          "- CONFERENCE: 대규모 컨퍼런스\n" +
                          "- TECH_TALK: 기술 발표/세미나\n" +
                          "- WORKSHOP: 실습 중심 워크샵",
            example = "MEETUP",
            allowableValues = {"STUDY_GROUP", "MEETUP", "CONFERENCE", "TECH_TALK", "WORKSHOP"},
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "이벤트 유형은 필수입니다")
        private EventType type;

        @Schema(
            description = "이벤트 시작 시간 (필수, ISO-8601 형식)\n종료 시간보다 빨라야 합니다",
            example = "2025-01-20T14:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "시작 시간은 필수입니다")
        @Future(message = "시작 시간은 미래여야 합니다")
        private LocalDateTime startTime;

        @Schema(
            description = "이벤트 종료 시간 (필수, ISO-8601 형식)\n시작 시간보다 늦어야 합니다",
            example = "2025-01-20T16:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "종료 시간은 필수입니다")
        @Future(message = "종료 시간은 미래여야 합니다")
        private LocalDateTime endTime;

        @Schema(
            description = "최대 참여자 수 (필수, 1-1000명)\n정원 초과 시 자동으로 대기 목록에 추가됩니다",
            example = "50",
            defaultValue = "50",
            minimum = "1",
            maximum = "1000"
        )
        @Min(value = 1, message = "최대 참여자 수는 최소 1명이어야 합니다")
        @Max(value = 1000, message = "최대 참여자 수는 최대 1000명입니다")
        private int maxParticipants = 50;
    }

    @Getter
    @NoArgsConstructor
    @Schema(name = "EventUpdateRequest", description = "이벤트 수정 요청")
    public static class UpdateRequest {

        @Schema(
            description = "이벤트 제목 (선택, 1-200자)",
            example = "2025 신년 독서 토론회 (시간 변경)",
            minLength = 1,
            maxLength = 200
        )
        @Size(min = 1, max = 200, message = "제목은 1-200자 이내여야 합니다")
        private String title;

        @Schema(
            description = "이벤트 상세 설명 (선택, 최대 2000자)",
            example = "일정 변경으로 시간이 조정되었습니다.",
            maxLength = 2000
        )
        @Size(max = 2000, message = "설명은 2000자 이내여야 합니다")
        private String description;

        @Schema(
            description = "이벤트 시작 시간 (선택, ISO-8601 형식)",
            example = "2025-01-21T14:00:00"
        )
        private LocalDateTime startTime;

        @Schema(
            description = "이벤트 종료 시간 (선택, ISO-8601 형식)",
            example = "2025-01-21T16:00:00"
        )
        private LocalDateTime endTime;
    }

    @Getter
    @Builder
    @Schema(name = "EventResponse", description = "이벤트 응답")
    public static class Response {

        @Schema(description = "이벤트 고유 ID", example = "1")
        private Long id;

        @Schema(description = "이벤트 제목", example = "2025 신년 독서 토론회")
        private String title;

        @Schema(description = "이벤트 설명", example = "2025년을 맞이하여 올해의 독서 목표를 공유하고 추천 도서를 토론하는 시간입니다.")
        private String description;

        @Schema(description = "이벤트 유형", example = "MEETUP")
        private EventType type;

        @Schema(description = "시작 시간 (ISO-8601)", example = "2025-01-20T14:00:00")
        private LocalDateTime startTime;

        @Schema(description = "종료 시간 (ISO-8601)", example = "2025-01-20T16:00:00")
        private LocalDateTime endTime;

        @Schema(description = "최대 참여자 수", example = "50")
        private int maxParticipants;

        @Schema(description = "발표자/진행자 정보")
        private MemberDto presenter;

        @Schema(description = "참여자 목록 (확정 및 대기)")
        private List<ParticipantResponse> participants;

        @Schema(description = "만석 여부", example = "false")
        private boolean isFullyBooked;

        @Schema(description = "현재 확정 참여자 수", example = "45")
        private int confirmedCount;

        @Schema(description = "대기 인원 수", example = "5")
        private int waitingCount;

        @Schema(description = "참가 가능 여부", example = "true")
        private boolean available;

        public static Response from(Event event) {
            // Calculate counts
            long confirmedCount = event.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
                .count();
            long waitingCount = event.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipationStatus.WAITING)
                .count();

            return Response.builder()
                    .id(event.getId())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .type(event.getType())
                    .startTime(event.getStartTime())
                    .endTime(event.getEndTime())
                    .maxParticipants(event.getMaxParticipants())
                    .presenter(MemberDto.from(event.getPresenter()))
                    .participants(event.getParticipants().stream()
                            .map(ParticipantResponse::from)
                            .toList())
                    .isFullyBooked(event.isFullyBooked())
                    .confirmedCount((int) confirmedCount)
                    .waitingCount((int) waitingCount)
                    .available(confirmedCount < event.getMaxParticipants())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(name = "MemberDto", description = "회원 정보")
    public static class MemberDto {

        @Schema(description = "회원 ID", example = "1")
        private Long id;

        @Schema(description = "회원 고유 식별자", example = "test-user")
        private String userId;

        @Schema(description = "회원 이름", example = "홍길동")
        private String name;

        @Schema(description = "이메일", example = "hong@example.com")
        private String email;

        public static MemberDto from(Member member) {
            return MemberDto.builder()
                    .id(member.getId())
                    .userId(member.getMemberId())
                    .name(member.getName())
                    .email(member.getEmail())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(name = "ParticipantResponse", description = "참여자 정보")
    public static class ParticipantResponse {

        @Schema(description = "참여 기록 ID", example = "1")
        private Long id;

        @Schema(description = "참여자 ID", example = "1")
        private Long memberId;

        @Schema(description = "참여자 이름", example = "김철수")
        private String memberName;

        @Schema(description = "참여 상태", example = "CONFIRMED",
                allowableValues = {"CONFIRMED", "WAITING", "CANCELLED"})
        private ParticipationStatus status;

        @Schema(description = "등록 일시", example = "2025-01-15T10:30:00")
        private LocalDateTime registrationDate;

        @Schema(description = "대기 번호 (WAITING 상태인 경우)", example = "3")
        private Integer waitingNumber;

        public static ParticipantResponse from(EventParticipation participation) {
            return ParticipantResponse.builder()
                    .id(participation.getId())
                    .memberId(participation.getParticipant().getId())
                    .memberName(participation.getParticipant().getName())
                    .status(participation.getStatus())
                    .registrationDate(participation.getRegistrationDate())
                    .waitingNumber(participation.getWaitingNumber())
                    .build();
        }
    }

    @Getter
    public static class PageResponse {
        private final List<Response> content;
        private final int pageNumber;
        private final int pageSize;
        private final long totalElements;
        private final int totalPages;
        private final boolean last;

        public static PageResponse from(Page<Event> page) {
            return new PageResponse(
                page.getContent().stream().map(Response::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
            );
        }

        @Builder
        private PageResponse(List<Response> content, int pageNumber, int pageSize,
                           long totalElements, int totalPages, boolean last) {
            this.content = content;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.last = last;
        }
    }
}
