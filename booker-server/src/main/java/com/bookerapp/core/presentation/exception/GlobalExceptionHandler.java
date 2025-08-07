package com.bookerapp.core.presentation.exception;

import com.bookerapp.core.domain.exception.BookException;
import com.bookerapp.core.domain.exception.BookOrderNotFoundException;
import com.bookerapp.core.domain.exception.DeletedBookOrderException;
import com.bookerapp.core.domain.exception.DuplicateIsbnException;
import com.bookerapp.core.domain.exception.InvalidBookException;
import com.bookerapp.core.domain.exception.InvalidFloorException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidBookException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBookException(
            InvalidBookException e, HttpServletRequest request) {
        logger.warn("InvalidBookException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "잘못된 도서 정보",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateIsbnException(
            DuplicateIsbnException e, HttpServletRequest request) {
        logger.warn("DuplicateIsbnException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "중복된 ISBN",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InvalidFloorException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFloorException(
            InvalidFloorException e, HttpServletRequest request) {
        logger.warn("InvalidFloorException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "잘못된 층 정보",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException e, HttpServletRequest request) {
        logger.warn("EntityNotFoundException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "리소스를 찾을 수 없음",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BookException.class)
    public ResponseEntity<ErrorResponse> handleBookException(
            BookException e, HttpServletRequest request) {
        logger.warn("BookException: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "도서 관련 오류",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.error("시스템 오류 (IllegalArgumentException): {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "시스템 오류가 발생했습니다",
                "관리자에게 문의해주세요",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        logger.warn("잘못된 상태: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "잘못된 상태",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        logger.warn("유효성 검사 실패: {}", errors);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "유효성 검사 실패",
                errors.toString(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        logger.warn("ResponseStatusException: {}", e.getReason());
        ErrorResponse errorResponse = new ErrorResponse(
                e.getStatusCode().value(),
                e.getStatusCode().toString(),
                e.getReason(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(BookOrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookOrderNotFoundException(BookOrderNotFoundException e) {
        logger.warn("도서 주문을 찾을 수 없음: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "도서 주문을 찾을 수 없음",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(DeletedBookOrderException.class)
    public ResponseEntity<ErrorResponse> handleDeletedBookOrderException(DeletedBookOrderException e) {
        logger.warn("삭제된 도서 주문에 접근: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.GONE.value(),
                "삭제된 도서 주문",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.GONE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        logger.error("예상치 못한 오류 발생", e);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "내부 서버 오류",
                "예상치 못한 오류가 발생했습니다.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(NotPendingStatusException.class)
    public ResponseEntity<ErrorResponse> handleNotPendingStatusException(NotPendingStatusException e) {
        logger.warn("승인 대기 상태 아님: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "승인 대기 상태 아님",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(NotApprovedStatusException.class)
    public ResponseEntity<ErrorResponse> handleNotApprovedStatusException(NotApprovedStatusException e) {
        logger.warn("승인된 상태 아님: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "승인된 상태 아님",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
            this.status = status;
            this.error = error;
            this.message = message;
            this.timestamp = timestamp;
        }

        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
