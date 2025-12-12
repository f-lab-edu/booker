package com.bookerapp.core.infrastructure.repository;

import com.bookerapp.core.domain.model.WorkLog;
import com.bookerapp.core.domain.model.WorkLogTag;
import com.bookerapp.core.domain.repository.WorkLogRepository;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class FileWorkLogRepository implements WorkLogRepository {

    private final Path rootLocation = Paths.get("work-logs");

    public FileWorkLogRepository() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @Override
    public WorkLog save(WorkLog workLog) {
        if (workLog.getCreatedAt() == null) {
            workLog.setCreatedAt(LocalDateTime.now());
        }
        
        // simple filename strategy: {timestamp}-{title}.md
        // sanitize title
        String safeTitle = workLog.getTitle().replaceAll("[^a-zA-Z0-9가-힣]", "_");
        String timestamp = workLog.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = timestamp + "-" + safeTitle + ".md";
        
        if (workLog.getId() == null) {
             workLog.setId(filename.replace(".md", ""));
        } else {
             // updating existing file? For now, we mainly support creating new logs.
             // If ID exists, we might overwrite, but let's stick to CREATE mostly.
             filename = workLog.getId() + ".md";
        }

        try {
            Path destinationFile = this.rootLocation.resolve(Paths.get(filename))
                    .normalize().toAbsolutePath();
            
            // Security check
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }
            
            // Write content
            // We'll write a simple header + content
            String tagsString = workLog.getTags().stream().map(Enum::name).collect(Collectors.joining(","));
            String fileContent = "---" + System.lineSeparator() +
                                 "title: " + workLog.getTitle() + System.lineSeparator() +
                                 "author: " + workLog.getAuthor() + System.lineSeparator() +
                                 "date: " + workLog.getCreatedAt().toString() + System.lineSeparator() +
                                 "tags: " + tagsString + System.lineSeparator() +
                                 "---" + System.lineSeparator() + System.lineSeparator() +
                                 workLog.getContent();
                                 
            Files.write(destinationFile, fileContent.getBytes());
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
        
        return workLog;
    }

    @Override
    public List<WorkLog> findAll() {
        try (Stream<Path> stream = Files.walk(this.rootLocation, 1)) {
            return stream.filter(path -> !path.equals(this.rootLocation) && path.toString().endsWith(".md"))
                .map(this::mapPathToWorkLog)
                .sorted(Comparator.comparing(WorkLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored files", e);
        }
    }
    
    @Override
    public Optional<WorkLog> findById(String id) {
        Path file = rootLocation.resolve(id + ".md");
        if (Files.exists(file)) {
            return Optional.of(mapPathToWorkLog(file));
        }
        return Optional.empty();
    }
    
    private WorkLog mapPathToWorkLog(Path path) {
        try {
            String filename = path.getFileName().toString().replace(".md", "");
            List<String> lines = Files.readAllLines(path);
            
            // Very basic parsing of the FrontMatter we wrote
            String title = "Untitled";
            String author = "Unknown";
            LocalDateTime createdAt = LocalDateTime.now();
            List<WorkLogTag> tags = new ArrayList<>();
            StringBuilder content = new StringBuilder();
            
            boolean inHeader = false;
            boolean headerParsed = false;
            
            for (String line : lines) {
                if (line.trim().equals("---")) {
                    if (!inHeader && !headerParsed) {
                        inHeader = true;
                        continue;
                    } else if (inHeader) {
                        inHeader = false;
                        headerParsed = true;
                        continue;
                    }
                }
                
                if (inHeader) {
                    if (line.startsWith("title:")) title = line.substring(6).trim();
                    else if (line.startsWith("author:")) author = line.substring(7).trim();
                    else if (line.startsWith("date:")) {
                         try {
                            createdAt = LocalDateTime.parse(line.substring(5).trim());
                         } catch (Exception e) {
                             // ignore parse error
                         }
                    } else if (line.startsWith("tags:")) {
                        String tagsStr = line.substring(5).trim();
                        if (!tagsStr.isEmpty()) {
                            for (String tag : tagsStr.split(",")) {
                                try {
                                    tags.add(WorkLogTag.valueOf(tag.trim()));
                                } catch (IllegalArgumentException e) {
                                    // ignore invalid tags
                                }
                            }
                        }
                    }
                } else {
                    content.append(line).append(System.lineSeparator());
                }
            }
            
            return WorkLog.builder()
                    .id(filename)
                    .title(title)
                    .author(author)
                    .createdAt(createdAt)
                    .tags(tags)
                    .content(content.toString()) // This might contain extra newlines at start
                    .build();
                    
        } catch (IOException e) {
             return WorkLog.builder().id(path.getFileName().toString()).title("Error reading file").build();
        }
    }
}
