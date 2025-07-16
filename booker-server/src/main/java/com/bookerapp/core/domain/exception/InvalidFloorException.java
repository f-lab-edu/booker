package com.bookerapp.core.domain.exception;

public class InvalidFloorException extends BookException {
    public InvalidFloorException(int value) {
        super(String.format("유효하지 않은 층수입니다: %d", value));
    }
}
