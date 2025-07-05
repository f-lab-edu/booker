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
    
    @Query("SELECT bl FROM BookLoan bl WHERE bl.memberId = :memberId AND bl.status IN :statuses")
    Page<BookLoan> findByMemberIdAndStatusIn(
            @Param("memberId") String memberId,
            @Param("statuses") List<LoanStatus> statuses,
            Pageable pageable
    );
    
    @Query("SELECT bl FROM BookLoan bl WHERE bl.book.id = :bookId AND bl.status = :status")
    Optional<BookLoan> findByBookIdAndStatus(
            @Param("bookId") Long bookId,
            @Param("status") LoanStatus status
    );
    
    @Query("SELECT COUNT(bl) > 0 FROM BookLoan bl WHERE bl.book.id = :bookId AND bl.status IN :statuses")
    boolean existsByBookIdAndStatusIn(
            @Param("bookId") Long bookId,
            @Param("statuses") List<LoanStatus> statuses
    );
    
    List<BookLoan> findByStatus(LoanStatus status);

    @Query("SELECT bl FROM BookLoan bl WHERE bl.book.id = :bookId AND bl.status = :status ORDER BY bl.createdAt")
    List<BookLoan> findWaitingListByBookId(
            @Param("bookId") Long bookId,
            @Param("status") LoanStatus status
    );

    @Query("SELECT COUNT(bl) FROM BookLoan bl WHERE bl.book.id = :bookId AND bl.status = :status")
    long countWaitingListByBookId(
            @Param("bookId") Long bookId,
            @Param("status") LoanStatus status
    );
} 