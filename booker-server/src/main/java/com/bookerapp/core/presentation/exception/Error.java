package com.bookerapp.core.presentation.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class Error {

    @Getter
    @RequiredArgsConstructor
    public static class Response {
        private final LocalDateTime timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;

        public static Response of(HttpStatus status, String error, String message, String path) {
            return new Response(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path
            );
        }
    }
} 
