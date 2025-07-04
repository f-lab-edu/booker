package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.UserContext;
import com.bookerapp.core.domain.model.Role;
import com.bookerapp.core.domain.model.UserResponse;
import com.bookerapp.core.presentation.aspect.RequireRoles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Book", description = "Book management APIs")
public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @GetMapping("/books")
    @Operation(summary = "Get all books")
    @RequireRoles({Role.USER, Role.ADMIN})
    public String getAllBooks(UserContext userContext) {
        logger.info("getAllBooks called by user: {} with roles: {}", userContext.getUserId(), userContext.getRoles());
        return "List of all books";
    }

    @GetMapping("/books/{id}")
    @Operation(summary = "Get book by ID")
    @RequireRoles({Role.USER, Role.ADMIN})
    public String getBookById(@PathVariable String id, UserContext userContext) {
        logger.info("getBookById called for id: {} by user: {} with roles: {}", id, userContext.getUserId(), userContext.getRoles());
        return "Book with ID: " + id;
    }

    @PostMapping("/books")
    @Operation(summary = "Create a new book")
    @RequireRoles({Role.ADMIN})
    public String createBook(@RequestBody String bookData, UserContext userContext) {
        logger.info("createBook called with data: {} by user: {} with roles: {}", bookData, userContext.getUserId(), userContext.getRoles());
        return "Created book: " + bookData;
    }

    @PutMapping("/books/{id}")
    @Operation(summary = "Update a book")
    @RequireRoles({Role.ADMIN})
    public String updateBook(@PathVariable String id, @RequestBody String bookData, UserContext userContext) {
        logger.info("updateBook called for id: {} with data: {} by user: {} with roles: {}", id, bookData, userContext.getUserId(), userContext.getRoles());
        return "Updated book " + id + " with: " + bookData;
    }

    @DeleteMapping("/books/{id}")
    @Operation(summary = "Delete a book")
    @RequireRoles({Role.ADMIN})
    public String deleteBook(@PathVariable String id, UserContext userContext) {
        logger.info("deleteBook called for id: {} by user: {} with roles: {}", id, userContext.getUserId(), userContext.getRoles());
        return "Deleted book: " + id;
    }
    
    @GetMapping("/user/info")
    @Operation(summary = "Get current user information")
    public UserResponse getUserInfo(UserContext userContext) {
        logger.info("getUserInfo called by user: {} with roles: {}", userContext.getUserId(), userContext.getRoles());
        return new UserResponse(
            userContext.getUserId(),
            userContext.getUsername(),
            userContext.getEmail(),
            userContext.getRoles(),
            userContext.getUserId() != null
        );
    }

}
