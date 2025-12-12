package com.bookerapp.core.application;

import com.bookerapp.core.domain.model.WorkLog;
import com.bookerapp.core.domain.model.WorkLogTag;
import com.bookerapp.core.domain.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final WorkLogRepository workLogRepository;

    public WorkLog createLog(String title, String content, String author, List<WorkLogTag> tags) {
        WorkLog log = WorkLog.builder()
                .title(title)
                .content(content)
                .author(author)
                .tags(tags != null ? tags : Collections.emptyList())
                .build();
        return workLogRepository.save(log);
    }

    public List<WorkLog> getAllLogs(List<WorkLogTag> filterTags) {
        List<WorkLog> allLogs = workLogRepository.findAll();
        
        if (filterTags == null || filterTags.isEmpty()) {
            return allLogs;
        }
        
        return allLogs.stream()
                .filter(log -> log.getTags() != null && !Collections.disjoint(log.getTags(), filterTags))
                .collect(Collectors.toList());
    }

    public WorkLog getLog(String id) {
        return workLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));
    }
}
