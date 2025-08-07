package com.bookerapp.core.domain.model.entity;

import com.bookerapp.core.domain.exception.InvalidBookException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(unique = true)
    private String isbn;

    @Column
    private String publisher;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status = BookStatus.AVAILABLE;

    @ManyToOne
    private BookLocation location;

    @Builder(builderClassName = "BookBuilder")
    private Book(String title, String author, String isbn, String publisher,
            String coverImageUrl, BookLocation location) {
        validateTitle(title);
        validateAuthor(author);
        validateIsbn(isbn);

        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.coverImageUrl = coverImageUrl;
        this.location = location;
    }

    private static void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidBookException("제목은 필수입니다.");
        }
    }

    private static void validateAuthor(String author) {
        if (author == null || author.trim().isEmpty()) {
            throw new InvalidBookException("저자는 필수입니다.");
        }
    }

    private static void validateIsbn(String isbn) {
        if (isbn != null && !isbn.matches("^\\d{10}|\\d{13}$")) {
            throw new InvalidBookException("ISBN은 10자리 또는 13자리 숫자여야 합니다.");
        }
    }

    public boolean isAvailableForLoan() {
        return status == BookStatus.AVAILABLE;
    }

    public void updateStatus(BookStatus newStatus) {
        this.status = newStatus;
    }

    public void updateLocation(BookLocation newLocation) {
        this.location = newLocation;
    }

    public void updateInformation(String title, String author, String isbn,
            String publisher, String coverImageUrl, BookLocation location) {
        validateTitle(title);
        validateAuthor(author);
        validateIsbn(isbn);

        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.coverImageUrl = coverImageUrl;
        this.location = location;
    }
}
