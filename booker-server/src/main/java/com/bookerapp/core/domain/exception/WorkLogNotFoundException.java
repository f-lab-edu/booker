package com.bookerapp.core.domain.exception;

public class WorkLogNotFoundException extends RuntimeException {
    public WorkLogNotFoundException(String id) {
        super(String.format("작업 로그를 찾을 수 없습니다. ID: %s", id));
    }
}
