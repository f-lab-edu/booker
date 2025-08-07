package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.dto.BookLoanDto;
import com.bookerapp.core.domain.service.BookLoanService;
import com.bookerapp.core.presentation.aspect.RequireRoles;
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
    @Operation(summary = "도서 대출 신청")
    @RequireRoles({Role.USER})
    public ResponseEntity<BookLoanDto.Response> createLoan(
            @Valid @RequestBody BookLoanDto.Request request,
            UserContext userContext) {
        BookLoanDto.Response response = bookLoanService.createLoan(userContext.getUserId(), request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{loanId}/return")
    @Operation(summary = "도서 반납 신청")
    @RequireRoles({Role.USER})
    public ResponseEntity<BookLoanDto.Response> returnBook(
            @PathVariable Long loanId,
            UserContext userContext) {
        return ResponseEntity.ok(bookLoanService.returnBook(userContext.getUserId(), loanId));
    }

    @PostMapping("/{loanId}/extend")
    @Operation(summary = "대출 기간 연장")
    @RequireRoles({Role.USER})
    public ResponseEntity<BookLoanDto.Response> extendLoan(
            @PathVariable Long loanId,
            UserContext userContext) {
        return ResponseEntity.ok(bookLoanService.extendLoan(userContext.getUserId(), loanId));
    }

    @GetMapping
    @Operation(summary = "내 대출 목록 조회")
    @RequireRoles({Role.USER})
    public ResponseEntity<Page<BookLoanDto.Response>> getMyLoans(
            @Valid BookLoanDto.SearchRequest request,
            UserContext userContext) {
        return ResponseEntity.ok(bookLoanService.getMyLoans(userContext.getUserId(), request));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "대출 상세 조회")
    @RequireRoles({Role.USER})
    public ResponseEntity<BookLoanDto.Response> getLoan(
            @PathVariable Long loanId,
            UserContext userContext) {
        return ResponseEntity.ok(bookLoanService.getLoan(userContext.getUserId(), loanId));
    }
} 