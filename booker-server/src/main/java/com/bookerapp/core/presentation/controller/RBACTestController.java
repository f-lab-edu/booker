package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.model.auth.Role;
import com.bookerapp.core.domain.model.response.UserResponse;
import com.bookerapp.core.presentation.aspect.RequireRoles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "RBACTest", description = "RBACTest APIs")
public class RBACTestController {
    private static final Logger logger = LoggerFactory.getLogger(RBACTestController.class);

    @GetMapping("/test/books")
    @Operation(summary = "Get all books")
    @RequireRoles({Role.USER, Role.ADMIN})
    public String getAllBooks(
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("getAllBooks called by user: {} with roles: {}", userContext.getUserId(), userContext.getRoles());
        return "List of all books";
    }

    @GetMapping("/test/books/{id}")
    @Operation(summary = "Get book by ID")
    @RequireRoles({Role.USER, Role.ADMIN})
    public String getBookById(
            @PathVariable String id,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("getBookById called for id: {} by user: {} with roles: {}", id, userContext.getUserId(), userContext.getRoles());
        return "Book with ID: " + id;
    }

    @PostMapping("/test/books")
    @Operation(summary = "Create a new book")
    @RequireRoles({Role.ADMIN})
    public String createBook(
            @RequestBody String bookData,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("createBook called with data: {} by user: {} with roles: {}", bookData, userContext.getUserId(), userContext.getRoles());
        return "Created book: " + bookData;
    }

    @PutMapping("/test/books/{id}")
    @Operation(summary = "Update a book")
    @RequireRoles({Role.ADMIN})
    public String updateBook(
            @PathVariable String id,
            @RequestBody String bookData,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("updateBook called for id: {} with data: {} by user: {} with roles: {}", id, bookData, userContext.getUserId(), userContext.getRoles());
        return "Updated book " + id + " with: " + bookData;
    }

    @DeleteMapping("/test/books/{id}")
    @Operation(summary = "Delete a book")
    @RequireRoles({Role.ADMIN})
    public String deleteBook(
            @PathVariable String id,
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("deleteBook called for id: {} by user: {} with roles: {}", id, userContext.getUserId(), userContext.getRoles());
        return "Deleted book: " + id;
    }

    @GetMapping("/test/user/info")
    @Operation(summary = "Get current user information")
    public UserResponse getUserInfo(
            @Parameter(hidden = true) UserContext userContext
    ) {
        logger.info("getUserInfo called by user: {} with roles: {}", userContext.getUserId(), userContext.getRoles());
        return new UserResponse(
            userContext.getUserId(),
            userContext.getUsername(),
            userContext.getEmail(),
            userContext.getRolesAsRole(),
            userContext.getUserId() != null
        );
    }
}
