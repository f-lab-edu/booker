package com.bookerapp.core.infrastructure.repository;

import com.bookerapp.core.domain.model.entity.BookOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookOrderRepository extends JpaRepository<BookOrder, Long> {

    List<BookOrder> findByRequesterIdAndIsDeletedFalse(String requesterId);

    List<BookOrder> findByStatusAndIsDeletedFalse(BookOrder.BookOrderStatus status);

    @Query("SELECT bo FROM BookOrder bo WHERE bo.isDeleted = false ORDER BY bo.createdAt DESC")
    List<BookOrder> findAllByIsDeletedFalse();

    @Query("SELECT bo FROM BookOrder bo WHERE bo.isDeleted = false AND bo.status = :status ORDER BY bo.createdAt DESC")
    List<BookOrder> findByStatusOrderByCreatedAtDesc(@Param("status") BookOrder.BookOrderStatus status);
}
