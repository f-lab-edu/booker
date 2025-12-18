package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.BookDto;
import com.bookerapp.core.domain.model.dto.PageResponse;
import com.bookerapp.core.domain.model.enums.BookStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.bookerapp.core.domain.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "1. Book", description = "도서 관리 API")
public class BookController {

        private final BookService bookService;

        @PostMapping
        @Operation(summary = "도서 생성", description = """
                        ## 개요
                        새로운 도서를 시스템에 등록합니다.
                        도서관 장서 관리를 위한 기본 정보를 입력받아 등록합니다.

                        ## 주요 파라미터
                        - `title`: 도서 제목 (필수)
                        - `author`: 저자명 (필수)
                        - `publisher`: 출판사명 (필수)
                        - `isbn`: ISBN 10자리 또는 13자리 숫자 (필수)
                        - `coverImageUrl`: 표지 이미지 URL (선택)
                        - `location`: 도서 위치 정보 - 층/구역/서가 (선택)

                        ## 응답 데이터
                        생성된 도서의 전체 정보와 고유 ID를 반환합니다.
                        Location 헤더에 생성된 리소스 URL이 포함됩니다.

                        ## 제약사항
                        - ISBN은 10자리 또는 13자리 숫자만 허용
                        - 중복 ISBN 등록 시 오류 발생 가능
                        - 위치 정보의 floor는 FOURTH 또는 TWELFTH만 허용
                        """)
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "도서 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.Response.class), examples = @ExampleObject(name = "생성된 도서 예시", value = """
                                {
                                  "id": 1,
                                  "title": "Clean Code",
                                  "author": "Robert C. Martin",
                                  "isbn": "9780132350884",
                                  "publisher": "Prentice Hall",
                                  "coverImageUrl": "https://images.example.com/books/clean-code-cover.jpg",
                                  "status": "AVAILABLE",
                                  "location": {
                                    "id": 1,
                                    "floor": "FOURTH",
                                    "section": "A",
                                    "shelf": "1"
                                  },
                                  "locationDisplay": "4"
                                }
                                """))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락 또는 유효성 검증 실패)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Validation failed\", \"details\": [\"title: must not be blank\"]}"))),
                        @ApiResponse(responseCode = "409", description = "중복된 ISBN"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<BookDto.Response> createBook(
                        @RequestBody(description = "도서 생성 요청 데이터", required = true, content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = BookDto.Request.class),
                                examples = {
                                        @ExampleObject(
                                                name = "Clean Code 예시",
                                                summary = "클린 코드 도서 등록",
                                                description = "로버트 C. 마틴의 Clean Code 도서를 4층 A구역 1서가에 등록하는 예시입니다",
                                                value = """
                                                {
                                                  "title": "Clean Code",
                                                  "author": "Robert C. Martin",
                                                  "isbn": "9780132350884",
                                                  "publisher": "Prentice Hall",
                                                  "coverImageUrl": "https://images.example.com/books/clean-code-cover.jpg",
                                                  "location": {
                                                    "floor": "FOURTH",
                                                    "section": "A",
                                                    "shelf": "1"
                                                  }
                                                }
                                                """
                                        ),
                                        @ExampleObject(
                                                name = "Effective Java 예시",
                                                summary = "이펙티브 자바 도서 등록",
                                                description = "조슈아 블로크의 Effective Java 도서를 12층 B구역 3서가에 등록하는 예시입니다",
                                                value = """
                                                {
                                                  "title": "Effective Java",
                                                  "author": "Joshua Bloch",
                                                  "isbn": "9780134685991",
                                                  "publisher": "Addison-Wesley",
                                                  "coverImageUrl": "https://images.example.com/books/effective-java-cover.jpg",
                                                  "location": {
                                                    "floor": "TWELFTH",
                                                    "section": "B",
                                                    "shelf": "3"
                                                  }
                                                }
                                                """
                                        ),
                                        @ExampleObject(
                                                name = "위치 정보 없이 등록",
                                                summary = "기본 위치로 도서 등록",
                                                description = "위치 정보를 생략하면 기본값(4층-A구역-1서가)으로 설정됩니다",
                                                value = """
                                                {
                                                  "title": "Design Patterns",
                                                  "author": "Gang of Four",
                                                  "isbn": "9780201633610",
                                                  "publisher": "Addison-Wesley"
                                                }
                                                """
                                        )
                                }
                        ))
                        @Valid @org.springframework.web.bind.annotation.RequestBody BookDto.Request request) {
                try {
                        BookDto.Response response = bookService.createBook(request, null);
                        URI location = ServletUriComponentsBuilder
                                        .fromCurrentRequest()
                                        .path("/{id}")
                                        .buildAndExpand(response.getId())
                                        .toUri();
                        return ResponseEntity.created(location).body(response);
                } catch (Exception e) {
                        System.err.println("=== BookController.createBook 에러 발생 ===");
                        System.err.println("Exception type: " + e.getClass().getName());
                        System.err.println("Exception message: " + e.getMessage());
                        e.printStackTrace();
                        throw e;
                }
        }

        @GetMapping("/{id}")
        @Operation(summary = "도서 조회", description = """
                        ## 개요
                        특정 도서의 상세 정보를 조회합니다.
                        도서 ID를 통해 제목, 저자, 출판사, ISBN, 위치, 상태 등 모든 정보를 확인할 수 있습니다.

                        ## 주요 파라미터
                        - `id`: 도서 ID (Long 타입, Path Parameter)

                        ## 응답 데이터
                        도서의 모든 정보를 포함한 상세 데이터를 반환합니다.
                        - 기본 정보: 제목, 저자, 출판사, ISBN
                        - 상태 정보: 대출 가능 여부 (AVAILABLE, LOANED 등)
                        - 위치 정보: 층, 구역, 서가

                        ## 제약사항
                        - 존재하지 않는 ID 조회 시 404 오류 발생
                        """)
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.Response.class), examples = @ExampleObject(name = "조회된 도서 예시", value = """
                                {
                                  "id": 1,
                                  "title": "Clean Code",
                                  "author": "Robert C. Martin",
                                  "isbn": "9780132350884",
                                  "publisher": "Prentice Hall",
                                  "coverImageUrl": "https://images.example.com/books/clean-code-cover.jpg",
                                  "status": "AVAILABLE",
                                  "location": {
                                    "id": 1,
                                    "floor": "FOURTH",
                                    "section": "A",
                                    "shelf": "1"
                                  },
                                  "locationDisplay": "4"
                                }
                                """))),
                        @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Book not found\", \"id\": 999}"))),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<BookDto.Response> getBook(
                        @Parameter(description = "조회할 도서의 고유 ID - 데이터베이스에 실제 존재하는 ID를 입력하세요", example = "1", required = true) @PathVariable Long id) {
                return ResponseEntity.ok(bookService.getBook(id));
        }

        @GetMapping
        @Operation(summary = "도서 검색 및 전체 조회 (페이징)", description = """
                        ## 개요
                        도서 목록을 조회하거나 검색하고 페이지네이션된 결과를 반환합니다.
                        검색 조건 없이 호출하면 전체 도서 목록을 페이징하여 반환합니다.

                        ## 사용 예시
                        - **전체 도서 조회**: `/api/v1/books?page=0&size=20`
                        - **제목 검색**: `/api/v1/books?title=클린&page=0&size=10`
                        - **저자 검색**: `/api/v1/books?author=마틴&page=0&size=10`
                        - **상태 필터링**: `/api/v1/books?status=AVAILABLE&page=0&size=20`

                        ## 주요 파라미터
                        - `title`: 제목으로 검색 (부분 일치, 선택)
                        - `author`: 저자명으로 검색 (부분 일치, 선택)
                        - `status`: 도서 상태로 필터링 (AVAILABLE, LOANED 등, 선택)
                        - `page`: 페이지 번호, 0부터 시작 (기본값: 0)
                        - `size`: 페이지 크기 (기본값: 20, 최대: 100)
                        - `sort`: 정렬 기준 (예: title,asc 또는 author,desc)

                        ## 응답 데이터
                        페이지네이션된 도서 목록과 메타데이터를 반환합니다.
                        - `content`: 도서 목록 배열
                        - `totalElements`: 전체 도서 수
                        - `totalPages`: 전체 페이지 수
                        - `page`: 현재 페이지 번호
                        - `size`: 페이지 크기
                        - `first`: 첫 페이지 여부
                        - `last`: 마지막 페이지 여부

                        ## 제약사항
                        - **검색 조건 미지정 시 전체 도서 반환** (페이징 적용)
                        - 최대 페이지 크기: 100
                        - 정렬 기준은 도서 필드명과 방향(asc/desc)을 쉼표로 구분
                        """)
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "검색 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class), examples = @ExampleObject(name = "검색 결과 예시", value = """
                                {
                                  "content": [
                                    {
                                      "id": 1,
                                      "title": "Clean Code",
                                      "author": "Robert C. Martin",
                                      "isbn": "9780132350884",
                                      "publisher": "Prentice Hall",
                                      "coverImageUrl": "https://images.example.com/books/clean-code-cover.jpg",
                                      "status": "AVAILABLE",
                                      "location": {
                                        "id": 1,
                                        "floor": "FOURTH",
                                        "section": "A",
                                        "shelf": "1"
                                      },
                                      "locationDisplay": "4"
                                    },
                                    {
                                      "id": 2,
                                      "title": "Clean Architecture",
                                      "author": "Robert C. Martin",
                                      "isbn": "9780134494166",
                                      "publisher": "Prentice Hall",
                                      "coverImageUrl": "https://images.example.com/books/clean-architecture-cover.jpg",
                                      "status": "AVAILABLE",
                                      "location": {
                                        "id": 2,
                                        "floor": "FOURTH",
                                        "section": "A",
                                        "shelf": "2"
                                      },
                                      "locationDisplay": "4"
                                    }
                                  ],
                                  "page": 0,
                                  "size": 20,
                                  "totalElements": 2,
                                  "totalPages": 1,
                                  "first": true,
                                  "last": true
                                }
                                """))),
                        @ApiResponse(responseCode = "400", description = "잘못된 검색 파라미터", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Invalid parameter\", \"message\": \"Page size must not exceed 100\"}"))),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<PageResponse<BookDto.Response>> searchBooks(
                        @Parameter(description = "도서 제목으로 검색 (부분 일치)", example = "Clean") @RequestParam(required = false) String title,
                        @Parameter(description = "저자명으로 검색 (부분 일치)", example = "Martin") @RequestParam(required = false) String author,
                        @Parameter(description = "도서 상태로 필터링 (AVAILABLE, LOANED 등)", example = "AVAILABLE") @RequestParam(required = false) BookStatus status,
                        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "페이지 크기 (기본값: 20, 최대: 100)", example = "20") @RequestParam(defaultValue = "20") int size,
                        @Parameter(description = "정렬 기준 (예: title,asc 또는 author,desc)", example = "title,asc") @RequestParam(defaultValue = "title,asc") String sort) {
                BookDto.SearchRequest request = new BookDto.SearchRequest();
                request.setTitle(title);
                request.setAuthor(author);
                request.setStatus(status);
                request.setPage(page);
                request.setSize(size);
                request.setSort(sort);
                return ResponseEntity.ok(PageResponse.of(bookService.searchBooks(request)));
        }

        @PutMapping("/{id}")
        @Operation(summary = "도서 수정", description = """
                        ## 개요
                        기존 도서 정보를 수정합니다.
                        도서의 기본 정보(제목, 저자, 출판사 등)를 업데이트할 수 있습니다.

                        ## 주요 파라미터
                        - `id`: 수정할 도서 ID (Path Parameter)
                        - Request Body: 수정할 도서 정보 (전체 필드 필수)
                          - `title`: 도서 제목
                          - `author`: 저자명
                          - `publisher`: 출판사명
                          - `isbn`: ISBN
                          - `coverImageUrl`: 표지 이미지 URL (선택)
                          - `location`: 도서 위치 정보 (선택)

                        ## 응답 데이터
                        수정된 도서의 전체 정보를 반환합니다.

                        ## 제약사항
                        - 존재하지 않는 ID 수정 시 404 오류 발생
                        - 모든 필수 필드 입력 필요 (부분 수정 불가)
                        - ISBN 형식 검증 필요 (10자리 또는 13자리)
                        """)
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.Response.class), examples = @ExampleObject(name = "수정된 도서 예시", value = """
                                {
                                  "id": 1,
                                  "title": "Clean Code (2nd Edition)",
                                  "author": "Robert C. Martin",
                                  "isbn": "9780132350884",
                                  "publisher": "Prentice Hall",
                                  "coverImageUrl": "https://images.example.com/books/clean-code-2nd-cover.jpg",
                                  "status": "AVAILABLE",
                                  "location": {
                                    "id": 1,
                                    "floor": "TWELFTH",
                                    "section": "B",
                                    "shelf": "5"
                                  },
                                  "locationDisplay": "12"
                                }
                                """))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락 또는 유효성 검증 실패)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Validation failed\", \"details\": [\"isbn: must match pattern\"]}"))),
                        @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<BookDto.Response> updateBook(
                        @Parameter(description = "수정할 도서의 고유 ID - 실제 존재하는 도서 ID를 입력하세요", example = "1", required = true) @PathVariable Long id,
                        @RequestBody(description = "도서 수정 요청 데이터 (전체 필드 입력 필요)", required = true, content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = BookDto.Request.class),
                                examples = {
                                        @ExampleObject(
                                                name = "제목 및 위치 수정",
                                                summary = "도서 제목과 보관 위치 변경",
                                                description = "도서 제목을 'Clean Code (2nd Edition)'으로 변경하고 12층 B구역 5서가로 이동하는 예시입니다",
                                                value = """
                                                {
                                                  "title": "Clean Code (2nd Edition)",
                                                  "author": "Robert C. Martin",
                                                  "isbn": "9780132350884",
                                                  "publisher": "Prentice Hall",
                                                  "coverImageUrl": "https://images.example.com/books/clean-code-2nd-cover.jpg",
                                                  "location": {
                                                    "floor": "TWELFTH",
                                                    "section": "B",
                                                    "shelf": "5"
                                                  }
                                                }
                                                """
                                        ),
                                        @ExampleObject(
                                                name = "표지 이미지 업데이트",
                                                summary = "도서 표지 이미지 URL 변경",
                                                description = "표지 이미지 URL만 변경하는 경우에도 모든 필수 필드를 입력해야 합니다",
                                                value = """
                                                {
                                                  "title": "Effective Java",
                                                  "author": "Joshua Bloch",
                                                  "isbn": "9780134685991",
                                                  "publisher": "Addison-Wesley",
                                                  "coverImageUrl": "https://images.example.com/books/effective-java-new-cover.jpg",
                                                  "location": {
                                                    "floor": "FOURTH",
                                                    "section": "A",
                                                    "shelf": "1"
                                                  }
                                                }
                                                """
                                        )
                                }
                        ))
                        @Valid @org.springframework.web.bind.annotation.RequestBody BookDto.Request request) {
                return ResponseEntity.ok(bookService.updateBook(id, request));
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "도서 삭제", description = """
                        ## 개요
                        도서를 시스템에서 삭제합니다.
                        물리적 삭제가 아닌 논리적 삭제(soft delete)로 처리될 수 있습니다.

                        ## 주요 파라미터
                        - `id`: 삭제할 도서 ID (Path Parameter)

                        ## 응답 데이터
                        응답 본문 없음 (204 No Content)
                        성공 시 빈 응답과 함께 204 상태 코드를 반환합니다.

                        ## 제약사항
                        - 존재하지 않는 ID 삭제 시 404 오류 발생
                        - 대출 중인 도서는 삭제 불가능할 수 있음 (409 Conflict)
                        - 삭제된 도서는 복구가 어려울 수 있으므로 주의 필요
                        """)
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "삭제 성공 (응답 본문 없음)"),
                        @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Book not found\", \"id\": 999}"))),
                        @ApiResponse(responseCode = "409", description = "삭제 불가능한 상태 (예: 대출 중)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Cannot delete\", \"reason\": \"Book is currently loaned\"}"))),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<Void> deleteBook(
                        @Parameter(description = "삭제할 도서의 고유 ID - 실제 존재하며 대출 중이지 않은 도서 ID를 입력하세요", example = "1", required = true) @PathVariable Long id) {
                bookService.deleteBook(id);
                return ResponseEntity.noContent().build();
        }
}
