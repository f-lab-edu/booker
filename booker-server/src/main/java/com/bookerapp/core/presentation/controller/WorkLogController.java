package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.WorkLogService;
import com.bookerapp.core.domain.model.WorkLog;
import com.bookerapp.core.domain.model.WorkLogTag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/work-logs")
@RequiredArgsConstructor
public class WorkLogController {

    private final WorkLogService workLogService;

    @PostMapping
    @Operation(summary = "Create a new work log")
    public ResponseEntity<WorkLog> createLog(@RequestBody CreateWorkLogRequest request) {
        WorkLog created = workLogService.createLog(request.getTitle(), request.getContent(), request.getAuthor(), request.getTags());
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
                
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List all work logs (optionally filtered by tags)")
    public ResponseEntity<List<WorkLog>> getAllLogs(@RequestParam(required = false) List<WorkLogTag> tags) {
        return ResponseEntity.ok(workLogService.getAllLogs(tags));
    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(summary = "Get raw markdown content of a log")
    public ResponseEntity<String> getLogContent(@PathVariable String id) {
        WorkLog log = workLogService.getLog(id);
        return ResponseEntity.ok(log.getContent());
    }

    @Data
    public static class CreateWorkLogRequest {
        private String title;
        private String content;
        private String author;
        private List<WorkLogTag> tags;
    }
}
