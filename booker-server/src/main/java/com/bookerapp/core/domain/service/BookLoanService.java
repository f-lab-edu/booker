package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.dto.BookLoanDto;
import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLoan;
import com.bookerapp.core.domain.model.enums.LoanStatus;
import com.bookerapp.core.domain.repository.BookLoanRepository;
import com.bookerapp.core.domain.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookLoanService {

    private final BookLoanRepository bookLoanRepository;
    private final BookRepository bookRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookLoanDto.Response createLoan(String memberId, BookLoanDto.Request request) {
        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new EntityNotFoundException("도서를 찾을 수 없습니다: " + request.getBookId()));
        
        boolean isBookAvailable = !bookLoanRepository.existsByBookIdAndStatusIn(
            book.getId(), Arrays.asList(LoanStatus.ACTIVE, LoanStatus.PENDING));
        
        BookLoan loan = new BookLoan(book, memberId);
        
        if (isBookAvailable) {
            loan.processLoan();
        } else {
            loan.setStatus(LoanStatus.WAITING);
        }

        BookLoan savedLoan = bookLoanRepository.save(loan);
        BookLoanDto.Response response = BookLoanDto.Response.from(savedLoan);

        // WAITING 상태인 경우 대기 순서 계산
        if (savedLoan.getStatus() == LoanStatus.WAITING) {
            Integer position = bookLoanRepository.findWaitingPosition(
                savedLoan.getBook().getId(),
                savedLoan.getCreatedAt()
            );
            response.setWaitingPosition(position);
        }

        return response;
    }

    @Transactional
    public BookLoanDto.Response returnBook(String memberId, Long loanId) {
        BookLoan loan = bookLoanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("대출 기록을 찾을 수 없습니다: " + loanId));

        if (!loan.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 대출 기록만 반납할 수 있습니다.");
        }

        loan.processReturn();
        BookLoan savedLoan = bookLoanRepository.save(loan);

        List<BookLoan> waitingList = bookLoanRepository.findWaitingListByBookId(loan.getBook().getId(), LoanStatus.WAITING);
        if (!waitingList.isEmpty()) {
            BookLoan nextLoan = waitingList.get(0);
            // TODO: 대기자에게 알림 발송
            // notificationService.sendBookAvailableNotification(nextLoan.getMemberId(), loan.getBook());
        }

        return BookLoanDto.Response.from(savedLoan);
    }

    @Transactional
    public BookLoanDto.Response extendLoan(String memberId, Long loanId) {
        BookLoan loan = bookLoanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("대출 기록을 찾을 수 없습니다: " + loanId));

        if (!loan.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 대출 기록만 연장할 수 있습니다.");
        }

        long waitingCount = bookLoanRepository.countByBookIdAndStatus(loan.getBook().getId(), LoanStatus.WAITING);
        if (waitingCount > 0) {
            throw new IllegalStateException("대기자가 있는 도서는 연장할 수 없습니다.");
        }

        loan.extend();
        BookLoan savedLoan = bookLoanRepository.save(loan);
        return BookLoanDto.Response.from(savedLoan);
    }

    @Transactional(readOnly = true)
    public Page<BookLoanDto.Response> getMyLoans(String memberId, BookLoanDto.SearchRequest request) {
        List<LoanStatus> statuses = request.getStatuses() != null && !request.getStatuses().isEmpty()
                ? request.getStatuses()
                : Arrays.asList(LoanStatus.values());

        return bookLoanRepository.findByMemberIdAndStatusIn(
                memberId,
                statuses,
                PageRequest.of(request.getPage(), request.getSize())
        ).map(loan -> {
            BookLoanDto.Response response = BookLoanDto.Response.from(loan);
            // WAITING 상태인 경우 대기 순서 계산
            if (loan.getStatus() == LoanStatus.WAITING) {
                Integer position = bookLoanRepository.findWaitingPosition(
                    loan.getBook().getId(),
                    loan.getCreatedAt()
                );
                response.setWaitingPosition(position);
            }
            return response;
        });
    }

    @Transactional(readOnly = true)
    public BookLoanDto.Response getLoan(String memberId, Long loanId) {
        BookLoan loan = bookLoanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("대출 기록을 찾을 수 없습니다: " + loanId));

        if (!loan.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 대출 기록만 조회할 수 있습니다.");
        }

        BookLoanDto.Response response = BookLoanDto.Response.from(loan);

        // WAITING 상태인 경우 대기 순서 계산
        if (loan.getStatus() == LoanStatus.WAITING) {
            Integer position = bookLoanRepository.findWaitingPosition(
                loan.getBook().getId(),
                loan.getCreatedAt()
            );
            response.setWaitingPosition(position);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public long getWaitingCount(Long bookId) {
        return bookLoanRepository.countByBookIdAndStatus(bookId, LoanStatus.WAITING);
    }

    @Transactional
    public void checkAndUpdateOverdueStatus() {
        List<BookLoan> activeLoans = bookLoanRepository.findByStatus(LoanStatus.ACTIVE);
        for (BookLoan loan : activeLoans) {
            loan.checkAndUpdateOverdueStatus();
            if (loan.isOverdue()) {
                bookLoanRepository.save(loan);
            }
        }
    }
}
