package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.dto.BookLoanDto;
import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLoan;
import com.bookerapp.core.domain.model.entity.BookStatus;
import com.bookerapp.core.domain.model.entity.LoanStatus;
import com.bookerapp.core.domain.repository.BookLoanRepository;
import com.bookerapp.core.domain.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookLoanServiceTest {

    @Mock
    private BookLoanRepository bookLoanRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookLoanService bookLoanService;

    private final String MEMBER_ID = "test-user-id";
    private final Long BOOK_ID = 1L;
    private final Long LOAN_ID = 1L;

    private BookLoanDto.Request createLoanRequest;

    @BeforeEach
    void setUp() {
        createLoanRequest = new BookLoanDto.Request();
        createLoanRequest.setBookId(BOOK_ID);
    }

    @Test
    void createLoan_성공() {
        // given
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);

        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(bookLoanRepository.existsByBookIdAndStatusIn(any(), any())).willReturn(false);
        given(bookLoanRepository.save(any(BookLoan.class))).will(invocation -> invocation.getArgument(0));

        // when
        BookLoanDto.Response response = bookLoanService.createLoan(MEMBER_ID, createLoanRequest);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getBookId()).isEqualTo(BOOK_ID);
                    assertThat(r.getMemberId()).isEqualTo(MEMBER_ID);
                    assertThat(r.getStatus()).isEqualTo(LoanStatus.ACTIVE);
                });
        verify(bookLoanRepository).save(any(BookLoan.class));
    }

    @Test
    void createLoan_도서_없음_실패() {
        // given
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookLoanService.createLoan(MEMBER_ID, createLoanRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("도서를 찾을 수 없습니다");
    }

    @Test
    void createLoan_이미_대출중_대기목록_등록() {
        // given
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);

        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(bookLoanRepository.existsByBookIdAndStatusIn(
                eq(BOOK_ID),
                eq(Arrays.asList(LoanStatus.ACTIVE, LoanStatus.PENDING))
        )).willReturn(true);
        given(bookLoanRepository.save(any(BookLoan.class))).will(invocation -> invocation.getArgument(0));

        // when
        BookLoanDto.Response response = bookLoanService.createLoan(MEMBER_ID, createLoanRequest);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getBookId()).isEqualTo(BOOK_ID);
                    assertThat(r.getMemberId()).isEqualTo(MEMBER_ID);
                    assertThat(r.getStatus()).isEqualTo(LoanStatus.WAITING);
                });
        verify(bookLoanRepository).save(any(BookLoan.class));
    }

    @Test
    void createLoan_대기목록_있어도_신청가능() {
        // given
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);

        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(book));
        given(bookLoanRepository.existsByBookIdAndStatusIn(
                eq(BOOK_ID),
                eq(Arrays.asList(LoanStatus.ACTIVE, LoanStatus.PENDING))
        )).willReturn(true);
        given(bookLoanRepository.save(any(BookLoan.class))).will(invocation -> invocation.getArgument(0));

        // when
        BookLoanDto.Response response = bookLoanService.createLoan("other-user", createLoanRequest);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getBookId()).isEqualTo(BOOK_ID);
                    assertThat(r.getMemberId()).isEqualTo("other-user");
                    assertThat(r.getStatus()).isEqualTo(LoanStatus.WAITING);
                });
        verify(bookLoanRepository).save(any(BookLoan.class));
    }

    @Test
    void returnBook_성공() {
        // given
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);

        BookLoan bookLoan = mock(BookLoan.class);
        when(bookLoan.getId()).thenReturn(LOAN_ID);
        when(bookLoan.getBook()).thenReturn(book);
        when(bookLoan.getMemberId()).thenReturn(MEMBER_ID);
        when(bookLoan.getStatus()).thenReturn(LoanStatus.RETURNED);
        when(bookLoan.getReturnDate()).thenReturn(LocalDateTime.now());

        given(bookLoanRepository.findById(LOAN_ID)).willReturn(Optional.of(bookLoan));
        given(bookLoanRepository.save(any(BookLoan.class))).willReturn(bookLoan);
        given(bookLoanRepository.findWaitingListByBookId(BOOK_ID, LoanStatus.WAITING))
                .willReturn(Collections.emptyList());

        // when
        BookLoanDto.Response response = bookLoanService.returnBook(MEMBER_ID, LOAN_ID);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getStatus()).isEqualTo(LoanStatus.RETURNED);
                    assertThat(r.getReturnDate()).isNotNull();
                });
        verify(bookLoanRepository).save(any(BookLoan.class));
    }

    @Test
    void returnBook_권한없음_실패() {
        // given
        BookLoan bookLoan = mock(BookLoan.class);
        when(bookLoan.getMemberId()).thenReturn(MEMBER_ID);

        given(bookLoanRepository.findById(LOAN_ID)).willReturn(Optional.of(bookLoan));

        // when & then
        assertThatThrownBy(() -> bookLoanService.returnBook("other-user", LOAN_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인의 대출 기록만 반납할 수 있습니다");
    }

    @Test
    void extendLoan_성공() {
        // given
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);

        BookLoan bookLoan = mock(BookLoan.class);
        when(bookLoan.getId()).thenReturn(LOAN_ID);
        when(bookLoan.getBook()).thenReturn(book);
        when(bookLoan.getMemberId()).thenReturn(MEMBER_ID);

        given(bookLoanRepository.findById(LOAN_ID)).willReturn(Optional.of(bookLoan));
        given(bookLoanRepository.countWaitingListByBookId(BOOK_ID, LoanStatus.WAITING)).willReturn(0L);
        given(bookLoanRepository.save(any(BookLoan.class))).willReturn(bookLoan);

        // when
        BookLoanDto.Response response = bookLoanService.extendLoan(MEMBER_ID, LOAN_ID);

        // then
        assertThat(response).isNotNull();
        verify(bookLoanRepository).save(any(BookLoan.class));
    }

    @Test
    void extendLoan_대기자있음_실패() {
        // given
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);

        BookLoan bookLoan = mock(BookLoan.class);
        when(bookLoan.getMemberId()).thenReturn(MEMBER_ID);
        when(bookLoan.getBook()).thenReturn(book);

        given(bookLoanRepository.findById(LOAN_ID)).willReturn(Optional.of(bookLoan));
        given(bookLoanRepository.countWaitingListByBookId(BOOK_ID, LoanStatus.WAITING)).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> bookLoanService.extendLoan(MEMBER_ID, LOAN_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대기자가 있는 도서는 연장할 수 없습니다");
    }

    @Test
    void getMyLoans_성공() {
        // given
        BookLoanDto.SearchRequest request = new BookLoanDto.SearchRequest();
        request.setPage(0);
        request.setSize(10);

        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);

        BookLoan bookLoan = mock(BookLoan.class);
        when(bookLoan.getId()).thenReturn(LOAN_ID);
        when(bookLoan.getBook()).thenReturn(book);
        when(bookLoan.getMemberId()).thenReturn(MEMBER_ID);

        Page<BookLoan> loanPage = new PageImpl<>(Collections.singletonList(bookLoan));
        given(bookLoanRepository.findByMemberIdAndStatusIn(
                eq(MEMBER_ID),
                any(),
                any(PageRequest.class)
        )).willReturn(loanPage);

        // when
        Page<BookLoanDto.Response> response = bookLoanService.getMyLoans(MEMBER_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent())
                .hasSize(1)
                .first()
                .satisfies(loan -> {
                    assertThat(loan.getBookId()).isEqualTo(BOOK_ID);
                    assertThat(loan.getMemberId()).isEqualTo(MEMBER_ID);
                });
    }

    @Test
    void getLoan_성공() {
        // given
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);

        BookLoan bookLoan = mock(BookLoan.class);
        when(bookLoan.getId()).thenReturn(LOAN_ID);
        when(bookLoan.getBook()).thenReturn(book);
        when(bookLoan.getMemberId()).thenReturn(MEMBER_ID);

        given(bookLoanRepository.findById(LOAN_ID)).willReturn(Optional.of(bookLoan));

        // when
        BookLoanDto.Response response = bookLoanService.getLoan(MEMBER_ID, LOAN_ID);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getId()).isEqualTo(LOAN_ID);
                    assertThat(r.getBookId()).isEqualTo(BOOK_ID);
                    assertThat(r.getMemberId()).isEqualTo(MEMBER_ID);
                });
    }

    @Test
    void getLoan_권한없음_실패() {
        // given
        BookLoan bookLoan = mock(BookLoan.class);
        when(bookLoan.getMemberId()).thenReturn(MEMBER_ID);

        given(bookLoanRepository.findById(LOAN_ID)).willReturn(Optional.of(bookLoan));

        // when & then
        assertThatThrownBy(() -> bookLoanService.getLoan("other-user", LOAN_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인의 대출 기록만 조회할 수 있습니다");
    }
} 