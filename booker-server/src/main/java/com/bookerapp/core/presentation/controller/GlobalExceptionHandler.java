package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.presentation.exception.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Error.Response> handleSecurityException(SecurityException ex) {
        Error.Response errorResponse = Error.Response.of(
            HttpStatus.FORBIDDEN,
            "권한 부족",
            ex.getMessage(),
            "/api/test/books"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
}
