package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Book", description = "Book management APIs")
public class BookController {

    @GetMapping
    @Operation(summary = "Get all books", security = @SecurityRequirement(name = "bearerAuth"))
    public String getAllBooks() {
        return "List of all books";
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public String getBookById(@PathVariable String id) {
        return "Book with ID: " + id;
    }

    @PostMapping
    @Operation(summary = "Create a new book", security = @SecurityRequirement(name = "bearerAuth"))
    public String createBook(@RequestBody String bookData) {
        return "Created book: " + bookData;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a book", security = @SecurityRequirement(name = "bearerAuth"))
    public String updateBook(@PathVariable String id, @RequestBody String bookData) {
        return "Updated book " + id + " with: " + bookData;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book", security = @SecurityRequirement(name = "bearerAuth"))
    public String deleteBook(@PathVariable String id) {
        return "Deleted book: " + id;
    }
} 