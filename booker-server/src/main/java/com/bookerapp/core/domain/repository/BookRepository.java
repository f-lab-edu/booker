package com.bookerapp.core.domain.repository;

import com.bookerapp.core.domain.model.entity.Book;
import com.bookerapp.core.domain.model.entity.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    
    Optional<Book> findByIsbn(String isbn);
    
    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR b.title LIKE %:title%) AND " +
           "(:author IS NULL OR b.author LIKE %:author%) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "b.isDeleted = false")
    Page<Book> searchBooks(
            @Param("title") String title,
            @Param("author") String author,
            @Param("status") BookStatus status,
            Pageable pageable
    );
    
    List<Book> findByStatusAndIsDeletedFalse(BookStatus status);
    
    @Query("SELECT COUNT(b) FROM Book b WHERE b.status = :status AND b.isDeleted = false")
    long countByStatus(@Param("status") BookStatus status);
} 