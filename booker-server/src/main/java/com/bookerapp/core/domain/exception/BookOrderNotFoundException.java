package com.bookerapp.core.domain.exception;

public class BookOrderNotFoundException extends RuntimeException {
    public BookOrderNotFoundException(Long id) {
        super(String.format("도서 주문 요청을 찾을 수 없습니다. ID: %d", id));
    }
}
