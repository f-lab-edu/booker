package com.bookerapp.core.domain.model.dto;

import com.bookerapp.core.domain.model.WorkLog;
import com.bookerapp.core.domain.model.WorkLogTag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class WorkLogDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(name = "CreateWorkLogRequest", description = "작업 로그 생성 요청")
    public static class CreateRequest {
        @Schema(description = "작업 로그 제목", example = "Spring Boot API 개발", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "제목은 필수입니다")
        private String title;

        @Schema(description = "Markdown 형식의 본문 내용", example = "# 작업 내용\n\n- API 엔드포인트 추가\n- Swagger 문서화\n- 테스트 코드 작성", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "내용은 필수입니다")
        private String content;

        @Schema(description = "작성자 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "작성자는 필수입니다")
        private String author;

        @Schema(description = "태그 목록 (DEVELOPMENT, MEETING, LEARNING, BUG_FIX, DEPLOYMENT)", example = "[\"DEVELOPMENT\", \"API\"]")
        private List<WorkLogTag> tags;
    }

    @Getter
    @NoArgsConstructor
    @Schema(name = "WorkLogResponse", description = "작업 로그 상세 응답 - 전체 내용 포함")
    public static class Response {
        @Schema(description = "작업 로그 고유 ID", example = "20251217-103000-abc123")
        private String id;

        @Schema(description = "작업 로그 제목", example = "Spring Boot API 개발")
        private String title;

        @Schema(description = "Markdown 형식의 전체 본문 내용", example = "# 작업 내용\n\n- API 엔드포인트 추가\n- Swagger 문서화")
        private String content;

        @Schema(description = "작성자 이름", example = "홍길동")
        private String author;

        @Schema(description = "작성 일시", example = "2025-12-17T10:30:00")
        private LocalDateTime createdAt;

        @Schema(description = "태그 목록", example = "[\"DEVELOPMENT\", \"API\"]")
        private List<WorkLogTag> tags;

        @Schema(description = "본문 내용 길이 (글자 수)", example = "1234")
        private Integer contentLength;

        public static Response from(WorkLog workLog) {
            Response response = new Response();
            response.id = workLog.getId();
            response.title = workLog.getTitle();
            response.content = workLog.getContent();
            response.author = workLog.getAuthor();
            response.createdAt = workLog.getCreatedAt();
            response.tags = workLog.getTags();
            response.contentLength = workLog.getContent() != null ? workLog.getContent().length() : 0;
            return response;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(name = "WorkLogSummaryResponse", description = "작업 로그 요약 응답 - 목록 조회용")
    public static class SummaryResponse {
        @Schema(description = "작업 로그 고유 ID", example = "20251217-103000-abc123")
        private String id;

        @Schema(description = "작업 로그 제목", example = "Spring Boot API 개발")
        private String title;

        @Schema(description = "작성자 이름", example = "홍길동")
        private String author;

        @Schema(description = "작성 일시", example = "2025-12-17T10:30:00")
        private LocalDateTime createdAt;

        @Schema(description = "태그 목록", example = "[\"DEVELOPMENT\", \"API\"]")
        private List<WorkLogTag> tags;

        @Schema(description = "본문 내용 길이 (글자 수)", example = "1234")
        private Integer contentLength;

        public static SummaryResponse from(WorkLog workLog) {
            SummaryResponse response = new SummaryResponse();
            response.id = workLog.getId();
            response.title = workLog.getTitle();
            response.author = workLog.getAuthor();
            response.createdAt = workLog.getCreatedAt();
            response.tags = workLog.getTags();
            response.contentLength = workLog.getContent() != null ? workLog.getContent().length() : 0;
            return response;
        }
    }
}
