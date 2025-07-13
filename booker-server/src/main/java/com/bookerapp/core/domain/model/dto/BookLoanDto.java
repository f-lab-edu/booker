package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.entity.BookLoan;
import com.bookerapp.core.domain.model.entity.LoanStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class BookLoanDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        private Long bookId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
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
    public static class SearchRequest {
        private List<LoanStatus> statuses;
        private int page = 0;
        private int size = 10;
    }
} 