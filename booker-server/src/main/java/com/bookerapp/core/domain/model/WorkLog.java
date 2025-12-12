package com.bookerapp.core.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLog {
    private String id; // filename without extension
    private String title;
    private String content; // markdown
    private String author;
    private LocalDateTime createdAt;
}
