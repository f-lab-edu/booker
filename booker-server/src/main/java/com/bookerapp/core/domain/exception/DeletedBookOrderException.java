package com.bookerapp.core.domain.exception;

public class DeletedBookOrderException extends RuntimeException {
    public DeletedBookOrderException(Long id) {
        super(String.format("삭제된 도서 주문 요청입니다. ID: %d", id));
    }
}
