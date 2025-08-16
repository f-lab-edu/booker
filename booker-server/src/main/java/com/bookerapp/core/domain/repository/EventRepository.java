package com.bookerapp.core.domain.repository;

import com.bookerapp.core.domain.model.event.Event;
import com.bookerapp.core.domain.model.event.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByType(EventType type, Pageable pageable);
    Page<Event> findAll(Pageable pageable);
} 