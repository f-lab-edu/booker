package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLocation;
import com.bookerapp.core.domain.model.entity.BookStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class BookDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank(message = "제목은 필수입니다")
        private String title;

        @NotBlank(message = "저자는 필수입니다")
        private String author;

        @Pattern(regexp = "^[0-9-]{10,13}$", message = "올바른 ISBN 형식이 아닙니다")
        private String isbn;

        private String publisher;
        private String coverImageUrl;
        private BookLocation location;

        public Book toEntity() {
            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setIsbn(isbn);
            book.setPublisher(publisher);
            book.setCoverImageUrl(coverImageUrl);
            book.setLocation(location);
            book.setStatus(BookStatus.AVAILABLE);
            return book;
        }
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String author;
        private String isbn;
        private String publisher;
        private String coverImageUrl;
        private BookStatus status;
        private BookLocation location;
        private String locationDisplay;

        public static Response from(Book book) {
            return Response.builder()
                    .id(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .isbn(book.getIsbn())
                    .publisher(book.getPublisher())
                    .coverImageUrl(book.getCoverImageUrl())
                    .status(book.getStatus())
                    .location(book.getLocation())
                    .locationDisplay(book.getLocation() != null ? book.getLocation().toString() : null)
                    .build();
        }
    }

    @Getter
    @Setter
    public static class SearchRequest {
        private String title;
        private String author;
        private BookStatus status;
        private int page = 0;
        private int size = 10;
    }
} 