package com.bookerapp.core.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_orders")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE book_orders SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class BookOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 200)
    private String author;

    @Column(length = 200)
    private String publisher;

    @Column(length = 20)
    private String isbn;

    @Column(nullable = false, length = 100)
    private String requesterId;

    @Column(nullable = false, length = 100)
    private String requesterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookOrderStatus status = BookOrderStatus.PENDING;

    @Column(length = 1000)
    private String adminComments;

    @Column
    private LocalDateTime approvedAt;

    @Column(length = 100)
    private String approvedBy;

    @Column
    private LocalDateTime receivedAt;

    @Column(length = 100)
    private String receivedBy;

    public enum BookOrderStatus {
        PENDING,
        APPROVED,
        REJECTED,
        RECEIVED
    }

    public BookOrder(String title, String author, String publisher, String isbn,
                     String requesterId, String requesterName) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.isbn = isbn;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.status = BookOrderStatus.PENDING;
        setCreatedBy(requesterId);
        setUpdatedBy(requesterId);
    }

    public void approve(String adminId, String comments) {
        this.status = BookOrderStatus.APPROVED;
        this.approvedBy = adminId;
        this.approvedAt = LocalDateTime.now();
        this.adminComments = comments;
    }

    public void reject(String adminId, String comments) {
        this.status = BookOrderStatus.REJECTED;
        this.approvedBy = adminId;
        this.approvedAt = LocalDateTime.now();
        this.adminComments = comments;
    }

    public void markAsReceived(String adminId) {
        this.status = BookOrderStatus.RECEIVED;
        this.receivedBy = adminId;
        this.receivedAt = LocalDateTime.now();
    }
}
