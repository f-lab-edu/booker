package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.exception.DuplicateIsbnException;
import com.bookerapp.core.domain.model.dto.BookDto;
import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLocation;
import com.bookerapp.core.domain.model.entity.BookStatus;
import com.bookerapp.core.domain.model.Floor;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private BookDto.Request createBookRequest;

    @BeforeEach
    void setUp() {
        createBookRequest = new BookDto.Request();
        createBookRequest.setTitle("테스트 도서");
        createBookRequest.setAuthor("테스트 저자");
        createBookRequest.setPublisher("테스트 출판사");
        createBookRequest.setIsbn("9788956746425");
        createBookRequest.setCoverImageUrl("http://example.com/cover.jpg");
        createBookRequest.setLocation(BookLocation.of(Floor.FOURTH));
    }

    @Test
    void createBook_성공() {
        // given
        Book mockBook = mock(Book.class);
        when(mockBook.getTitle()).thenReturn(createBookRequest.getTitle());
        when(mockBook.getPublisher()).thenReturn(createBookRequest.getPublisher());
        when(mockBook.getIsbn()).thenReturn(createBookRequest.getIsbn());
        when(mockBook.getCoverImageUrl()).thenReturn(createBookRequest.getCoverImageUrl());
        when(mockBook.getStatus()).thenReturn(BookStatus.AVAILABLE);
        when(mockBook.getLocation()).thenReturn(createBookRequest.getLocation());

        given(bookRepository.findByIsbn(createBookRequest.getIsbn())).willReturn(Optional.empty());
        given(bookRepository.save(any(Book.class))).willReturn(mockBook);

        // when
        BookDto.Response response = bookService.createBook(createBookRequest);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getTitle()).isEqualTo(createBookRequest.getTitle());
                    assertThat(r.getPublisher()).isEqualTo(createBookRequest.getPublisher());
                    assertThat(r.getIsbn()).isEqualTo(createBookRequest.getIsbn());
                    assertThat(r.getCoverImageUrl()).isEqualTo(createBookRequest.getCoverImageUrl());
                    assertThat(r.getStatus()).isEqualTo(BookStatus.AVAILABLE);
                    assertThat(r.getLocation().getFloor()).isEqualTo(Floor.FOURTH);
                });
    }

    @Test
    void createBook_중복된_ISBN_실패() {
        // given
        Book mockBook = mock(Book.class);
        given(bookRepository.findByIsbn(createBookRequest.getIsbn())).willReturn(Optional.of(mockBook));

        // when & then
        assertThatThrownBy(() -> bookService.createBook(createBookRequest))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining("이미 등록된 ISBN입니다");
    }

    @Test
    void getBook_성공() {
        // given
        Book mockBook = mock(Book.class);
        when(mockBook.getId()).thenReturn(1L);
        when(mockBook.getTitle()).thenReturn("테스트 도서");
        when(mockBook.getAuthor()).thenReturn("테스트 저자");
        when(mockBook.getLocation()).thenReturn(BookLocation.of(Floor.FOURTH));
        when(mockBook.getStatus()).thenReturn(BookStatus.AVAILABLE);

        given(bookRepository.findById(1L)).willReturn(Optional.of(mockBook));

        // when
        BookDto.Response response = bookService.getBook(1L);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getId()).isEqualTo(mockBook.getId());
                    assertThat(r.getTitle()).isEqualTo(mockBook.getTitle());
                    assertThat(r.getAuthor()).isEqualTo(mockBook.getAuthor());
                });
    }

    @Test
    void getBook_존재하지_않는_도서_실패() {
        // given
        given(bookRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookService.getBook(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("도서를 찾을 수 없습니다");
    }

    @Test
    void searchBooks_성공() {
        // given
        Book mockBook = mock(Book.class);
        when(mockBook.getTitle()).thenReturn("테스트 도서");
        when(mockBook.getAuthor()).thenReturn("테스트 저자");
        when(mockBook.getLocation()).thenReturn(BookLocation.of(Floor.FOURTH));
        when(mockBook.getStatus()).thenReturn(BookStatus.AVAILABLE);

        BookDto.SearchRequest searchRequest = new BookDto.SearchRequest();
        searchRequest.setTitle("테스트");
        searchRequest.setAuthor("저자");
        searchRequest.setStatus(BookStatus.AVAILABLE);
        searchRequest.setPage(0);
        searchRequest.setSize(10);

        Page<Book> bookPage = new PageImpl<>(List.of(mockBook));
        given(bookRepository.searchBooks(
                searchRequest.getTitle(),
                searchRequest.getAuthor(),
                searchRequest.getStatus(),
                PageRequest.of(searchRequest.getPage(), searchRequest.getSize())
        )).willReturn(bookPage);

        // when
        Page<BookDto.Response> responses = bookService.searchBooks(searchRequest);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getContent().get(0))
                .satisfies(r -> {
                    assertThat(r.getTitle()).isEqualTo(mockBook.getTitle());
                    assertThat(r.getAuthor()).isEqualTo(mockBook.getAuthor());
                });
    }

    @Test
    void updateBook_성공() {
        // given
        Book mockBook = mock(Book.class);
        when(mockBook.getTitle()).thenReturn("수정된 제목");
        when(mockBook.getAuthor()).thenReturn("수정된 저자");
        when(mockBook.getPublisher()).thenReturn("수정된 출판사");
        when(mockBook.getLocation()).thenReturn(BookLocation.of(Floor.TWELFTH));
        when(mockBook.getStatus()).thenReturn(BookStatus.AVAILABLE);

        given(bookRepository.findById(1L)).willReturn(Optional.of(mockBook));
        given(bookRepository.save(any(Book.class))).willReturn(mockBook);

        BookDto.Request updateRequest = new BookDto.Request();
        updateRequest.setTitle("수정된 제목");
        updateRequest.setAuthor("수정된 저자");
        updateRequest.setPublisher("수정된 출판사");
        updateRequest.setIsbn("9788956746425");
        updateRequest.setLocation(BookLocation.of(Floor.TWELFTH));

        // when
        BookDto.Response response = bookService.updateBook(1L, updateRequest);

        // then
        verify(mockBook).updateInformation(
                updateRequest.getTitle(),
                updateRequest.getAuthor(),
                updateRequest.getIsbn(),
                updateRequest.getPublisher(),
                updateRequest.getCoverImageUrl(),
                updateRequest.getLocation()
        );

        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getTitle()).isEqualTo(updateRequest.getTitle());
                    assertThat(r.getAuthor()).isEqualTo(updateRequest.getAuthor());
                    assertThat(r.getPublisher()).isEqualTo(updateRequest.getPublisher());
                    assertThat(r.getLocation().getFloor()).isEqualTo(Floor.TWELFTH);
                });
    }

    @Test
    void updateBook_존재하지_않는_도서_실패() {
        // given
        given(bookRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookService.updateBook(1L, createBookRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("도서를 찾을 수 없습니다");
    }

    @Test
    void updateBook_중복된_ISBN_실패() {
        // given
        Book mockBook = mock(Book.class);
        Book existingBook = mock(Book.class);
        when(existingBook.getId()).thenReturn(2L);

        given(bookRepository.findById(1L)).willReturn(Optional.of(mockBook));
        given(bookRepository.findByIsbn("9788956746999")).willReturn(Optional.of(existingBook));

        BookDto.Request updateRequest = new BookDto.Request();
        updateRequest.setTitle("수정된 제목");
        updateRequest.setAuthor("수정된 저자");
        updateRequest.setIsbn("9788956746999");

        // when & then
        assertThatThrownBy(() -> bookService.updateBook(1L, updateRequest))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining("이미 등록된 ISBN입니다");
    }

    @Test
    void deleteBook_성공() {
        // given
        Book mockBook = mock(Book.class);
        given(bookRepository.findById(1L)).willReturn(Optional.of(mockBook));

        // when
        bookService.deleteBook(1L);

        // then
        verify(mockBook).markAsDeleted();
        verify(bookRepository).save(mockBook);
    }

    @Test
    void deleteBook_존재하지_않는_도서_실패() {
        // given
        given(bookRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookService.deleteBook(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("도서를 찾을 수 없습니다");
    }
}
