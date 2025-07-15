package com.bookerapp.core.presentation.exception;

public class NotPendingStatusException extends RuntimeException {
    public NotPendingStatusException() {
        super("승인 대기 중인 요청만 처리할 수 있습니다.");
    }
    public NotPendingStatusException(String message) {
        super(message);
    }
}
