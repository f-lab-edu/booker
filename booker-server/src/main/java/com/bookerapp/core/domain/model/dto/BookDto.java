package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLocation;
import com.bookerapp.core.domain.model.enums.BookStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BookDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        @NotBlank(message = "제목은 필수입니다")
        private String title;

        @NotBlank(message = "저자는 필수입니다")
        private String author;

        @NotBlank(message = "출판사는 필수입니다")
        private String publisher;

        @NotBlank(message = "ISBN은 필수입니다")
        private String isbn;

        private String coverImageUrl;
        private BookLocation location;

        public Book toEntity() {
            return Book.builder()
                    .title(title)
                    .author(author)
                    .publisher(publisher)
                    .isbn(isbn)
                    .coverImageUrl(coverImageUrl)
                    .location(location)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String author;
        private String publisher;
        private String isbn;
        private String coverImageUrl;
        private BookStatus status;
        private BookLocation location;
        private String locationDisplay;

        public static Response from(Book book) {
            Response response = new Response();
            response.id = book.getId();
            response.title = book.getTitle();
            response.author = book.getAuthor();
            response.publisher = book.getPublisher();
            response.isbn = book.getIsbn();
            response.coverImageUrl = book.getCoverImageUrl();
            response.status = book.getStatus();
            response.location = book.getLocation();
            response.locationDisplay = book.getLocation().toString();
            return response;
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
