package com.bookerapp.core.application.dto;

import com.bookerapp.core.domain.model.entity.BookOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class BookOrderResponseDto {

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

    public BookOrderResponseDto(BookOrder bookOrder) {
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
