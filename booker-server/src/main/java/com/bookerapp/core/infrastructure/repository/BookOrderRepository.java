package com.bookerapp.core.infrastructure.repository;

import com.bookerapp.core.domain.model.entity.BookOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookOrderRepository extends JpaRepository<BookOrder, Long> {

    Page<BookOrder> findByRequesterIdOrderByCreatedAtDesc(String requesterId, Pageable pageable);
    List<BookOrder> findByStatus(BookOrder.BookOrderStatus status);
    Page<BookOrder> findByStatusOrderByCreatedAtDesc(BookOrder.BookOrderStatus status, Pageable pageable);
}
