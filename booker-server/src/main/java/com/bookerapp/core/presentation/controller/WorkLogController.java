package com.bookerapp.core.presentation.controller;

import com.bookerapp.core.application.WorkLogService;
import com.bookerapp.core.domain.model.WorkLogTag;
import com.bookerapp.core.domain.model.dto.PageResponse;
import com.bookerapp.core.domain.model.dto.WorkLogDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/work-logs")
@RequiredArgsConstructor
@Tag(name = "6. WorkLog", description = "작업 로그 관리 API")
public class WorkLogController {

    private final WorkLogService workLogService;

    @PostMapping
    @Operation(summary = "작업 로그 생성", description = """
            ## 개요
            새로운 작업 로그를 Markdown 형식으로 생성합니다.
            개발 작업, 회의록, 학습 내용 등을 체계적으로 기록할 수 있습니다.

            ## 주요 파라미터
            - `title`: 작업 로그 제목 (필수)
            - `content`: Markdown 형식의 본문 내용 (필수)
            - `author`: 작성자 이름 (필수)
            - `tags`: 분류를 위한 태그 목록 (선택)

            ## 응답 데이터
            생성된 작업 로그의 전체 정보와 고유 ID를 반환합니다.
            Location 헤더에 생성된 리소스 URL이 포함됩니다.

            ## 제약사항
            - 제목, 내용, 작성자는 필수 입력 항목입니다
            - Content는 Markdown 형식을 권장합니다
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "작업 로그 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkLogDto.Response.class), examples = @ExampleObject(value = """
                    {
                      "id": "20251217-103000-abc123",
                      "title": "Spring Boot API 개발",
                      "content": "# 작업 내용\\n\\n- API 엔드포인트 추가\\n- Swagger 문서화\\n- 테스트 코드 작성",
                      "author": "홍길동",
                      "createdAt": "2025-12-17T10:30:00",
                      "tags": ["DEVELOPMENT", "API"],
                      "contentLength": 1234
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락 또는 유효성 검증 실패)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "timestamp": "2025-12-18T10:30:00",
                      "status": 400,
                      "error": "제목은 필수입니다, 내용은 필수입니다",
                      "message": "MethodArgumentNotValidException",
                      "path": "/api/v1/work-logs"
                    }
                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<WorkLogDto.Response> createLog(@Valid @RequestBody WorkLogDto.CreateRequest request) {
        WorkLogDto.Response created = workLogService.createLog(
                request.getTitle(),
                request.getContent(),
                request.getAuthor(),
                request.getTags());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "작업 로그 목록 조회", description = """
            ## 개요
            모든 작업 로그를 조회하거나, 특정 태그로 필터링하여 조회합니다.
            페이지네이션을 지원하여 대량의 작업 로그를 효율적으로 조회할 수 있습니다.

            ## 주요 파라미터
            - `tags`: 필터링할 태그 목록 (선택)
              - 미지정 시: 모든 작업 로그 반환
              - 지정 시: 해당 태그를 포함하는 작업 로그만 반환
            - `page`: 페이지 번호 (0부터 시작, 기본값: 0)
            - `size`: 페이지 크기 (기본값: 20)
            - `sort`: 정렬 기준 (기본값: createdAt,desc - 최신순)

            ## 응답 데이터
            페이지네이션된 작업 로그 요약 목록을 반환합니다.
            각 항목에는 ID, 제목, 작성자, 태그, 생성일시, 본문 길이가 포함됩니다.
            페이지 정보(현재 페이지, 전체 페이지 수, 전체 항목 수)도 함께 반환됩니다.

            ## 제약사항
            - 태그는 DEVELOPMENT, MEETING, LEARNING, BUG_FIX, DEPLOYMENT 중 선택
            - 여러 태그를 동시에 필터링 가능
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class), examples = @ExampleObject(value = """
                    {
                      "content": [
                        {
                          "id": "20251217-103000-abc123",
                          "title": "Spring Boot API 개발",
                          "author": "홍길동",
                          "createdAt": "2025-12-17T10:30:00",
                          "tags": ["DEVELOPMENT", "API"],
                          "contentLength": 1234
                        },
                        {
                          "id": "20251216-153000-def456",
                          "title": "주간 회의록",
                          "author": "김개발",
                          "createdAt": "2025-12-16T15:30:00",
                          "tags": ["MEETING"],
                          "contentLength": 567
                        }
                      ],
                      "page": 0,
                      "size": 20,
                      "totalElements": 2,
                      "totalPages": 1,
                      "first": true,
                      "last": true
                    }
                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<PageResponse<WorkLogDto.SummaryResponse>> getAllLogs(
            @Parameter(description = "필터링할 태그 목록 (예: DEVELOPMENT, MEETING)", example = "DEVELOPMENT")
            @RequestParam(required = false) List<WorkLogTag> tags,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준 (예: createdAt,desc)", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<WorkLogDto.SummaryResponse> result = workLogService.getAllLogs(tags, pageable);

        return ResponseEntity.ok(PageResponse.of(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "작업 로그 상세 조회", description = """
            ## 개요
            특정 작업 로그의 전체 상세 정보를 JSON 형식으로 조회합니다.
            본문 내용, 메타데이터를 포함한 모든 정보가 반환됩니다.

            ## 주요 파라미터
            - `id`: 조회할 작업 로그의 고유 ID

            ## 응답 데이터
            작업 로그의 전체 상세 정보를 JSON으로 반환합니다.
            ID, 제목, 전체 본문 내용(Markdown), 작성자, 생성일시, 태그, 본문 길이가 포함됩니다.

            ## 제약사항
            - ID는 작업 로그 생성 시 반환된 값을 사용
            - 존재하지 않는 ID 조회 시 404 오류 발생
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkLogDto.Response.class), examples = @ExampleObject(value = """
                    {
                      "id": "20251217-103000-abc123",
                      "title": "Spring Boot API 개발",
                      "content": "# 작업 내용\\n\\n- API 엔드포인트 추가\\n- Swagger 문서화\\n- 테스트 코드 작성",
                      "author": "홍길동",
                      "createdAt": "2025-12-17T10:30:00",
                      "tags": ["DEVELOPMENT", "API"],
                      "contentLength": 1234
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "작업 로그를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "timestamp": "2025-12-18T10:30:00",
                      "status": 404,
                      "error": "작업 로그를 찾을 수 없음",
                      "message": "작업 로그를 찾을 수 없습니다. ID: invalid-id",
                      "path": "/api/v1/work-logs/invalid-id"
                    }
                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<WorkLogDto.Response> getLog(
            @Parameter(description = "작업 로그 ID", example = "20251217-103000-abc123") @PathVariable String id) {
        WorkLogDto.Response response = workLogService.getLog(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}/content", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(summary = "작업 로그 Markdown 원본 조회", description = """
            ## 개요
            특정 작업 로그의 Markdown 원본 내용만을 조회합니다.
            Content-Type이 text/markdown으로 반환되어 직접 렌더링 가능합니다.

            ## 주요 파라미터
            - `id`: 조회할 작업 로그의 고유 ID

            ## 응답 데이터
            Markdown 형식의 원본 텍스트만 반환합니다.
            메타데이터 없이 순수한 본문 내용만 포함됩니다.

            ## 제약사항
            - ID는 작업 로그 생성 시 반환된 값을 사용
            - 존재하지 않는 ID 조회 시 404 오류 발생

            ## 사용 예시
            - Markdown 렌더러에서 직접 렌더링
            - 파일로 다운로드
            - 외부 에디터에서 편집
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "text/markdown", examples = @ExampleObject(value = "# 작업 내용\n\n- API 엔드포인트 추가\n- Swagger 문서화\n- 테스트 코드 작성\n\n## 상세 설명\n\nREST API 개발 완료"))),
            @ApiResponse(responseCode = "404", description = "작업 로그를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "timestamp": "2025-12-18T10:30:00",
                      "status": 404,
                      "error": "작업 로그를 찾을 수 없음",
                      "message": "작업 로그를 찾을 수 없습니다. ID: invalid-id",
                      "path": "/api/v1/work-logs/invalid-id/content"
                    }
                    """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<String> getLogContent(
            @Parameter(description = "작업 로그 ID", example = "20251217-103000-abc123") @PathVariable String id) {
        String content = workLogService.getLogContent(id);
        return ResponseEntity.ok(content);
    }
}
