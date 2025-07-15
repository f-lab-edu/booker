package com.bookerapp.core.application.dto;

import com.bookerapp.core.domain.model.entity.BookOrder;
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
    public static class Action {
        @Size(max = 1000, message = "관리자 코멘트는 1000자를 초과할 수 없습니다")
        private String comments;

        public Action(String comments) {
            this.comments = comments;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @NotBlank(message = "도서명은 필수입니다")
        @Size(max = 30, message = "도서명은 30자를 초과할 수 없습니다")
        private String title;

        @Size(max = 30, message = "저자명은 30자를 초과할 수 없습니다")
        private String author;

        @Size(max = 30, message = "출판사명은 30자를 초과할 수 없습니다")
        private String publisher;

        @Size(max = 20, message = "ISBN은 20자를 초과할 수 없습니다")
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
    public static class Response {
        private Long id;
        private String title;
        private String author;
        private String publisher;
        private String isbn;
        private String requesterId;
        private String requesterName;
        private BookOrder.BookOrderStatus status;
        private String adminComments;
        private LocalDateTime approvedAt;
        private String approvedBy;
        private LocalDateTime receivedAt;
        private String receivedBy;
        private LocalDateTime createdAt;
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
