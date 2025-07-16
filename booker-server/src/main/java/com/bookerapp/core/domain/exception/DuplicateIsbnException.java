package com.bookerapp.core.domain.exception;

public class DuplicateIsbnException extends BookException {
    public DuplicateIsbnException(String isbn) {
        super(String.format("이미 등록된 ISBN입니다: %s", isbn));
    }
}
