package com.bookerapp.core.domain.repository;

import com.bookerapp.core.domain.model.entity.BookLoan;
import com.bookerapp.core.domain.model.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {

    Page<BookLoan> findByMemberIdAndStatusIn(String memberId, List<LoanStatus> statuses, Pageable pageable);

    Optional<BookLoan> findByBookIdAndStatus(Long bookId, LoanStatus status);

    boolean existsByBookIdAndStatusIn(Long bookId, List<LoanStatus> statuses);

    List<BookLoan> findByStatus(LoanStatus status);

    // TODO: QueryDSL로 이전 예정 - OrderBy 처리를 위해 현재는 @Query 사용
    @Query("SELECT bl FROM BookLoan bl WHERE bl.book.id = :bookId AND bl.status = :status ORDER BY bl.createdAt")
    List<BookLoan> findWaitingListByBookId(
            @Param("bookId") Long bookId,
            @Param("status") LoanStatus status
    );

    long countByBookIdAndStatus(Long bookId, LoanStatus status);

    default long countWaitingListByBookId(Long bookId, LoanStatus status) {
        return countByBookIdAndStatus(bookId, status);
    }
}
