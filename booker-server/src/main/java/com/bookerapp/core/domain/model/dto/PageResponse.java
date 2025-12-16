package com.bookerapp.core.domain.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Schema(name = "PageResponse", description = "페이징 응답")
public class PageResponse<T> {

    @Schema(description = "데이터 목록")
    private final List<T> content;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private final int page;

    @Schema(description = "페이지 크기", example = "20")
    private final int size;

    @Schema(description = "전체 데이터 개수", example = "100")
    private final long totalElements;

    @Schema(description = "전체 페이지 수", example = "5")
    private final int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private final boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private final boolean last;

    private PageResponse(List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
