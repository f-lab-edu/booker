package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.domain.model.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Book", description = "Book management APIs")
public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @GetMapping("/books")
    @Operation(summary = "Get all books")
    public String getAllBooks(UserContext userContext) {
        validateUserAccess(userContext, "USER", "ADMIN");
        logger.info("getAllBooks called by user: {} with roles: {}", userContext.getUserId(), userContext.getRoles());
        return "List of all books";
    }

    @GetMapping("/books/{id}")
    @Operation(summary = "Get book by ID")
    public String getBookById(@PathVariable String id, UserContext userContext) {
        validateUserAccess(userContext, "USER", "ADMIN");
        logger.info("getBookById called for id: {} by user: {} with roles: {}", id, userContext.getUserId(), userContext.getRoles());
        return "Book with ID: " + id;
    }

    @PostMapping("/books")
    @Operation(summary = "Create a new book")
    public String createBook(@RequestBody String bookData, UserContext userContext) {
        validateAdminAccess(userContext);
        logger.info("createBook called with data: {} by user: {} with roles: {}", bookData, userContext.getUserId(), userContext.getRoles());
        return "Created book: " + bookData;
    }

    @PutMapping("/books/{id}")
    @Operation(summary = "Update a book")
    public String updateBook(@PathVariable String id, @RequestBody String bookData, UserContext userContext) {
        validateAdminAccess(userContext);
        logger.info("updateBook called for id: {} with data: {} by user: {} with roles: {}", id, bookData, userContext.getUserId(), userContext.getRoles());
        return "Updated book " + id + " with: " + bookData;
    }

    @DeleteMapping("/books/{id}")
    @Operation(summary = "Delete a book")
    public String deleteBook(@PathVariable String id, UserContext userContext) {
        validateAdminAccess(userContext);
        logger.info("deleteBook called for id: {} by user: {} with roles: {}", id, userContext.getUserId(), userContext.getRoles());
        return "Deleted book: " + id;
    }
    
    @GetMapping("/user/info")
    @Operation(summary = "Get current user information")
    public Map<String, Object> getUserInfo(UserContext userContext) {
        logger.info("getUserInfo called by user: {} with roles: {}", userContext.getUserId(), userContext.getRoles());
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userContext.getUserId());
        userInfo.put("username", userContext.getUsername());
        userInfo.put("email", userContext.getEmail());
        userInfo.put("roles", userContext.getRoles());
        userInfo.put("authenticated", userContext.getUserId() != null);
        
        return userInfo;
    }
    
    private void validateUserAccess(UserContext userContext, String... allowedRoles) {
        logger.debug("Validating user access - UserContext: {}, Required roles: {}", 
                    userContext != null ? userContext.getRoles() : "null", 
                    java.util.Arrays.toString(allowedRoles));
        
        if (userContext == null || userContext.getUserId() == null) {
            logger.warn("User not authenticated - UserContext: {}", userContext);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        
        if (!userContext.hasAnyRole(allowedRoles)) {
            logger.warn("Insufficient permissions - User roles: {}, Required roles: {}", 
                       userContext.getRoles(), java.util.Arrays.toString(allowedRoles));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
        
        logger.debug("Access granted - User roles: {}", userContext.getRoles());
    }
    
    private void validateAdminAccess(UserContext userContext) {
        validateUserAccess(userContext, "ADMIN");
    }
}
