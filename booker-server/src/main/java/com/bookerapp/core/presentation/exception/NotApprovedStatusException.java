package com.bookerapp.core.presentation.exception;

public class NotApprovedStatusException extends RuntimeException {
    public NotApprovedStatusException() {
        super("승인된 요청만 입고 처리할 수 있습니다.");
    }
    public NotApprovedStatusException(String message) {
        super(message);
    }
}
