package com.bookerapp.core.domain.model.entity;

public enum BookStatus {
    AVAILABLE,    // 대출 가능
    LOANED,      // 대출 중
    RESERVED,    // 예약됨
    PROCESSING,  // 처리 중 (입고/등록 등)
    UNAVAILABLE  // 대출 불가 (분실/파손 등)
}
