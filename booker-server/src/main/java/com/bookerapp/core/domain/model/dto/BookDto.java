package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLocation;
import com.bookerapp.core.domain.model.enums.BookStatus;
import com.bookerapp.core.domain.model.enums.Floor;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

public class BookDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "BookRequest")
    public static class Request {
        @NotBlank(message = "제목은 필수입니다")
        @Schema(description = "도서 제목", example = "스프링 부트 입문", required = true)
        private String title;

        @NotBlank(message = "저자는 필수입니다")
        @Schema(description = "저자명", example = "김영한", required = true)
        private String author;

        @NotBlank(message = "출판사는 필수입니다")
        @Schema(description = "출판사명", example = "한빛미디어", required = true)
        private String publisher;

        @NotBlank(message = "ISBN은 필수입니다")
        @Schema(description = "ISBN (10자리 또는 13자리 숫자)",
                example = "9791158392826",
                pattern = "^\\d{10}|\\d{13}$",
                required = true)
        private String isbn;

        @Schema(description = "도서 표지 이미지 URL", example = "https://example.com/book-cover.jpg")
        private String coverImageUrl;

        @Schema(description = "도서 위치 정보")
        private LocationRequest location;

        public Book toEntity() {
            try {
                BookLocation bookLocation = null;
                if (location != null) {
                    bookLocation = BookLocation.of(Floor.valueOf(location.getFloor()));
                    bookLocation.setSection(location.getSection());
                    bookLocation.setShelf(location.getShelf());
                }

                return Book.builder()
                        .title(title)
                        .author(author)
                        .publisher(publisher)
                        .isbn(isbn)
                        .coverImageUrl(coverImageUrl)
                        .location(bookLocation)
                        .build();
            } catch (Exception e) {
                System.err.println("=== BookDto.toEntity 에러 발생 ===");
                System.err.println("Exception type: " + e.getClass().getName());
                System.err.println("Exception message: " + e.getMessage());
                System.err.println("Location: " + location);
                if (location != null) {
                    System.err.println("Floor: " + location.getFloor());
                    System.err.println("Section: " + location.getSection());
                    System.err.println("Shelf: " + location.getShelf());
                }
                e.printStackTrace();
                throw new RuntimeException("도서 엔티티 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "LocationRequest")
    public static class LocationRequest {
        @Schema(description = "층수", example = "FOURTH", allowableValues = {"FOURTH", "TWELFTH"})
        private String floor;

        @Schema(description = "구역", example = "A")
        private String section = "A";

        @Schema(description = "서가", example = "1")
        private String shelf = "1";
    }

    @Getter
    @NoArgsConstructor
    @Schema(name = "BookResponse")
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
            Response response = new Response();
            response.id = book.getId();
            response.title = book.getTitle();
            response.author = book.getAuthor();
            response.isbn = book.getIsbn();
            response.publisher = book.getPublisher();
            response.coverImageUrl = book.getCoverImageUrl();
            response.status = book.getStatus();
            response.location = book.getLocation();
            if (book.getLocation() != null && book.getLocation().getFloor() != null) {
                response.locationDisplay = String.valueOf(book.getLocation().getFloor().getValue());
            }
            return response;
        }
    }

    @Getter
    @Setter
    @Schema(name = "BookSearchRequest")
    public static class SearchRequest {
        private String title;
        private String author;
        private BookStatus status;
        private int page = 0;
        private int size = 20;
    }
}
