package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.BookLoanDto;
import com.bookerapp.core.domain.model.dto.PageResponse;
import com.bookerapp.core.domain.service.BookLoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "2. Book Loan", description = "도서 대출 관련 API")
public class BookLoanController {

    private final BookLoanService bookLoanService;

    @PostMapping
    @Operation(summary = "도서 대출 신청", description = """
            ## 개요
            도서 대출을 신청합니다. 도서가 이미 대출 중인 경우 WAITING(대기) 상태로 생성되고,
            대출 가능한 경우 즉시 ACTIVE(대출중) 상태로 변경됩니다.

            ## 주요 파라미터
            - `bookId`: 대출할 도서의 고유 ID (필수)

            ## 응답 데이터
            - `id`: 대출 신청 ID
            - `status`: ACTIVE (즉시 대출) 또는 WAITING (대기 목록)
            - `loanDate`: 대출 개시일 (ACTIVE인 경우에만 존재)
            - `dueDate`: 반납 예정일 (ACTIVE인 경우, 대출일로부터 2주 후)
            - `waitingPosition`: 대기 순서 (WAITING인 경우에만 표시)
            - `extensionCount`: 연장 횟수 (기본값: 0, 최대 1회)
            - `overdueFee`: 연체료 (연체 중인 경우 표시)

            ## 제약사항
            - 인증 필요: 현재는 test-user로 테스트 중 (추후 Bearer Token 인증 적용 예정)
            - 도서가 대출 중이면 자동으로 대기 목록에 추가됩니다
            - 최대 대출 가능 권수: 5권 (추후 구현 예정)
            - 연체 중인 도서가 있으면 대출 불가 (추후 구현 예정)
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "대출 신청 성공 - Location 헤더에 생성된 리소스 URL 포함",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookLoanDto.Response.class),
                            examples = {
                                    @ExampleObject(name = "즉시 대출 성공", summary = "도서가 대출 가능하여 즉시 ACTIVE 상태로 생성됨", value = """
                                            {
                                              "id": 1,
                                              "bookId": 2,
                                              "bookTitle": "Clean Code (2nd Edition) - ISBN Changed",
                                              "bookAuthor": "Robert C. Martin",
                                              "bookIsbn": "9780132350884",
                                              "bookCoverImageUrl": "https://images.example.com/books/clean-code.jpg",
                                              "memberId": "f882b4bd-b04e-4d2a-aaac-f45ab772af72",
                                              "loanDate": "2025-08-06T00:38:22.279675",
                                              "dueDate": "2025-08-20T00:38:22.279675",
                                              "returnDate": null,
                                              "status": "ACTIVE",
                                              "overdue": false,
                                              "overdueFee": 0,
                                              "extensionCount": 0,
                                              "waitingPosition": null
                                            }
                                            """),
                                    @ExampleObject(name = "대기 목록 추가", summary = "도서가 대출 중이어서 WAITING 상태로 생성됨", value = """
                                            {
                                              "id": 2,
                                              "bookId": 2,
                                              "bookTitle": "Clean Code (2nd Edition) - ISBN Changed",
                                              "bookAuthor": "Robert C. Martin",
                                              "bookIsbn": "9780132350884",
                                              "bookCoverImageUrl": "https://images.example.com/books/clean-code.jpg",
                                              "memberId": "9a25cf99-1ab7-4331-b70a-54c1bf3211cc",
                                              "loanDate": null,
                                              "dueDate": null,
                                              "returnDate": null,
                                              "status": "WAITING",
                                              "overdue": false,
                                              "overdueFee": 0,
                                              "extensionCount": 0,
                                              "waitingPosition": 1
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Bad Request\", \"message\": \"도서 ID는 필수입니다\"}"))),
            @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Not Found\", \"message\": \"도서를 찾을 수 없습니다: 999\"}"))),
            @ApiResponse(responseCode = "422", description = "유효성 검증 실패",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "Validation Failed",
                                      "details": [
                                        {
                                          "field": "bookId",
                                          "message": "도서 ID는 필수입니다"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookLoanDto.Response> createLoan(
            @RequestBody(description = "도서 대출 신청 요청 데이터", required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BookLoanDto.Request.class),
                            examples = {
                                    @ExampleObject(
                                            name = "대출 신청 예시",
                                            summary = "도서 ID 2번 대출 신청",
                                            description = "실제 DB에 존재하는 도서 ID로 대출을 신청합니다",
                                            value = """
                                                    {
                                                      "bookId": 2
                                                    }
                                                    """
                                    )
                            }
                    ))
            @Valid @org.springframework.web.bind.annotation.RequestBody BookLoanDto.Request request,
            @Parameter(description = "사용자 ID (현재는 테스트용 기본값 사용, 추후 인증 시스템 연동 예정)", example = "test-user")
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        BookLoanDto.Response response = bookLoanService.createLoan(userId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "내 대출 목록 조회 (페이징)", description = """
            ## 개요
            본인의 도서 대출 목록을 조회합니다. 상태별 필터링 및 페이징을 지원합니다.

            ## 사용 예시
            - **전체 대출 내역 조회**: `/api/v1/loans?page=0&size=20`
            - **대출 중인 도서만**: `/api/v1/loans?statuses=ACTIVE&page=0&size=10`
            - **연체 도서 확인**: `/api/v1/loans?statuses=OVERDUE&page=0&size=10`
            - **복수 상태 조회**: `/api/v1/loans?statuses=ACTIVE,WAITING&page=0&size=10`

            ## 주요 파라미터
            - `statuses`: 대출 상태 필터 (복수 선택 가능, 선택)
              - PENDING: 대출 신청
              - WAITING: 대기 중
              - ACTIVE: 대출 중
              - OVERDUE: 연체
              - RETURNED: 반납 완료
              - CANCELLED: 취소됨
            - `page`: 페이지 번호, 0부터 시작 (기본값: 0)
            - `size`: 페이지 크기 (기본값: 20)
            - `sort`: 정렬 기준 (기본값: createdAt,desc)

            ## 응답 데이터
            페이지네이션된 대출 목록과 메타데이터를 반환합니다.
            - `content`: 대출 목록 배열
            - `totalElements`: 전체 대출 건수
            - `totalPages`: 전체 페이지 수
            - `page`: 현재 페이지 번호
            - `size`: 페이지 크기
            - `first`: 첫 페이지 여부
            - `last`: 마지막 페이지 여부

            ## 제약사항
            - 본인의 대출 기록만 조회 가능 (userId 기반 필터링)
            - 상태 필터 미지정 시 모든 상태의 대출 목록 반환
            - 최대 페이지 크기: 100 (추후 구현 예정)
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class),
                            examples = @ExampleObject(name = "대출 목록 조회 결과", value = """
                                    {
                                      "content": [
                                        {
                                          "id": 1,
                                          "bookId": 2,
                                          "bookTitle": "Clean Code (2nd Edition) - ISBN Changed",
                                          "bookAuthor": "Robert C. Martin",
                                          "bookIsbn": "9780132350884",
                                          "bookCoverImageUrl": "https://images.example.com/books/clean-code.jpg",
                                          "memberId": "f882b4bd-b04e-4d2a-aaac-f45ab772af72",
                                          "loanDate": "2025-08-06T00:38:22.279675",
                                          "dueDate": "2025-08-20T00:38:22.279675",
                                          "returnDate": null,
                                          "status": "ACTIVE",
                                          "overdue": false,
                                          "overdueFee": 0,
                                          "extensionCount": 0,
                                          "waitingPosition": null
                                        },
                                        {
                                          "id": 2,
                                          "bookId": 9,
                                          "bookTitle": "Effective Java",
                                          "bookAuthor": "Joshua Bloch",
                                          "bookIsbn": "9780134685991",
                                          "bookCoverImageUrl": "https://images.example.com/books/effective-java.jpg",
                                          "memberId": "f882b4bd-b04e-4d2a-aaac-f45ab772af72",
                                          "loanDate": "2025-08-06T02:18:07.091161",
                                          "dueDate": "2025-08-20T02:18:07.091161",
                                          "returnDate": null,
                                          "status": "ACTIVE",
                                          "overdue": false,
                                          "overdueFee": 0,
                                          "extensionCount": 0,
                                          "waitingPosition": null
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
            @ApiResponse(responseCode = "400", description = "잘못된 검색 파라미터",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Invalid parameter\", \"message\": \"Page size must not exceed 100\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<PageResponse<BookLoanDto.Response>> getMyLoans(
            @Valid BookLoanDto.SearchRequest request,
            @Parameter(description = "사용자 ID (현재는 테스트용 기본값 사용)", example = "test-user")
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        return ResponseEntity.ok(PageResponse.of(bookLoanService.getMyLoans(userId, request)));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "대출 상세 조회", description = """
            ## 개요
            특정 대출 건의 상세 정보를 조회합니다.
            대출 ID를 통해 도서 정보, 대출일, 반납 예정일, 연체 여부, 연체료 등 모든 정보를 확인할 수 있습니다.

            ## 주요 파라미터
            - `loanId`: 대출 ID (Long 타입, Path Parameter)

            ## 응답 데이터
            대출의 모든 정보를 포함한 상세 데이터를 반환합니다.
            - 대출 기본 정보: ID, 도서 ID, 회원 ID, 상태
            - 도서 정보: 제목, 저자, ISBN, 표지 이미지 URL
            - 날짜 정보: 대출일, 반납 예정일, 반납일
            - 연체 정보: 연체 여부, 연체료
            - 연장 정보: 연장 횟수
            - 대기 정보: 대기 순서 (WAITING 상태인 경우)

            ## 제약사항
            - 본인의 대출 기록만 조회 가능
            - 존재하지 않는 ID 조회 시 404 오류 발생
            - 타인의 대출 기록 조회 시 403 오류 발생
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookLoanDto.Response.class),
                            examples = @ExampleObject(name = "대출 상세 정보", value = """
                                    {
                                      "id": 1,
                                      "bookId": 2,
                                      "bookTitle": "Clean Code (2nd Edition) - ISBN Changed",
                                      "bookAuthor": "Robert C. Martin",
                                      "bookIsbn": "9780132350884",
                                      "bookCoverImageUrl": "https://images.example.com/books/clean-code.jpg",
                                      "memberId": "f882b4bd-b04e-4d2a-aaac-f45ab772af72",
                                      "loanDate": "2025-08-06T00:38:22.279675",
                                      "dueDate": "2025-08-20T00:38:22.279675",
                                      "returnDate": null,
                                      "status": "ACTIVE",
                                      "overdue": false,
                                      "overdueFee": 0,
                                      "extensionCount": 0,
                                      "waitingPosition": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "권한 없음 - 본인의 대출 기록만 조회 가능",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Forbidden\", \"message\": \"본인의 대출 기록만 조회할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "대출 기록을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Not Found\", \"message\": \"대출 기록을 찾을 수 없습니다: 999\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookLoanDto.Response> getLoan(
            @Parameter(description = "조회할 대출의 고유 ID - 실제 존재하는 대출 ID를 입력하세요", example = "1", required = true)
            @PathVariable Long loanId,
            @Parameter(description = "사용자 ID (현재는 테스트용 기본값 사용)", example = "test-user")
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        return ResponseEntity.ok(bookLoanService.getLoan(userId, loanId));
    }

    @PostMapping("/{loanId}/return")
    @Operation(summary = "도서 반납", description = """
            ## 개요
            대출 중이거나 연체 중인 도서를 반납합니다.
            반납 시 도서 상태가 AVAILABLE로 변경되고, 대기자가 있으면 알림이 발송됩니다.

            ## 주요 파라미터
            - `loanId`: 반납할 대출의 ID

            ## 응답 데이터
            - `status`: RETURNED (반납 완료)
            - `returnDate`: 실제 반납일
            - `overdueFee`: 연체료 (연체된 경우 표시)

            ## 제약사항
            - 본인의 대출 기록만 반납 가능
            - ACTIVE(대출중) 또는 OVERDUE(연체) 상태에서만 반납 가능
            - WAITING(대기), RETURNED(반납완료), CANCELLED(취소됨) 상태에서는 반납 불가

            ## 오류 예시
            - 잘못된 상태에서 반납 시도: `반납 처리는 ACTIVE 또는 OVERDUE 상태에서만 가능합니다.`
            - 타인의 대출 반납 시도: `본인의 대출 기록만 반납할 수 있습니다.`
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반납 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookLoanDto.Response.class),
                            examples = {
                                    @ExampleObject(name = "정상 반납", summary = "연체 없이 반납", value = """
                                            {
                                              "id": 1,
                                              "bookId": 2,
                                              "bookTitle": "Clean Code (2nd Edition) - ISBN Changed",
                                              "bookAuthor": "Robert C. Martin",
                                              "bookIsbn": "9780132350884",
                                              "bookCoverImageUrl": "https://images.example.com/books/clean-code.jpg",
                                              "memberId": "f882b4bd-b04e-4d2a-aaac-f45ab772af72",
                                              "loanDate": "2025-08-06T00:38:22.279675",
                                              "dueDate": "2025-08-20T00:38:22.279675",
                                              "returnDate": "2025-08-15T10:30:00",
                                              "status": "RETURNED",
                                              "overdue": false,
                                              "overdueFee": 0,
                                              "extensionCount": 0,
                                              "waitingPosition": null
                                            }
                                            """),
                                    @ExampleObject(name = "연체 후 반납", summary = "연체료 발생 후 반납", value = """
                                            {
                                              "id": 1,
                                              "bookId": 2,
                                              "bookTitle": "Clean Code (2nd Edition) - ISBN Changed",
                                              "bookAuthor": "Robert C. Martin",
                                              "bookIsbn": "9780132350884",
                                              "bookCoverImageUrl": "https://images.example.com/books/clean-code.jpg",
                                              "memberId": "f882b4bd-b04e-4d2a-aaac-f45ab772af72",
                                              "loanDate": "2025-08-06T00:38:22.279675",
                                              "dueDate": "2025-08-20T00:38:22.279675",
                                              "returnDate": "2025-08-25T14:20:00",
                                              "status": "RETURNED",
                                              "overdue": false,
                                              "overdueFee": 0,
                                              "extensionCount": 0,
                                              "waitingPosition": null
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 반납 불가능한 상태",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Bad Request\", \"message\": \"반납 처리는 ACTIVE 또는 OVERDUE 상태에서만 가능합니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 - 본인의 대출 기록만 반납 가능",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Forbidden\", \"message\": \"본인의 대출 기록만 반납할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "대출 기록을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Not Found\", \"message\": \"대출 기록을 찾을 수 없습니다: 999\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookLoanDto.Response> returnBook(
            @Parameter(description = "반납할 대출의 고유 ID - ACTIVE 또는 OVERDUE 상태인 대출 ID를 입력하세요", example = "1", required = true)
            @PathVariable Long loanId,
            @Parameter(description = "사용자 ID (현재는 테스트용 기본값 사용)", example = "test-user")
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        return ResponseEntity.ok(bookLoanService.returnBook(userId, loanId));
    }

    @PostMapping("/{loanId}/extend")
    @Operation(summary = "대출 기간 연장", description = """
            ## 개요
            대출 기간을 1주일 연장합니다. 연장은 최대 1회까지만 가능합니다.
            대기자가 있는 도서는 연장할 수 없습니다.

            ## 주요 파라미터
            - `loanId`: 연장할 대출의 ID

            ## 응답 데이터
            - `dueDate`: 연장된 반납 예정일 (기존 반납일 + 7일)
            - `extensionCount`: 연장 횟수 증가

            ## 제약사항
            - 본인의 대출 기록만 연장 가능
            - ACTIVE(대출중) 상태에서만 연장 가능
            - 최대 연장 횟수: 1회
            - 연체 중인 도서는 연장 불가
            - 대기자가 있는 도서는 연장 불가

            ## 오류 예시
            - 대기자가 있는 경우: `대기자가 있는 도서는 연장할 수 없습니다.`
            - 연장 횟수 초과: `최대 연장 횟수(1회)를 초과했습니다.`
            - 잘못된 상태: `연장은 ACTIVE 상태에서만 가능합니다.`
            - 연체 중: `연체 중인 도서는 연장할 수 없습니다.`
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "연장 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookLoanDto.Response.class),
                            examples = @ExampleObject(name = "연장 성공", value = """
                                    {
                                      "id": 1,
                                      "bookId": 2,
                                      "bookTitle": "Clean Code (2nd Edition) - ISBN Changed",
                                      "bookAuthor": "Robert C. Martin",
                                      "bookIsbn": "9780132350884",
                                      "bookCoverImageUrl": "https://images.example.com/books/clean-code.jpg",
                                      "memberId": "f882b4bd-b04e-4d2a-aaac-f45ab772af72",
                                      "loanDate": "2025-08-06T00:38:22.279675",
                                      "dueDate": "2025-08-27T00:38:22.279675",
                                      "returnDate": null,
                                      "status": "ACTIVE",
                                      "overdue": false,
                                      "overdueFee": 0,
                                      "extensionCount": 1,
                                      "waitingPosition": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 연장 불가능한 상태",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "대기자 존재", value = "{\"error\": \"Bad Request\", \"message\": \"대기자가 있는 도서는 연장할 수 없습니다.\"}"),
                                    @ExampleObject(name = "연장 횟수 초과", value = "{\"error\": \"Bad Request\", \"message\": \"최대 연장 횟수(1회)를 초과했습니다.\"}"),
                                    @ExampleObject(name = "연체 중", value = "{\"error\": \"Bad Request\", \"message\": \"연체 중인 도서는 연장할 수 없습니다.\"}")
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음 - 본인의 대출 기록만 연장 가능",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Forbidden\", \"message\": \"본인의 대출 기록만 연장할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "대출 기록을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"Not Found\", \"message\": \"대출 기록을 찾을 수 없습니다: 999\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<BookLoanDto.Response> extendLoan(
            @Parameter(description = "연장할 대출의 고유 ID - ACTIVE 상태이며 연장 횟수가 1회 미만인 대출 ID를 입력하세요", example = "1", required = true)
            @PathVariable Long loanId,
            @Parameter(description = "사용자 ID (현재는 테스트용 기본값 사용)", example = "test-user")
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        return ResponseEntity.ok(bookLoanService.extendLoan(userId, loanId));
    }
}
