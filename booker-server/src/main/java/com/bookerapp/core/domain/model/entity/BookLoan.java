package com.bookerapp.core.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_loans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookLoan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Book book;

    @Column(nullable = false)
    private String memberId;

    @Column
    private LocalDateTime loanDate;

    @Column
    private LocalDateTime dueDate;

    @Column
    private LocalDateTime returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.PENDING;

    private final int DEFAULT_LOAN_DURATION = 2;
    private final int EXTEND_DURATION = 1;
    private final int WARNING_DUE_DAY = 3;

    public BookLoan(Book book, String memberId) {
        this.book = book;
        this.memberId = memberId;
        this.status = LoanStatus.PENDING;
    }

    public void processLoan() {
        if (status != LoanStatus.PENDING) {
            throw new IllegalStateException("대출 처리는 PENDING 상태에서만 가능합니다.");
        }
        this.status = LoanStatus.ACTIVE;
        this.loanDate = LocalDateTime.now();
        this.dueDate = loanDate.plusWeeks(DEFAULT_LOAN_DURATION);
        this.book.updateStatus(BookStatus.LOANED);
    }

    public void processReturn() {
        if (status != LoanStatus.ACTIVE && status != LoanStatus.OVERDUE) {
            throw new IllegalStateException("반납 처리는 ACTIVE 또는 OVERDUE 상태에서만 가능합니다.");
        }
        this.status = LoanStatus.RETURNED;
        this.returnDate = LocalDateTime.now();
        this.book.updateStatus(BookStatus.AVAILABLE);
    }

    public void extend() {
        if (status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("연장은 ACTIVE 상태에서만 가능합니다.");
        }
        checkAndUpdateOverdueStatus();
        if (isOverdue()) {
            throw new IllegalStateException("연체 중인 도서는 연장할 수 없습니다.");
        }
        this.dueDate = this.dueDate.plusWeeks(EXTEND_DURATION);
    }

    public boolean isOverdue() {
        return status == LoanStatus.OVERDUE ||
               (status == LoanStatus.ACTIVE && LocalDateTime.now().isAfter(dueDate));
    }

    public void checkAndUpdateOverdueStatus() {
        if (status == LoanStatus.ACTIVE && LocalDateTime.now().isAfter(dueDate)) {
            this.status = LoanStatus.OVERDUE;
        }
    }

    public void notifyDueDateApproaching() {
        if (status == LoanStatus.ACTIVE) {
            LocalDateTime warningDate = dueDate.minusDays(WARNING_DUE_DAY);
            if (LocalDateTime.now().isAfter(warningDate)) {
                // TODO: 알림 서비스 연동
            }
        }
    }
}
