package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.BookDto;
import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.auth.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.bookerapp.core.domain.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    @Operation(summary = "도서 생성")
    public ResponseEntity<BookDto.Response> createBook(
            @Valid @RequestBody BookDto.Request request,
            @Parameter(hidden = true) UserContext userContext) {
        try {
            BookDto.Response response = bookService.createBook(request, userContext);
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
    @Operation(summary = "도서 조회")
    public ResponseEntity<BookDto.Response> getBook(
            @PathVariable Long id,
            @Parameter(hidden = true) UserContext userContext) {
        return ResponseEntity.ok(bookService.getBook(id));
    }

    @GetMapping
    @Operation(summary = "도서 검색")
    public ResponseEntity<Page<BookDto.Response>> searchBooks(
            BookDto.SearchRequest request,
            @Parameter(hidden = true) UserContext userContext) {
        return ResponseEntity.ok(bookService.searchBooks(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "도서 수정")
    public ResponseEntity<BookDto.Response> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookDto.Request request,
            @Parameter(hidden = true) UserContext userContext) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "도서 삭제")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long id,
            @Parameter(hidden = true) UserContext userContext) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
