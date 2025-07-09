package com.bookerapp.core.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "books")
@Getter
@Setter
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

    public boolean isAvailableForLoan() {
        return status == BookStatus.AVAILABLE;
    }

    public void updateStatus(BookStatus newStatus) {
        this.status = newStatus;
    }

    public void updateLocation(BookLocation newLocation) {
        this.location = newLocation;
    }
}
