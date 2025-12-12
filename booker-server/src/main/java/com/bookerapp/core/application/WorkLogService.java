package com.bookerapp.core.application;

import com.bookerapp.core.domain.model.WorkLog;
import com.bookerapp.core.domain.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final WorkLogRepository workLogRepository;

    public WorkLog createLog(String title, String content, String author) {
        WorkLog log = WorkLog.builder()
                .title(title)
                .content(content)
                .author(author)
                .build();
        return workLogRepository.save(log);
    }

    public List<WorkLog> getAllLogs() {
        return workLogRepository.findAll();
    }

    public WorkLog getLog(String id) {
        return workLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));
    }
}
