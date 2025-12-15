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
        private Long id;
        private Long bookId;
        private String bookTitle;
        private String memberId;
        private LocalDateTime loanDate;
        private LocalDateTime dueDate;
        private LocalDateTime returnDate;
        private LoanStatus status;

        public static Response from(BookLoan loan) {
            Response response = new Response();
            response.id = loan.getId();
            response.bookId = loan.getBook().getId();
            response.bookTitle = loan.getBook().getTitle();
            response.memberId = loan.getMemberId();
            response.loanDate = loan.getLoanDate();
            response.dueDate = loan.getDueDate();
            response.returnDate = loan.getReturnDate();
            response.status = loan.getStatus();
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
