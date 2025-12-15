package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.BookLoanDto;
import com.bookerapp.core.domain.service.BookLoanService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Book Loan", description = "도서 대출 관련 API")
public class BookLoanController {

    private final BookLoanService bookLoanService;

    @PostMapping
    @Operation(
        summary = "도서 대출 신청",
        description = "도서 대출을 신청합니다. 대출 신청 시 WAITING 상태로 생성되며, 관리자 승인 후 ACTIVE 상태로 변경됩니다."
    )
    public ResponseEntity<BookLoanDto.Response> createLoan(
            @Valid @RequestBody BookLoanDto.Request request,
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        BookLoanDto.Response response = bookLoanService.createLoan(userId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{loanId}/return")
    @Operation(
        summary = "도서 반납 신청",
        description = "도서를 반납합니다.\n\n" +
                      "**제약 조건:**\n" +
                      "- 반납은 ACTIVE(대출중) 또는 OVERDUE(연체) 상태의 대출만 가능합니다.\n" +
                      "- WAITING(대기), RETURNED(반납완료), CANCELLED(취소됨) 상태에서는 반납할 수 없습니다.\n\n" +
                      "**오류 예시:**\n" +
                      "- 상태가 WAITING인 경우: `반납 처리는 ACTIVE 또는 OVERDUE 상태에서만 가능합니다.`"
    )
    public ResponseEntity<BookLoanDto.Response> returnBook(
            @PathVariable Long loanId,
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        return ResponseEntity.ok(bookLoanService.returnBook(userId, loanId));
    }

    @PostMapping("/{loanId}/extend")
    @Operation(
        summary = "대출 기간 연장",
        description = "대출 기간을 연장합니다.\n\n" +
                      "**제약 조건:**\n" +
                      "- 해당 도서에 대기자가 있는 경우 연장할 수 없습니다.\n" +
                      "- ACTIVE 상태의 대출만 연장 가능합니다.\n\n" +
                      "**오류 예시:**\n" +
                      "- 대기자가 있는 경우: `대기자가 있는 도서는 연장할 수 없습니다.`"
    )
    public ResponseEntity<BookLoanDto.Response> extendLoan(
            @PathVariable Long loanId,
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        return ResponseEntity.ok(bookLoanService.extendLoan(userId, loanId));
    }

    @GetMapping
    @Operation(
        summary = "내 대출 목록 조회",
        description = "본인의 도서 대출 목록을 조회합니다. 상태별 필터링 및 페이징을 지원합니다."
    )
    public ResponseEntity<Page<BookLoanDto.Response>> getMyLoans(
            @Valid BookLoanDto.SearchRequest request,
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        return ResponseEntity.ok(bookLoanService.getMyLoans(userId, request));
    }

    @GetMapping("/{loanId}")
    @Operation(
        summary = "대출 상세 조회",
        description = "특정 대출 건의 상세 정보를 조회합니다."
    )
    public ResponseEntity<BookLoanDto.Response> getLoan(
            @PathVariable Long loanId,
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        return ResponseEntity.ok(bookLoanService.getLoan(userId, loanId));
    }
}
