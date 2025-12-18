package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.exception.DuplicateIsbnException;
import com.bookerapp.core.domain.model.dto.BookDto;
import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookLocation;
import com.bookerapp.core.domain.model.enums.Floor;
import com.bookerapp.core.domain.model.auth.UserContext;
import com.bookerapp.core.domain.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional
    public BookDto.Response createBook(BookDto.Request request, UserContext userContext) {
        try {
            if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
                throw new DuplicateIsbnException(request.getIsbn());
            }
            Book book = request.toEntity();
            Book savedBook = bookRepository.save(book);
            return BookDto.Response.from(savedBook);
        } catch (Exception e) {
            System.err.println("=== BookService.createBook 에러 발생 ===");
            System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Exception message: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public BookDto.Response getBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("도서를 찾을 수 없습니다: " + id));
        return BookDto.Response.from(book);
    }

    @Transactional(readOnly = true)
    public Page<BookDto.Response> searchBooks(BookDto.SearchRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
        return bookRepository.searchBooks(request.getTitle(), request.getAuthor(), request.getStatus(), pageRequest)
                .map(BookDto.Response::from);
    }

    @Transactional
    public BookDto.Response updateBook(Long id, BookDto.Request request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("도서를 찾을 수 없습니다: " + id));

        // ISBN이 변경되는 경우에만 중복 체크
        if (!book.getIsbn().equals(request.getIsbn())) {
            bookRepository.findByIsbn(request.getIsbn()).ifPresent(existingBook -> {
                throw new DuplicateIsbnException(
                    String.format("이미 등록된 ISBN입니다: %s (도서 ID: %d, 제목: %s)",
                        request.getIsbn(), existingBook.getId(), existingBook.getTitle())
                );
            });
        }

        BookLocation bookLocation = null;
        if (request.getLocation() != null) {
            bookLocation = BookLocation.of(Floor.valueOf(request.getLocation().getFloor()));
            bookLocation.setSection(request.getLocation().getSection());
            bookLocation.setShelf(request.getLocation().getShelf());
        }

        book.updateInformation(
            request.getTitle(),
            request.getAuthor(),
            request.getIsbn(),
            request.getPublisher(),
            request.getCoverImageUrl(),
            bookLocation
        );

        Book updatedBook = bookRepository.save(book);
        return BookDto.Response.from(updatedBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("도서를 찾을 수 없습니다: " + id));
        bookRepository.delete(book);
    }
}
