package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class EventDto {

    @Getter
    @Builder
    @Schema(name = "EventParticipant", description = "이벤트 참여자 정보")
    public static class ParticipantDto {
        @Schema(description = "참여 ID", example = "1")
        private Long id;

        @Schema(description = "참여자 회원 정보")
        private MemberDto member;

        @Schema(description = "참여 상태", example = "CONFIRMED")
        private ParticipationStatus status;

        @Schema(description = "신청일시", example = "2025-01-15T10:30:00")
        private LocalDateTime registrationDate;

        @Schema(description = "대기 번호 (대기 상태인 경우)", example = "1")
        private Integer waitingNumber;

        public static ParticipantDto from(EventParticipation participation) {
            if (participation == null) {
                return null;
            }
            return ParticipantDto.builder()
                    .id(participation.getId())
                    .member(MemberDto.from(participation.getParticipant()))
                    .status(participation.getStatus())
                    .registrationDate(participation.getRegistrationDate())
                    .waitingNumber(participation.getWaitingNumber())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "EventCreateRequest", description = "이벤트 생성 요청")
    public static class CreateRequest {
        @Schema(description = "이벤트 제목", example = "신년 독서 이벤트", requiredMode = Schema.RequiredMode.REQUIRED)
        private String title;

        @Schema(description = "이벤트 설명", example = "2025년 신년 맞이 독서 토론회", requiredMode = Schema.RequiredMode.REQUIRED)
        private String description;

        @Schema(description = "이벤트 유형", example = "MEETUP",
                allowableValues = {"STUDY_GROUP", "MEETUP", "CONFERENCE", "TECH_TALK", "WORKSHOP"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private EventType type;

        @Schema(description = "시작 시간 (ISO-8601 형식)", example = "2025-01-15T14:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime startTime;

        @Schema(description = "종료 시간 (ISO-8601 형식)", example = "2025-01-15T16:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime endTime;

        @Schema(description = "최대 참여자 수", example = "100", defaultValue = "50")
        private int maxParticipants = 50;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "EventUpdateRequest", description = "이벤트 수정 요청")
    public static class UpdateRequest {
        @Schema(description = "이벤트 제목", example = "신년 독서 이벤트 (수정됨)")
        private String title;

        @Schema(description = "이벤트 설명", example = "2025년 신년 맞이 독서 토론회 - 참여 인원 증가")
        private String description;

        @Schema(description = "시작 시간 (ISO-8601 형식)", example = "2025-01-15T15:00:00")
        private LocalDateTime startTime;

        @Schema(description = "종료 시간 (ISO-8601 형식)", example = "2025-01-15T17:00:00")
        private LocalDateTime endTime;
    }

    @Getter
    @Builder
    @Schema(name = "EventResponse", description = "이벤트 응답")
    public static class Response {
        @Schema(description = "이벤트 ID", example = "1")
        private Long id;

        @Schema(description = "이벤트 제목", example = "2025 신년 독서 토론회")
        private String title;

        @Schema(description = "이벤트 설명", example = "2025년을 맞이하여 올해의 독서 목표를 공유하고 추천 도서를 토론하는 시간입니다.")
        private String description;

        @Schema(description = "이벤트 유형", example = "MEETUP")
        private EventType type;

        @Schema(description = "시작 시간", example = "2025-01-20T14:00:00")
        private LocalDateTime startTime;

        @Schema(description = "종료 시간", example = "2025-01-20T16:00:00")
        private LocalDateTime endTime;

        @Schema(description = "최대 참여자 수", example = "50")
        private int maxParticipants;

        @Schema(description = "발표자/진행자 정보")
        private MemberDto presenter;

        @Schema(description = "참여자 목록")
        private List<ParticipantDto> participants;

        @Schema(description = "정원 마감 여부 - 확정 참여자가 최대 인원에 도달했는지 여부", example = "false")
        private boolean isFullyBooked;

        @Schema(description = "확정 참여자 수", example = "25")
        private int confirmedCount;

        @Schema(description = "참가 가능 여부 - 추가 참가가 가능한지 여부 (정원 마감 시 false)", example = "true")
        private boolean available;

        public static Response from(Event event) {
            long confirmedCount = event.getParticipants() != null ?
                    event.getParticipants().stream()
                            .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
                            .count() : 0;

            boolean isFullyBooked = confirmedCount >= event.getMaxParticipants();

            return Response.builder()
                    .id(event.getId())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .type(event.getType())
                    .startTime(event.getStartTime())
                    .endTime(event.getEndTime())
                    .maxParticipants(event.getMaxParticipants())
                    .presenter(MemberDto.from(event.getPresenter()))
                    .participants(event.getParticipants() != null ?
                            event.getParticipants().stream()
                                    .map(ParticipantDto::from)
                                    .collect(Collectors.toList()) :
                            List.of())
                    .isFullyBooked(isFullyBooked)
                    .confirmedCount((int) confirmedCount)
                    .available(!isFullyBooked)
                    .build();
        }
    }
}
