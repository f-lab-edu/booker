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
    @Schema(name = "BookRequest", description = "도서 생성/수정 요청 모델")
    public static class Request {
        @NotBlank(message = "제목은 필수입니다")
        @Schema(
            description = "도서 제목 - 도서관에 등록할 책의 정확한 제목을 입력합니다",
            example = "Clean Code",
            required = true
        )
        private String title;

        @NotBlank(message = "저자는 필수입니다")
        @Schema(
            description = "저자명 - 도서의 저자 이름을 입력합니다. 공동 저자의 경우 'Gang of Four'와 같이 표기 가능",
            example = "Robert C. Martin",
            required = true
        )
        private String author;

        @NotBlank(message = "출판사는 필수입니다")
        @Schema(
            description = "출판사명 - 도서를 출판한 출판사 이름",
            example = "Prentice Hall",
            required = true
        )
        private String publisher;

        @NotBlank(message = "ISBN은 필수입니다")
        @Schema(
            description = "ISBN - 국제 표준 도서 번호 (10자리 또는 13자리 숫자만 허용). 하이픈(-) 없이 숫자만 입력하세요",
            example = "9780132350884",
            pattern = "^\\d{10}|\\d{13}$",
            required = true
        )
        private String isbn;

        @Schema(
            description = "도서 표지 이미지 URL - 도서의 표지 이미지가 저장된 URL 주소 (선택사항)",
            example = "https://images.example.com/books/clean-code-cover.jpg"
        )
        private String coverImageUrl;

        @Schema(
            description = "도서 위치 정보 - 도서관 내 도서의 물리적 위치 (층, 구역, 서가). 미입력 시 기본값(4층-A구역-1서가)으로 설정됩니다"
        )
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
    @Schema(name = "LocationResponse", description = "도서 위치 응답 모델 - 필요한 필드만 포함")
    public static class LocationResponse {
        @Schema(description = "위치 고유 ID", example = "1")
        private Long id;

        @Schema(description = "층수 (4층 또는 12층)", example = "FOURTH")
        private String floor;

        @Schema(description = "구역 코드", example = "A")
        private String section;

        @Schema(description = "서가 번호", example = "1")
        private String shelf;

        public static LocationResponse from(BookLocation location) {
            if (location == null) {
                return null;
            }
            LocationResponse response = new LocationResponse();
            response.id = location.getId();
            response.floor = location.getFloor() != null ? location.getFloor().name() : null;
            response.section = location.getSection();
            response.shelf = location.getShelf();
            return response;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "LocationRequest", description = "도서 위치 정보 - 도서관 내 도서의 물리적 보관 위치")
    public static class LocationRequest {
        @Schema(
            description = "층수 - 도서가 보관된 층. 현재 4층(FOURTH)과 12층(TWELFTH)만 지원",
            example = "FOURTH",
            allowableValues = { "FOURTH", "TWELFTH" },
            defaultValue = "FOURTH"
        )
        private String floor = "FOURTH";

        @Schema(
            description = "구역 - 해당 층 내의 구역 코드 (알파벳 A-Z)",
            example = "A",
            defaultValue = "A"
        )
        private String section = "A";

        @Schema(
            description = "서가 - 해당 구역 내의 서가 번호",
            example = "1",
            defaultValue = "1"
        )
        private String shelf = "1";
    }

    @Getter
    @NoArgsConstructor
    @Schema(name = "BookResponse", description = "도서 응답 모델 - 조회/생성/수정 시 반환되는 도서 정보")
    public static class Response {
        @Schema(description = "도서 고유 ID", example = "1")
        private Long id;

        @Schema(description = "도서 제목", example = "Clean Code")
        private String title;

        @Schema(description = "저자명", example = "Robert C. Martin")
        private String author;

        @Schema(description = "ISBN (국제 표준 도서 번호)", example = "9780132350884")
        private String isbn;

        @Schema(description = "출판사명", example = "Prentice Hall")
        private String publisher;

        @Schema(description = "표지 이미지 URL", example = "https://images.example.com/books/clean-code-cover.jpg")
        private String coverImageUrl;

        @Schema(description = "도서 상태 - AVAILABLE(대출가능), LOANED(대출중), PROCESSING(처리중), RESERVED(예약됨), UNAVAILABLE(이용불가)", example = "AVAILABLE")
        private BookStatus status;

        @Schema(description = "도서 위치 정보 객체")
        private LocationResponse location;

        @Schema(description = "도서 위치 표시용 텍스트 (층 정보)", example = "4")
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
            response.location = LocationResponse.from(book.getLocation());
            if (book.getLocation() != null && book.getLocation().getFloor() != null) {
                response.locationDisplay = String.valueOf(book.getLocation().getFloor().getValue());
            }
            return response;
        }
    }

    @Getter
    @Setter
    @Schema(name = "BookSearchRequest", description = "도서 검색 및 필터링 요청 모델")
    public static class SearchRequest {
        @Schema(
            description = "도서 제목으로 검색 (부분 일치 검색 지원). 예: 'Clean'을 입력하면 'Clean Code', 'Clean Architecture' 등이 검색됨",
            example = "Clean",
            nullable = true
        )
        private String title;

        @Schema(
            description = "저자명으로 검색 (부분 일치 검색 지원). 예: 'Martin'을 입력하면 'Robert C. Martin' 등이 검색됨",
            example = "Martin",
            nullable = true
        )
        private String author;

        @Schema(
            description = "도서 상태로 필터링 - AVAILABLE(대출가능), LOANED(대출중), PROCESSING(처리중), RESERVED(예약됨), UNAVAILABLE(이용불가)",
            example = "AVAILABLE",
            allowableValues = { "AVAILABLE", "LOANED", "PROCESSING", "RESERVED", "UNAVAILABLE" },
            nullable = true
        )
        private BookStatus status;

        @Schema(
            description = "페이지 번호 (0부터 시작). 첫 페이지는 0, 두 번째 페이지는 1입니다",
            example = "0",
            defaultValue = "0"
        )
        private int page = 0;

        @Schema(
            description = "한 페이지당 표시할 도서 수 (최대 100권)",
            example = "20",
            defaultValue = "20"
        )
        private int size = 20;

        @Schema(
            description = "정렬 기준 - '필드명,방향' 형식. 방향은 asc(오름차순) 또는 desc(내림차순). 예: 'title,asc' 또는 'author,desc'",
            example = "title,asc",
            defaultValue = "title,asc"
        )
        private String sort = "title,asc";
    }
}
