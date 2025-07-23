package com.bookerapp.core.infrastructure.repository;

import com.bookerapp.core.domain.model.entity.BookOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookOrderRepository extends JpaRepository<BookOrder, Long> {

    List<BookOrder> findByRequesterId(String requesterId);

    List<BookOrder> findByStatus(BookOrder.BookOrderStatus status);

    @Query("SELECT bo FROM BookOrder bo ORDER BY bo.createdAt DESC")
    List<BookOrder> findAll();

    @Query("SELECT bo FROM BookOrder bo WHERE bo.status = :status ORDER BY bo.createdAt DESC")
    List<BookOrder> findByStatusOrderByCreatedAtDesc(@Param("status") BookOrder.BookOrderStatus status);
}
