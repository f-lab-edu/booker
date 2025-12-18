package com.bookerapp.core.application;

import com.bookerapp.core.domain.exception.WorkLogNotFoundException;
import com.bookerapp.core.domain.model.WorkLog;
import com.bookerapp.core.domain.model.WorkLogTag;
import com.bookerapp.core.domain.model.dto.WorkLogDto;
import com.bookerapp.core.domain.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final WorkLogRepository workLogRepository;

    public WorkLogDto.Response createLog(String title, String content, String author, List<WorkLogTag> tags) {
        WorkLog log = WorkLog.builder()
                .title(title)
                .content(content)
                .author(author)
                .tags(tags != null ? tags : Collections.emptyList())
                .build();
        WorkLog saved = workLogRepository.save(log);
        return WorkLogDto.Response.from(saved);
    }

    public Page<WorkLogDto.SummaryResponse> getAllLogs(List<WorkLogTag> filterTags, Pageable pageable) {
        List<WorkLog> allLogs = workLogRepository.findAll();

        List<WorkLog> filteredLogs;
        if (filterTags == null || filterTags.isEmpty()) {
            filteredLogs = allLogs;
        } else {
            filteredLogs = allLogs.stream()
                    .filter(log -> log.getTags() != null && !Collections.disjoint(log.getTags(), filterTags))
                    .collect(Collectors.toList());
        }

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredLogs.size());

        List<WorkLogDto.SummaryResponse> content = filteredLogs.subList(start, end).stream()
                .map(WorkLogDto.SummaryResponse::from)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                content,
                pageable,
                filteredLogs.size()
        );
    }

    public WorkLogDto.Response getLog(String id) {
        WorkLog log = workLogRepository.findById(id)
                .orElseThrow(() -> new WorkLogNotFoundException(id));
        return WorkLogDto.Response.from(log);
    }

    public String getLogContent(String id) {
        WorkLog log = workLogRepository.findById(id)
                .orElseThrow(() -> new WorkLogNotFoundException(id));
        return log.getContent();
    }
}
