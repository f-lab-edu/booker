package com.bookerapp.core.domain.model.enums;

public enum LoanStatus {
    PENDING,     // 대출 신청됨
    WAITING,     // 대기 리스트에 있음
    ACTIVE,      // 대출 중
    OVERDUE,     // 연체
    RETURNED,    // 반납 완료
    CANCELLED    // 취소됨
}
