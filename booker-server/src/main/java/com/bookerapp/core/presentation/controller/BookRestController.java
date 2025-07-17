package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.dto.BookDto;
import com.bookerapp.core.domain.model.auth.Role;
import io.swagger.v3.oas.annotations.Operation;
import com.bookerapp.core.domain.service.BookService;
import com.bookerapp.core.presentation.aspect.RequireRoles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

// 깃 충돌 방지를 위해, 테스트용으로 임시 컨트롤러 추가

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookRestController {

    private final BookService bookService;

    @PostMapping
    @Operation(summary = "도서 생성")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<BookDto.Response> createBook(@Valid @RequestBody BookDto.Request request) {
        BookDto.Response response = bookService.createBook(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "도서 조회")
    @RequireRoles({Role.USER})
    public ResponseEntity<BookDto.Response> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBook(id));
    }

    @GetMapping
    @Operation(summary = "도서 검색")
    @RequireRoles({Role.USER})
    public ResponseEntity<Page<BookDto.Response>> searchBooks(BookDto.SearchRequest request) {
        return ResponseEntity.ok(bookService.searchBooks(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "도서 수정")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<BookDto.Response> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookDto.Request request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "도서 삭제")
    @RequireRoles({Role.ADMIN})
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
