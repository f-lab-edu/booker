package com.bookerapp.core.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Book", description = "Book management APIs")
public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @GetMapping
    @Operation(summary = "Get all books", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public String getAllBooks() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("getAllBooks called by user with authorities: {}", auth.getAuthorities());
        return "List of all books";
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public String getBookById(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("getBookById called for id: {} by user with authorities: {}", id, auth.getAuthorities());
        return "Book with ID: " + id;
    }

    @PostMapping
    @Operation(summary = "Create a new book", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public String createBook(@RequestBody String bookData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("createBook called with data: {} by user with authorities: {}", bookData, auth.getAuthorities());
        return "Created book: " + bookData;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a book", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public String updateBook(@PathVariable String id, @RequestBody String bookData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("updateBook called for id: {} with data: {} by user with authorities: {}", id, bookData, auth.getAuthorities());
        return "Updated book " + id + " with: " + bookData;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public String deleteBook(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("deleteBook called for id: {} by user with authorities: {}", id, auth.getAuthorities());
        return "Deleted book: " + id;
    }
} 