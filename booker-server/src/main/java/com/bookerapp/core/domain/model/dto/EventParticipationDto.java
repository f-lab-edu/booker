package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.event.EventParticipation;
import com.bookerapp.core.domain.model.event.ParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event Participation DTO
 * 이벤트 참여 신청 요청/응답 데이터 전송 객체
 */
public class EventParticipationDto {

    /**
     * 이벤트 참여 신청 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "EventParticipationRequest", description = "이벤트 참여 신청 요청")
    public static class Request {

        @NotNull(message = "회원 ID는 필수입니다")
        @Schema(
                description = "참여할 회원 ID - 실제 DB에 존재하는 회원 ID",
                example = "test-member-001",
                required = true
        )
        private String memberId;

        public Request(String memberId) {
            this.memberId = memberId;
        }
    }

    /**
     * 이벤트 참여 신청 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "EventParticipationResponse", description = "이벤트 참여 신청 응답")
    public static class Response {

        @Schema(description = "참여 ID", example = "1")
        private Long id;

        @Schema(description = "이벤트 ID", example = "1")
        private Long eventId;

        @Schema(description = "이벤트 제목", example = "Spring Boot 실전 가이드")
        private String eventTitle;

        @Schema(description = "회원 ID", example = "test-member-001")
        private String memberId;

        @Schema(description = "회원 이름", example = "홍길동")
        private String memberName;

        @Schema(
                description = "참여 상태 - CONFIRMED(확정), WAITING(대기), CANCELLED(취소)",
                example = "CONFIRMED"
        )
        private ParticipationStatus status;

        @Schema(description = "신청일시", example = "2025-12-18T10:30:00")
        private LocalDateTime registrationDate;

        @Schema(
                description = "대기 번호 - WAITING 상태인 경우만 존재",
                example = "3"
        )
        private Integer waitingNumber;

        @Schema(
                description = "사용된 전략 - SYNCHRONIZED 또는 CAS",
                example = "SYNCHRONIZED"
        )
        private String strategy;

        /**
         * Entity를 Response DTO로 변환
         *
         * @param participation EventParticipation 엔티티
         * @param strategy 사용된 동시성 제어 전략
         * @return Response DTO
         */
        public static Response from(EventParticipation participation, String strategy) {
            Response response = new Response();
            response.id = participation.getId();
            response.eventId = participation.getEvent().getId();
            response.eventTitle = participation.getEvent().getTitle();
            response.memberId = participation.getParticipant().getMemberId();
            response.memberName = participation.getParticipant().getName();
            response.status = participation.getStatus();
            response.registrationDate = participation.getRegistrationDate();
            response.waitingNumber = participation.getWaitingNumber();
            response.strategy = strategy;
            return response;
        }
    }
}
