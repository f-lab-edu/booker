package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.entity.BookLoan;
import com.bookerapp.core.domain.model.enums.LoanStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class BookLoanDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "BookLoanRequest") // OpenAPI 스키마 네이밍 충돌 방지
    public static class Request {
        @NotNull(message = "도서 ID는 필수입니다")
        @Schema(description = "대출할 도서의 ID", example = "1", required = true)
        private Long bookId;

        public Request(Long bookId) {
            this.bookId = bookId;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "BookLoanResponse")
    public static class Response {
        @Schema(description = "대출 ID", example = "1")
        private Long id;

        @Schema(description = "도서 ID", example = "2")
        private Long bookId;

        @Schema(description = "도서 제목", example = "Clean Code (2nd Edition) - ISBN Changed")
        private String bookTitle;

        @Schema(description = "도서 저자", example = "Robert C. Martin")
        private String bookAuthor;

        @Schema(description = "도서 ISBN", example = "9780132350884")
        private String bookIsbn;

        @Schema(description = "도서 표지 이미지 URL", example = "https://images.example.com/books/clean-code.jpg")
        private String bookCoverImageUrl;

        @Schema(description = "회원 ID", example = "f882b4bd-b04e-4d2a-aaac-f45ab772af72")
        private String memberId;

        @Schema(description = "대출 개시일 - ACTIVE 상태일 때만 존재", example = "2025-08-06T00:38:22.279675")
        private LocalDateTime loanDate;

        @Schema(description = "반납 예정일 - ACTIVE 상태일 때만 존재, 대출일로부터 2주(14일) 후", example = "2025-08-20T00:38:22.279675")
        private LocalDateTime dueDate;

        @Schema(description = "실제 반납일 - RETURNED 상태일 때만 존재", example = "2025-08-15T10:30:00")
        private LocalDateTime returnDate;

        @Schema(description = "대출 상태 - PENDING(대출신청), WAITING(대기중), ACTIVE(대출중), OVERDUE(연체), RETURNED(반납완료), CANCELLED(취소됨)",
                example = "ACTIVE")
        private LoanStatus status;

        @Schema(description = "연체 여부 - 반납예정일 초과 시 true", example = "false")
        private boolean overdue;

        @Schema(description = "연체료 (원) - 연체일 × 100원, 반납 전에만 표시", example = "0")
        private Integer overdueFee;

        @Schema(description = "연장 횟수 - 최대 1회까지 가능", example = "0")
        private Integer extensionCount;

        @Schema(description = "대기 순서 - WAITING 상태인 경우만 표시", example = "3")
        private Integer waitingPosition;

        public static Response from(BookLoan loan) {
            Response response = new Response();
            response.id = loan.getId();
            response.bookId = loan.getBook().getId();
            response.bookTitle = loan.getBook().getTitle();
            response.bookAuthor = loan.getBook().getAuthor();
            response.bookIsbn = loan.getBook().getIsbn();
            response.bookCoverImageUrl = loan.getBook().getCoverImageUrl();
            response.memberId = loan.getMemberId();
            response.loanDate = loan.getLoanDate();
            response.dueDate = loan.getDueDate();
            response.returnDate = loan.getReturnDate();
            response.status = loan.getStatus();
            response.overdue = loan.isOverdue();
            response.overdueFee = loan.calculateOverdueFee();
            response.extensionCount = loan.getExtensionCount();
            // waitingPosition은 서비스 레이어에서 설정
            return response;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "BookLoanSearchRequest", description = "대출 검색 요청")
    public static class SearchRequest {
        @Schema(description = "대출 상태 목록 (복수 선택 가능)", example = "[\"ACTIVE\", \"WAITING\"]")
        private List<LoanStatus> statuses;

        @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
        private int page = 0;

        @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
        private int size = 20;

        @Schema(description = "정렬 기준 (예: createdAt,desc)", example = "createdAt,desc", defaultValue = "createdAt,desc")
        private String sort = "createdAt,desc";
    }
}
