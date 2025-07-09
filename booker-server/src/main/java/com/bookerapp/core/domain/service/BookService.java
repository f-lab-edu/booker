package com.bookerapp.core.domain.service;

import com.bookerapp.core.domain.model.dto.BookDto;
import com.bookerapp.core.domain.model.entity.Book;
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
    public BookDto.Response createBook(BookDto.Request request) {
        if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 ISBN입니다: " + request.getIsbn());
        }
        Book book = request.toEntity();
        Book savedBook = bookRepository.save(book);
        return BookDto.Response.from(savedBook);
    }

    public BookDto.Response getBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("도서를 찾을 수 없습니다: " + id));
        return BookDto.Response.from(book);
    }

    @Transactional(readOnly = true)
    public Page<BookDto.Response> searchBooks(BookDto.SearchRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
        return bookRepository.searchBooks(
                request.getTitle(),
                request.getAuthor(),
                request.getStatus(),
                pageRequest
        ).map(BookDto.Response::from);
    }

    @Transactional
    public BookDto.Response updateBook(Long id, BookDto.Request request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("도서를 찾을 수 없습니다: " + id));

        if (request.getIsbn() != null &&
                bookRepository.findByIsbn(request.getIsbn())
                        .filter(existingBook -> !existingBook.getId().equals(id))
                        .isPresent()) {
            throw new IllegalArgumentException("이미 등록된 ISBN입니다: " + request.getIsbn());
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setLocation(request.getLocation());

        Book updatedBook = bookRepository.save(book);
        return BookDto.Response.from(updatedBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("도서를 찾을 수 없습니다: " + id));
        book.markAsDeleted();
        bookRepository.save(book);
    }
}
