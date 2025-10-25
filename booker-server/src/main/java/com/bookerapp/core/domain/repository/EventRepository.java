package com.bookerapp.core.domain.repository;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByType(EventType type, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Event> findWithPessimisticLockById(Long id);
}
