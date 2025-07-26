package com.bookerapp.core.presentation.dto.event;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.EventType;
import com.bookerapp.core.domain.model.event.Member;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public class EventDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String title;
        private String description;
        private EventType type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int maxParticipants;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private EventType type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int maxParticipants;
        private Member presenter;
        private List<ParticipantResponse> participants;
        private boolean isFullyBooked;

        public static Response from(Event event) {
            return Response.builder()
                    .id(event.getId())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .type(event.getType())
                    .startTime(event.getStartTime())
                    .endTime(event.getEndTime())
                    .maxParticipants(event.getMaxParticipants())
                    .presenter(event.getPresenter())
                    .participants(event.getParticipants().stream()
                            .map(ParticipantResponse::from)
                            .toList())
                    .isFullyBooked(event.isFullyBooked())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ParticipantResponse {
        private Long id;
        private Long memberId;
        private String memberName;
        private ParticipationStatus status;
        private LocalDateTime registrationDate;
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