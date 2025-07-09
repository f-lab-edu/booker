package com.bookerapp.core.domain.service;

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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private BookDto.Request createBookRequest;
    private Book book;

    @BeforeEach
    void setUp() {
        createBookRequest = new BookDto.Request();
        createBookRequest.setTitle("테스트 도서");
        createBookRequest.setAuthor("테스트 저자");
        createBookRequest.setPublisher("테스트 출판사");
        createBookRequest.setIsbn("9788956746425");
        createBookRequest.setCoverImageUrl("http://example.com/cover.jpg");
        createBookRequest.setLocation(BookLocation.of(Floor.FOURTH));

        book = createBookRequest.toEntity();
        book.setId(1L);
    }

    @Test
    void createBook_성공() {
        // given
        given(bookRepository.findByIsbn(createBookRequest.getIsbn())).willReturn(Optional.empty());
        given(bookRepository.save(any(Book.class))).willReturn(book);

        // when
        BookDto.Response response = bookService.createBook(createBookRequest);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getTitle()).isEqualTo(createBookRequest.getTitle());
                    assertThat(r.getAuthor()).isEqualTo(createBookRequest.getAuthor());
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
        given(bookRepository.findByIsbn(createBookRequest.getIsbn())).willReturn(Optional.of(book));

        // when & then
        assertThatThrownBy(() -> bookService.createBook(createBookRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된 ISBN입니다");
    }

    @Test
    void getBook_성공() {
        // given
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));

        // when
        BookDto.Response response = bookService.getBook(1L);

        // then
        assertThat(response)
                .isNotNull()
                .satisfies(r -> {
                    assertThat(r.getId()).isEqualTo(book.getId());
                    assertThat(r.getTitle()).isEqualTo(book.getTitle());
                    assertThat(r.getAuthor()).isEqualTo(book.getAuthor());
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
        BookDto.SearchRequest searchRequest = new BookDto.SearchRequest();
        searchRequest.setTitle("테스트");
        searchRequest.setAuthor("저자");
        searchRequest.setStatus(BookStatus.AVAILABLE);
        searchRequest.setPage(0);
        searchRequest.setSize(10);

        Page<Book> bookPage = new PageImpl<>(List.of(book));
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
                    assertThat(r.getTitle()).isEqualTo(book.getTitle());
                    assertThat(r.getAuthor()).isEqualTo(book.getAuthor());
                });
    }

    @Test
    void updateBook_성공() {
        // given
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookRepository.save(any(Book.class))).willReturn(book);

        BookDto.Request updateRequest = new BookDto.Request();
        updateRequest.setTitle("수정된 제목");
        updateRequest.setAuthor("수정된 저자");
        updateRequest.setPublisher("수정된 출판사");
        updateRequest.setIsbn("9788956746425");
        updateRequest.setLocation(BookLocation.of(Floor.TWELFTH));

        // when
        BookDto.Response response = bookService.updateBook(1L, updateRequest);

        // then
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
        Book existingBook = new Book();
        existingBook.setId(2L);
        existingBook.setIsbn("9788956746999");

        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookRepository.findByIsbn("9788956746999")).willReturn(Optional.of(existingBook));

        BookDto.Request updateRequest = new BookDto.Request();
        updateRequest.setIsbn("9788956746999");

        // when & then
        assertThatThrownBy(() -> bookService.updateBook(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된 ISBN입니다");
    }

    @Test
    void deleteBook_성공() {
        // given
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));

        // when
        bookService.deleteBook(1L);

        // then
        verify(bookRepository).save(book);
        assertThat(book.isDeleted()).isTrue();
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
