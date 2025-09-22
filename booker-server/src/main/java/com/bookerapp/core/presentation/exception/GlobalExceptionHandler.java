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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidBookException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBookException(
            InvalidBookException e, HttpServletRequest request) {
        logger.warn("InvalidBookException: {}", e.getMessage());
        ErrorResponse errorResponse =  ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "잘못된 도서 정보",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateIsbnException(
            DuplicateIsbnException e, HttpServletRequest request) {
        logger.warn("DuplicateIsbnException: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.CONFLICT,
                "중복된 ISBN",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InvalidFloorException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFloorException(
            InvalidFloorException e, HttpServletRequest request) {
        logger.warn("InvalidFloorException: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "잘못된 층 정보",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException e, HttpServletRequest request) {
        logger.warn("EntityNotFoundException: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND,
                "리소스를 찾을 수 없음",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BookException.class)
    public ResponseEntity<ErrorResponse> handleBookException(
            BookException e, HttpServletRequest request) {
        logger.warn("BookException: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "도서 관련 오류",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        logger.error("시스템 오류 (IllegalArgumentException): {}", e.getMessage(), e);
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "시스템 오류가 발생했습니다",
                "관리자에게 문의해주세요",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


    @ExceptionHandler(BookOrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookOrderNotFoundException(BookOrderNotFoundException e) {
        logger.warn("도서 주문을 찾을 수 없음: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND,
                "도서 주문을 찾을 수 없음",
                e.getMessage(),
                "BookOrderNotFoundException"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(DeletedBookOrderException.class)
    public ResponseEntity<ErrorResponse> handleDeletedBookOrderException(DeletedBookOrderException e) {
        logger.warn("삭제된 도서 주문에 접근: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.GONE,
                "삭제된 도서 주문",
                e.getMessage(),
                "DeletedBookOrderException"
        );
        return ResponseEntity.status(HttpStatus.GONE).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, message, "MethodArgumentNotValidException", request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        String message = "잘못된 JSON 형식입니다.";
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            String cause = e.getCause().getMessage();
            if (cause.contains("Floor")) {
                message = "층수는 'FOURTH' 또는 'TWELFTH'만 가능합니다.";
            }
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, message, "HttpMessageNotReadableException", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        logger.error("예상치 못한 오류 발생", e);
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "내부 서버 오류",
                "예상치 못한 오류가 발생했습니다.",
                "Exception"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(NotPendingStatusException.class)
    public ResponseEntity<ErrorResponse> handleNotPendingStatusException(NotPendingStatusException e) {
        logger.warn("승인 대기 상태 아님: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "승인 대기 상태 아님",
                e.getMessage(),
                "NotPendingStatusException"
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(NotApprovedStatusException.class)
    public ResponseEntity<ErrorResponse> handleNotApprovedStatusException(NotApprovedStatusException e) {
        logger.warn("승인된 상태 아님: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "승인된 상태 아님",
                e.getMessage(),
                "NotApprovedStatusException"
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

}
