package com.bookerapp.core.domain.repository;

import com.bookerapp.core.domain.model.WorkLog;
import java.util.List;
import java.util.Optional;

public interface WorkLogRepository {
    WorkLog save(WorkLog workLog);
    List<WorkLog> findAll();
    Optional<WorkLog> findById(String id);
}
