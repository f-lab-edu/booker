package com.bookerapp.core.domain.exception;

public class InvalidBookException extends BookException {
    public InvalidBookException(String message) {
        super(message);
    }
}
