package com.bookerapp.core.application.dto;

import com.bookerapp.core.domain.model.entity.BookOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class BookOrderDto {
    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "BookOrderAction", description = "도서 주문 요청 처리 (승인/거부) 요청 모델")
    public static class Action {
        @Size(max = 1000, message = "관리자 코멘트는 1000자를 초과할 수 없습니다")
        @Schema(
            description = "관리자 코멘트 - 승인 또는 거부 사유를 입력합니다. 거부 시 필수 입력을 권장합니다",
            example = "해당 도서는 저작권 문제로 구입이 어렵습니다",
            maxLength = 1000
        )
        private String comments;

        public Action(String comments) {
            this.comments = comments;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "BookOrderRequest", description = "도서 주문 요청 생성 모델")
    public static class Request {
        @NotBlank(message = "도서명은 필수입니다")
        @Size(max = 30, message = "도서명은 30자를 초과할 수 없습니다")
        @Schema(
            description = "도서명 - 주문을 요청할 도서의 제목을 입력합니다",
            example = "Effective Java",
            required = true,
            maxLength = 30
        )
        private String title;

        @Size(max = 30, message = "저자명은 30자를 초과할 수 없습니다")
        @Schema(
            description = "저자명 - 도서의 저자 이름을 입력합니다",
            example = "Joshua Bloch",
            maxLength = 30
        )
        private String author;

        @Size(max = 30, message = "출판사명은 30자를 초과할 수 없습니다")
        @Schema(
            description = "출판사명 - 도서의 출판사 이름을 입력합니다",
            example = "Addison-Wesley",
            maxLength = 30
        )
        private String publisher;

        @Size(max = 20, message = "ISBN은 20자를 초과할 수 없습니다")
        @Schema(
            description = "ISBN - 국제 표준 도서 번호 (10자리 또는 13자리). 하이픈 없이 숫자만 입력하세요",
            example = "9780134685991",
            maxLength = 20
        )
        private String isbn;

        public Request(String title, String author, String publisher, String isbn) {
            this.title = title;
            this.author = author;
            this.publisher = publisher;
            this.isbn = isbn;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "BookOrderResponse", description = "도서 주문 요청 응답 모델")
    public static class Response {
        @Schema(description = "주문 요청 고유 ID", example = "1")
        private Long id;

        @Schema(description = "도서명", example = "Effective Java")
        private String title;

        @Schema(description = "저자명", example = "Joshua Bloch")
        private String author;

        @Schema(description = "출판사명", example = "Addison-Wesley")
        private String publisher;

        @Schema(description = "ISBN - 국제 표준 도서 번호", example = "9780134685991")
        private String isbn;

        @Schema(description = "요청자 ID - 주문을 요청한 사용자 식별자", example = "user123")
        private String requesterId;

        @Schema(description = "요청자 이름", example = "홍길동")
        private String requesterName;

        @Schema(
            description = "주문 상태 - PENDING(검토대기), APPROVED(승인됨), REJECTED(거부됨), RECEIVED(입고완료)",
            example = "PENDING",
            allowableValues = { "PENDING", "APPROVED", "REJECTED", "RECEIVED" }
        )
        private BookOrder.BookOrderStatus status;

        @Schema(description = "관리자 코멘트 - 승인/거부 시 관리자가 작성한 메모", example = "예산 부족으로 다음 분기에 구매 예정")
        private String adminComments;

        @Schema(description = "승인 일시 - 관리자가 주문을 승인한 시각", example = "2024-01-15T14:30:00")
        private LocalDateTime approvedAt;

        @Schema(description = "승인 처리자 - 승인을 처리한 관리자 ID", example = "admin")
        private String approvedBy;

        @Schema(description = "입고 일시 - 도서가 실제로 입고된 시각", example = "2024-01-20T10:15:00")
        private LocalDateTime receivedAt;

        @Schema(description = "입고 처리자 - 입고를 처리한 관리자 ID", example = "admin")
        private String receivedBy;

        @Schema(description = "생성 일시 - 주문 요청이 생성된 시각", example = "2024-01-10T09:00:00")
        private LocalDateTime createdAt;

        @Schema(description = "수정 일시 - 주문 정보가 마지막으로 수정된 시각", example = "2024-01-15T14:30:00")
        private LocalDateTime updatedAt;

        public Response(BookOrder bookOrder) {
            this.id = bookOrder.getId();
            this.title = bookOrder.getTitle();
            this.author = bookOrder.getAuthor();
            this.publisher = bookOrder.getPublisher();
            this.isbn = bookOrder.getIsbn();
            this.requesterId = bookOrder.getRequesterId();
            this.requesterName = bookOrder.getRequesterName();
            this.status = bookOrder.getStatus();
            this.adminComments = bookOrder.getAdminComments();
            this.approvedAt = bookOrder.getApprovedAt();
            this.approvedBy = bookOrder.getApprovedBy();
            this.receivedAt = bookOrder.getReceivedAt();
            this.receivedBy = bookOrder.getReceivedBy();
            this.createdAt = bookOrder.getCreatedAt();
            this.updatedAt = bookOrder.getUpdatedAt();
        }
    }
}
