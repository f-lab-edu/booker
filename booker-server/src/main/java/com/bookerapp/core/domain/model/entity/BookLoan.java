package com.bookerapp.core.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_loans")
@Getter
@Setter
@NoArgsConstructor
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

    public void processLoan() {
        if (status != LoanStatus.PENDING) {
            throw new IllegalStateException("대출 처리는 PENDING 상태에서만 가능합니다.");
        }
        this.status = LoanStatus.ACTIVE;
        this.loanDate = LocalDateTime.now();
        this.dueDate = loanDate.plusWeeks(2); // 기본 대출 기간 2주
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
        if (isOverdue()) {
            throw new IllegalStateException("연체 중인 도서는 연장할 수 없습니다.");
        }
        this.dueDate = this.dueDate.plusWeeks(1); // 1주일 연장
    }

    public boolean isOverdue() {
        if (status == LoanStatus.ACTIVE && LocalDateTime.now().isAfter(dueDate)) {
            this.status = LoanStatus.OVERDUE;
            return true;
        }
        return status == LoanStatus.OVERDUE;
    }

    public void notifyDueDateApproaching() {
        if (status == LoanStatus.ACTIVE) {
            LocalDateTime warningDate = dueDate.minusDays(3);
            if (LocalDateTime.now().isAfter(warningDate)) {
                // TODO: 알림 서비스 연동
            }
        }
    }
}
