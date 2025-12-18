# Book Order API Documentation Enhancement Plan

## 작성일: 2024-12-18

## 현재 상황 분석

### 구현 완료 사항
- ✅ 7개 REST 엔드포인트 모두 구현됨
- ✅ Service Layer 비즈니스 로직 구현
- ✅ 기본 Swagger 어노테이션 적용
- ✅ DTO 구조 및 Validation
- ✅ 통합 테스트 코드 작성

### 개선 필요 사항 (BookController 패턴과 비교)
- ❌ 상세한 한국어 설명 (개요, 주요 파라미터, 응답 데이터, 제약사항)
- ❌ Request Body에 대한 구체적인 예시 (@ExampleObject)
- ❌ 완전한 에러 응답 문서화 (@ApiResponses)
- ❌ DTO 필드별 @Schema 설명 및 예시
- ❌ 실제 DB 값을 사용한 테스트 가능한 예시

## 프로젝트 정보

### 기술 스택
- Framework: Spring Boot 3.x
- Language: Java 17+
- API Documentation: Swagger/OpenAPI 3.0
- Validation: Jakarta Validation

### 기존 API 엔드포인트

| HTTP Method | URL | 설명 | 구현 상태 |
|-------------|-----|------|-----------|
| POST | `/api/v1/book-orders` | 도서 주문 요청 생성 | ✅ |
| GET | `/api/v1/book-orders/my` | 내 도서 주문 요청 목록 조회 | ✅ |
| GET | `/api/v1/book-orders` | 모든 도서 주문 요청 목록 조회 (관리자) | ✅ |
| GET | `/api/v1/book-orders/{id}` | 도서 주문 요청 상세 조회 | ✅ |
| POST | `/api/v1/book-orders/{id}/approve` | 도서 주문 요청 승인 (관리자) | ✅ |
| POST | `/api/v1/book-orders/{id}/reject` | 도서 주문 요청 거부 (관리자) | ✅ |
| POST | `/api/v1/book-orders/{id}/receive` | 도서 입고 처리 (관리자) | ✅ |

### 비즈니스 플로우

```
[사용자] 도서 주문 요청 생성 (POST /book-orders)
   ↓
[시스템] PENDING 상태로 생성
   ↓
[관리자] 승인 OR 거부
   ↓
APPROVED → [관리자] 입고 처리 → RECEIVED (완료)
REJECTED → 종료
```

### 상태 전이도

```
PENDING → APPROVED → RECEIVED
   ↓
REJECTED
```

## URL 설계 검토 및 결정

### 현재 URL 패턴 분석

| Endpoint | HTTP Method | URL | RESTful 점수 | 평가 |
|----------|-------------|-----|--------------|------|
| 목록 조회 (전체) | GET | `/api/v1/book-orders` | ✅ Excellent | 표준 RESTful |
| 목록 조회 (내것) | GET | `/api/v1/book-orders/my` | ✅ Good | 사용자별 필터링 |
| 상세 조회 | GET | `/api/v1/book-orders/{id}` | ✅ Excellent | 표준 RESTful |
| 생성 | POST | `/api/v1/book-orders` | ✅ Excellent | 표준 RESTful |
| 승인 | POST | `/api/v1/book-orders/{id}/approve` | ✅ Good | Action-oriented |
| 거부 | POST | `/api/v1/book-orders/{id}/reject` | ✅ Good | Action-oriented |
| 입고 | POST | `/api/v1/book-orders/{id}/receive` | ✅ Good | Action-oriented |

### URL 설계 결정 사항

**현재 설계를 유지**합니다. 이유:

1. **비즈니스 의미 명확성**: `approve`, `reject`, `receive`는 단순한 상태 변경이 아니라 비즈니스 액션
2. **BookLoan API와 일관성**: BookLoan의 `return`, `extend`와 같은 패턴
3. **산업 표준**: GitHub, Stripe 등 주요 API들이 사용하는 패턴
4. **확장성**: 각 액션마다 고유한 파라미터 및 검증 로직 추가 가능
5. **개발자 경험**: URL만 보고도 어떤 동작인지 명확히 이해 가능

### 대안 검토 (채택 안 함)

#### 대안 1: PATCH로 상태 변경
```
PATCH /api/v1/book-orders/{id}
Body: { "status": "APPROVED", "comments": "..." }
```
**거부 이유**:
- 비즈니스 의미가 불명확
- 단일 엔드포인트에서 여러 액션 처리로 복잡도 증가
- 각 상태 전이의 유효성 검증이 어려움

#### 대안 2: PUT으로 리소스 교체
```
PUT /api/v1/book-orders/{id}/status
Body: { "status": "APPROVED" }
```
**거부 이유**:
- 여전히 액션 의미가 명확하지 않음
- 승인/거부/입고의 차이점이 드러나지 않음

### Query Parameter vs Path Parameter 결정

| 파라미터 | 타입 | 결정 | 이유 |
|----------|------|------|------|
| id | Path | ✅ | 리소스 식별에 필수 |
| status (필터) | Query | ✅ | 선택적 필터링 조건 |
| page, size, sort | Query | ✅ | 페이징/정렬 옵션 |
| userId (내부) | Header/Context | ✅ | 인증 토큰에서 추출 |

## 개선 구현 계획

### Phase 1: DTO Schema 문서화

#### 1.1 BookOrderDto.Request 개선

**현재 상태**:
```java
@NotBlank(message = "도서명은 필수입니다")
@Size(max = 30, message = "도서명은 30자를 초과할 수 없습니다")
private String title;
```

**개선 목표**:
```java
@Schema(
    description = "도서 제목 - 주문하려는 도서의 정확한 제목을 입력하세요",
    example = "클린 아키텍처",
    required = true,
    maxLength = 30
)
@NotBlank(message = "도서명은 필수입니다")
@Size(max = 30, message = "도서명은 30자를 초과할 수 없습니다")
private String title;

@Schema(
    description = "저자명 - 도서 저자 이름 (선택사항)",
    example = "로버트 C. 마틴",
    maxLength = 30
)
@Size(max = 30, message = "저자명은 30자를 초과할 수 없습니다")
private String author;

@Schema(
    description = "출판사명 - 도서 출판사 이름 (선택사항)",
    example = "인사이트",
    maxLength = 30
)
@Size(max = 30, message = "출판사명은 30자를 초과할 수 없습니다")
private String publisher;

@Schema(
    description = "ISBN - 10자리 또는 13자리 ISBN 번호 (선택사항, 있으면 입력 권장)",
    example = "9788966262472",
    maxLength = 20
)
@Size(max = 20, message = "ISBN은 20자를 초과할 수 없습니다")
private String isbn;
```

#### 1.2 BookOrderDto.Response 개선

**개선 목표**: 각 필드에 상세한 @Schema 어노테이션 추가

```java
@Schema(description = "도서 주문 요청 ID - 시스템에서 자동 생성", example = "1")
private Long id;

@Schema(description = "도서 제목", example = "클린 아키텍처")
private String title;

@Schema(description = "저자명", example = "로버트 C. 마틴")
private String author;

@Schema(description = "출판사명", example = "인사이트")
private String publisher;

@Schema(description = "ISBN 번호", example = "9788966262472")
private String isbn;

@Schema(description = "요청자 ID - 주문을 요청한 사용자의 고유 ID", example = "user-123")
private String requesterId;

@Schema(description = "요청자 이름 - 주문을 요청한 사용자의 이름", example = "홍길동")
private String requesterName;

@Schema(
    description = "주문 상태 - PENDING(검토대기), APPROVED(승인됨), REJECTED(거부됨), RECEIVED(입고완료)",
    example = "PENDING",
    allowableValues = {"PENDING", "APPROVED", "REJECTED", "RECEIVED"}
)
private BookOrder.BookOrderStatus status;

@Schema(description = "관리자 코멘트 - 승인/거부 시 관리자가 작성한 의견", example = "도서관 정책에 부합하여 승인합니다")
private String adminComments;

@Schema(description = "승인/거부 일시 - ISO 8601 형식", example = "2024-12-18T10:30:00")
private LocalDateTime approvedAt;

@Schema(description = "승인/거부한 관리자 ID", example = "admin-001")
private String approvedBy;

@Schema(description = "입고 완료 일시 - ISO 8601 형식", example = "2024-12-20T14:00:00")
private LocalDateTime receivedAt;

@Schema(description = "입고 처리한 관리자 ID", example = "admin-002")
private String receivedBy;

@Schema(description = "생성 일시 - 주문 요청 생성 시각", example = "2024-12-18T09:00:00")
private LocalDateTime createdAt;

@Schema(description = "수정 일시 - 마지막 수정 시각", example = "2024-12-18T10:30:00")
private LocalDateTime updatedAt;
```

#### 1.3 BookOrderDto.Action 개선

```java
@Schema(
    description = "관리자 코멘트 - 승인/거부 사유를 입력합니다 (최대 1000자)",
    example = "도서관 정책 및 예산을 고려하여 승인합니다",
    maxLength = 1000
)
@Size(max = 1000, message = "관리자 코멘트는 1000자를 초과할 수 없습니다")
private String comments;
```

### Phase 2: Controller 엔드포인트 상세 문서화

각 엔드포인트마다 BookController 패턴을 따라 작성합니다.

#### 2.1 POST /api/v1/book-orders - 도서 주문 요청 생성

**문서화 구조**:
```java
@Operation(
    summary = "도서 주문 요청 생성",
    description = """
        ## 개요
        도서관에 없는 도서를 주문 요청합니다.
        요청된 도서는 관리자의 승인을 거쳐 입고 처리됩니다.

        ## 주요 파라미터
        - `title`: 도서 제목 (필수, 최대 30자)
        - `author`: 저자명 (선택, 최대 30자)
        - `publisher`: 출판사명 (선택, 최대 30자)
        - `isbn`: ISBN 번호 (선택, 최대 20자)

        ## 응답 데이터
        생성된 도서 주문 요청의 전체 정보를 반환합니다.
        - 초기 상태: PENDING (검토 대기)
        - Location 헤더에 생성된 리소스 URL 포함

        ## 제약사항
        - 인증 필요: Bearer Token (Authorization 헤더)
        - 도서 제목은 필수 입력
        - 중복 주문 요청 가능 (시스템에서 별도 제한 없음)
        - 상태 흐름: PENDING → APPROVED/REJECTED → RECEIVED(승인 시에만)
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "도서 주문 요청 생성 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BookOrderDto.Response.class),
            examples = @ExampleObject(
                name = "생성된 주문 요청 예시",
                value = """
                    {
                      "id": 1,
                      "title": "클린 아키텍처",
                      "author": "로버트 C. 마틴",
                      "publisher": "인사이트",
                      "isbn": "9788966262472",
                      "requesterId": "user-123",
                      "requesterName": "홍길동",
                      "status": "PENDING",
                      "adminComments": null,
                      "approvedAt": null,
                      "approvedBy": null,
                      "receivedAt": null,
                      "receivedBy": null,
                      "createdAt": "2024-12-18T09:00:00",
                      "updatedAt": "2024-12-18T09:00:00"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (필수 필드 누락 또는 유효성 검증 실패)",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "error": "Validation Failed",
                      "details": [
                        {
                          "field": "title",
                          "message": "도서명은 필수입니다"
                        }
                      ]
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패 - 유효한 인증 토큰이 필요합니다"
    ),
    @ApiResponse(
        responseCode = "422",
        description = "유효성 검증 실패",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "error": "Validation Failed",
                      "details": [
                        {
                          "field": "title",
                          "message": "도서명은 30자를 초과할 수 없습니다"
                        }
                      ]
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류"
    )
})
@RequestBody(
    description = "도서 주문 요청 데이터",
    required = true,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = BookOrderDto.Request.class),
        examples = {
            @ExampleObject(
                name = "클린 아키텍처 주문",
                summary = "기술서 주문 예시",
                description = "로버트 마틴의 클린 아키텍처 도서를 주문하는 예시입니다",
                value = """
                    {
                      "title": "클린 아키텍처",
                      "author": "로버트 C. 마틴",
                      "publisher": "인사이트",
                      "isbn": "9788966262472"
                    }
                    """
            ),
            @ExampleObject(
                name = "필수 정보만 입력",
                summary = "제목만으로 주문",
                description = "제목만 입력하고 나머지 정보는 생략하는 경우",
                value = """
                    {
                      "title": "도메인 주도 설계"
                    }
                    """
            ),
            @ExampleObject(
                name = "소설 주문",
                summary = "문학 도서 주문 예시",
                description = "소설 도서를 주문하는 예시입니다",
                value = """
                    {
                      "title": "1984",
                      "author": "조지 오웰",
                      "publisher": "민음사",
                      "isbn": "9788937460876"
                    }
                    """
            )
        }
    )
)
```

#### 2.2 GET /api/v1/book-orders/my - 내 도서 주문 요청 목록 조회

```java
@Operation(
    summary = "내 도서 주문 요청 목록 조회 (페이징)",
    description = """
        ## 개요
        현재 로그인한 사용자가 요청한 도서 주문 목록을 조회합니다.
        페이징과 정렬을 지원합니다.

        ## 사용 예시
        - 최근 주문 조회: `/api/v1/book-orders/my?page=0&size=20&sort=createdAt,desc`
        - 제목 기준 정렬: `/api/v1/book-orders/my?page=0&size=10&sort=title,asc`

        ## 주요 파라미터
        - `page`: 페이지 번호 (0부터 시작, 기본값: 0)
        - `size`: 페이지 크기 (기본값: 20)
        - `sort`: 정렬 기준 (예: createdAt,desc)

        ## 응답 데이터
        페이지네이션된 주문 요청 목록과 메타데이터를 반환합니다.
        - `content`: 주문 요청 배열
        - `totalElements`: 전체 요청 수
        - `totalPages`: 전체 페이지 수
        - `page`: 현재 페이지 번호
        - `size`: 페이지 크기

        ## 제약사항
        - 인증 필요: Bearer Token
        - 본인의 주문 요청만 조회 가능
        - 최대 페이지 크기: 100
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                name = "내 주문 목록 예시",
                value = """
                    {
                      "content": [
                        {
                          "id": 2,
                          "title": "리팩터링 2판",
                          "author": "마틴 파울러",
                          "publisher": "한빛미디어",
                          "isbn": "9791162242742",
                          "requesterId": "user-123",
                          "requesterName": "홍길동",
                          "status": "APPROVED",
                          "adminComments": "도서관 정책에 부합하여 승인합니다",
                          "approvedAt": "2024-12-18T10:30:00",
                          "approvedBy": "admin-001",
                          "receivedAt": null,
                          "receivedBy": null,
                          "createdAt": "2024-12-18T09:00:00",
                          "updatedAt": "2024-12-18T10:30:00"
                        },
                        {
                          "id": 1,
                          "title": "클린 아키텍처",
                          "author": "로버트 C. 마틴",
                          "publisher": "인사이트",
                          "isbn": "9788966262472",
                          "requesterId": "user-123",
                          "requesterName": "홍길동",
                          "status": "PENDING",
                          "adminComments": null,
                          "approvedAt": null,
                          "approvedBy": null,
                          "receivedAt": null,
                          "receivedBy": null,
                          "createdAt": "2024-12-17T14:00:00",
                          "updatedAt": "2024-12-17T14:00:00"
                        }
                      ],
                      "page": 0,
                      "size": 20,
                      "totalElements": 2,
                      "totalPages": 1,
                      "first": true,
                      "last": true
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패 - 유효한 인증 토큰이 필요합니다"
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 페이징 파라미터"
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류"
    )
})
```

#### 2.3 GET /api/v1/book-orders - 모든 도서 주문 요청 목록 조회 (관리자)

```java
@Operation(
    summary = "모든 도서 주문 요청 목록 조회 (관리자용)",
    description = """
        ## 개요
        모든 사용자의 도서 주문 요청을 조회합니다. 관리자 권한이 필요합니다.
        상태별 필터링과 페이징, 정렬을 지원합니다.

        ## 사용 예시
        - 전체 주문 조회: `/api/v1/book-orders?page=0&size=20&sort=createdAt,desc`
        - 대기 중인 주문만: `/api/v1/book-orders?status=PENDING&page=0&size=20`
        - 승인된 주문만: `/api/v1/book-orders?status=APPROVED&page=0&size=20`

        ## 주요 파라미터
        - `status`: 주문 상태 필터 (PENDING, APPROVED, REJECTED, RECEIVED, 선택)
        - `page`: 페이지 번호 (0부터 시작, 기본값: 0)
        - `size`: 페이지 크기 (기본값: 20)
        - `sort`: 정렬 기준 (기본값: createdAt,desc)

        ## 응답 데이터
        페이지네이션된 주문 요청 목록과 메타데이터를 반환합니다.
        모든 사용자의 주문 요청을 포함합니다.

        ## 제약사항
        - 인증 필요: Bearer Token
        - 관리자 권한 필요 (ROLE_ADMIN)
        - 최대 페이지 크기: 100
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                name = "관리자 주문 목록 예시",
                value = """
                    {
                      "content": [
                        {
                          "id": 3,
                          "title": "데이터 중심 애플리케이션 설계",
                          "author": "마틴 클레프만",
                          "publisher": "위키북스",
                          "isbn": "9791158391409",
                          "requesterId": "user-456",
                          "requesterName": "김철수",
                          "status": "PENDING",
                          "adminComments": null,
                          "approvedAt": null,
                          "approvedBy": null,
                          "receivedAt": null,
                          "receivedBy": null,
                          "createdAt": "2024-12-18T11:00:00",
                          "updatedAt": "2024-12-18T11:00:00"
                        },
                        {
                          "id": 2,
                          "title": "리팩터링 2판",
                          "author": "마틴 파울러",
                          "publisher": "한빛미디어",
                          "isbn": "9791162242742",
                          "requesterId": "user-123",
                          "requesterName": "홍길동",
                          "status": "APPROVED",
                          "adminComments": "도서관 정책에 부합하여 승인합니다",
                          "approvedAt": "2024-12-18T10:30:00",
                          "approvedBy": "admin-001",
                          "receivedAt": null,
                          "receivedBy": null,
                          "createdAt": "2024-12-18T09:00:00",
                          "updatedAt": "2024-12-18T10:30:00"
                        }
                      ],
                      "page": 0,
                      "size": 20,
                      "totalElements": 2,
                      "totalPages": 1,
                      "first": true,
                      "last": true
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패"
    ),
    @ApiResponse(
        responseCode = "403",
        description = "권한 부족 - 관리자 권한이 필요합니다"
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 파라미터"
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류"
    )
})
```

#### 2.4 GET /api/v1/book-orders/{id} - 도서 주문 요청 상세 조회

```java
@Operation(
    summary = "도서 주문 요청 상세 조회",
    description = """
        ## 개요
        특정 도서 주문 요청의 상세 정보를 조회합니다.
        본인의 주문 요청 또는 관리자만 조회 가능합니다.

        ## 주요 파라미터
        - `id`: 주문 요청 ID (Long 타입, Path Parameter)

        ## 응답 데이터
        주문 요청의 모든 정보를 포함한 상세 데이터를 반환합니다.
        - 기본 정보: 제목, 저자, 출판사, ISBN
        - 요청자 정보: 요청자 ID 및 이름
        - 상태 정보: 현재 상태, 승인/거부/입고 일시
        - 관리자 코멘트: 승인/거부 사유

        ## 제약사항
        - 인증 필요: Bearer Token
        - 본인의 주문 요청 또는 관리자만 조회 가능
        - 존재하지 않는 ID 조회 시 404 오류 발생
        - 권한 없는 요청 조회 시 403 오류 발생
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BookOrderDto.Response.class),
            examples = @ExampleObject(
                name = "주문 상세 조회 예시",
                value = """
                    {
                      "id": 1,
                      "title": "클린 아키텍처",
                      "author": "로버트 C. 마틴",
                      "publisher": "인사이트",
                      "isbn": "9788966262472",
                      "requesterId": "user-123",
                      "requesterName": "홍길동",
                      "status": "APPROVED",
                      "adminComments": "도서관 정책에 부합하여 승인합니다",
                      "approvedAt": "2024-12-18T10:30:00",
                      "approvedBy": "admin-001",
                      "receivedAt": null,
                      "receivedBy": null,
                      "createdAt": "2024-12-18T09:00:00",
                      "updatedAt": "2024-12-18T10:30:00"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패"
    ),
    @ApiResponse(
        responseCode = "403",
        description = "권한 부족 - 본인의 주문 요청이 아니며 관리자도 아닙니다"
    ),
    @ApiResponse(
        responseCode = "404",
        description = "주문 요청을 찾을 수 없음",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "error": "Book order not found",
                      "id": 999
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류"
    )
})
```

#### 2.5 POST /api/v1/book-orders/{id}/approve - 도서 주문 요청 승인

```java
@Operation(
    summary = "도서 주문 요청 승인 (관리자 전용)",
    description = """
        ## 개요
        관리자가 도서 주문 요청을 승인합니다.
        승인된 주문은 이후 입고 처리가 가능합니다.

        ## 주요 파라미터
        - `id`: 주문 요청 ID (Path Parameter)
        - `comments`: 승인 사유 또는 관리자 코멘트 (Request Body, 선택)

        ## 응답 데이터
        승인 처리된 주문 요청의 상세 정보를 반환합니다.
        - 상태: APPROVED로 변경
        - approvedAt: 승인 일시 기록
        - approvedBy: 승인한 관리자 ID 기록

        ## 제약사항
        - 인증 필요: Bearer Token
        - 관리자 권한 필요 (ROLE_ADMIN)
        - 주문 상태가 PENDING이어야 승인 가능
        - 이미 승인/거부된 주문은 재처리 불가
        - 승인 후 입고 처리(receive)를 진행할 수 있음
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "승인 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BookOrderDto.Response.class),
            examples = @ExampleObject(
                name = "승인 완료 예시",
                value = """
                    {
                      "id": 1,
                      "title": "클린 아키텍처",
                      "author": "로버트 C. 마틴",
                      "publisher": "인사이트",
                      "isbn": "9788966262472",
                      "requesterId": "user-123",
                      "requesterName": "홍길동",
                      "status": "APPROVED",
                      "adminComments": "도서관 정책에 부합하여 승인합니다",
                      "approvedAt": "2024-12-18T10:30:00",
                      "approvedBy": "admin-001",
                      "receivedAt": null,
                      "receivedBy": null,
                      "createdAt": "2024-12-18T09:00:00",
                      "updatedAt": "2024-12-18T10:30:00"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 - PENDING 상태가 아닌 주문은 승인 불가",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "error": "Invalid state transition",
                      "message": "이미 처리된 주문 요청입니다",
                      "currentStatus": "APPROVED"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패"
    ),
    @ApiResponse(
        responseCode = "403",
        description = "권한 부족 - 관리자 권한이 필요합니다"
    ),
    @ApiResponse(
        responseCode = "404",
        description = "주문 요청을 찾을 수 없음"
    ),
    @ApiResponse(
        responseCode = "422",
        description = "유효성 검증 실패 - 코멘트 길이 초과",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "error": "Validation Failed",
                      "details": [
                        {
                          "field": "comments",
                          "message": "관리자 코멘트는 1000자를 초과할 수 없습니다"
                        }
                      ]
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류"
    )
})
@RequestBody(
    description = "승인 처리 데이터 - 관리자 코멘트 (선택)",
    required = true,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = BookOrderDto.Action.class),
        examples = {
            @ExampleObject(
                name = "코멘트 포함 승인",
                summary = "승인 사유 작성",
                value = """
                    {
                      "comments": "도서관 정책에 부합하여 승인합니다"
                    }
                    """
            ),
            @ExampleObject(
                name = "코멘트 없이 승인",
                summary = "승인 사유 생략",
                value = """
                    {
                      "comments": ""
                    }
                    """
            )
        }
    )
)
```

#### 2.6 POST /api/v1/book-orders/{id}/reject - 도서 주문 요청 거부

```java
@Operation(
    summary = "도서 주문 요청 거부 (관리자 전용)",
    description = """
        ## 개요
        관리자가 도서 주문 요청을 거부합니다.
        거부된 주문은 더 이상 처리할 수 없습니다.

        ## 주요 파라미터
        - `id`: 주문 요청 ID (Path Parameter)
        - `comments`: 거부 사유 (Request Body, 권장)

        ## 응답 데이터
        거부 처리된 주문 요청의 상세 정보를 반환합니다.
        - 상태: REJECTED로 변경
        - approvedAt: 거부 처리 일시 기록
        - approvedBy: 거부 처리한 관리자 ID 기록

        ## 제약사항
        - 인증 필요: Bearer Token
        - 관리자 권한 필요 (ROLE_ADMIN)
        - 주문 상태가 PENDING이어야 거부 가능
        - 이미 승인/거부된 주문은 재처리 불가
        - 거부 사유를 comments에 작성하는 것을 권장
        - 거부 후에는 되돌릴 수 없음
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "거부 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BookOrderDto.Response.class),
            examples = @ExampleObject(
                name = "거부 완료 예시",
                value = """
                    {
                      "id": 1,
                      "title": "클린 아키텍처",
                      "author": "로버트 C. 마틴",
                      "publisher": "인사이트",
                      "isbn": "9788966262472",
                      "requesterId": "user-123",
                      "requesterName": "홍길동",
                      "status": "REJECTED",
                      "adminComments": "현재 도서관 예산 부족으로 거부합니다",
                      "approvedAt": "2024-12-18T10:30:00",
                      "approvedBy": "admin-001",
                      "receivedAt": null,
                      "receivedBy": null,
                      "createdAt": "2024-12-18T09:00:00",
                      "updatedAt": "2024-12-18T10:30:00"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 - PENDING 상태가 아닌 주문은 거부 불가",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "error": "Invalid state transition",
                      "message": "이미 처리된 주문 요청입니다",
                      "currentStatus": "APPROVED"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패"
    ),
    @ApiResponse(
        responseCode = "403",
        description = "권한 부족 - 관리자 권한이 필요합니다"
    ),
    @ApiResponse(
        responseCode = "404",
        description = "주문 요청을 찾을 수 없음"
    ),
    @ApiResponse(
        responseCode = "422",
        description = "유효성 검증 실패 - 코멘트 길이 초과",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "error": "Validation Failed",
                      "details": [
                        {
                          "field": "comments",
                          "message": "관리자 코멘트는 1000자를 초과할 수 없습니다"
                        }
                      ]
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류"
    )
})
@RequestBody(
    description = "거부 처리 데이터 - 거부 사유를 작성하는 것을 권장합니다",
    required = true,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = BookOrderDto.Action.class),
        examples = {
            @ExampleObject(
                name = "예산 부족 사유",
                summary = "예산 문제로 거부",
                value = """
                    {
                      "comments": "현재 도서관 예산 부족으로 거부합니다"
                    }
                    """
            ),
            @ExampleObject(
                name = "중복 도서 사유",
                summary = "기존 소장 도서와 중복",
                value = """
                    {
                      "comments": "이미 도서관에 동일한 도서가 있어 거부합니다"
                    }
                    """
            ),
            @ExampleObject(
                name = "정책 부합하지 않음",
                summary = "도서관 정책에 맞지 않음",
                value = """
                    {
                      "comments": "도서관 장서 정책에 부합하지 않아 거부합니다"
                    }
                    """
            )
        }
    )
)
```

#### 2.7 POST /api/v1/book-orders/{id}/receive - 도서 입고 처리

```java
@Operation(
    summary = "도서 입고 처리 (관리자 전용)",
    description = """
        ## 개요
        승인된 도서의 입고를 처리합니다.
        입고 처리가 완료되면 도서 주문 프로세스가 종료됩니다.

        ## 주요 파라미터
        - `id`: 주문 요청 ID (Path Parameter)

        ## 응답 데이터
        입고 처리된 주문 요청의 상세 정보를 반환합니다.
        - 상태: RECEIVED로 변경
        - receivedAt: 입고 완료 일시 기록
        - receivedBy: 입고 처리한 관리자 ID 기록

        ## 제약사항
        - 인증 필요: Bearer Token
        - 관리자 권한 필요 (ROLE_ADMIN)
        - 주문 상태가 APPROVED여야 입고 처리 가능
        - PENDING이나 REJECTED 상태에서는 입고 불가
        - 입고 처리 후 주문 프로세스 완료
        - 입고 처리 후에는 되돌릴 수 없음
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "입고 처리 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BookOrderDto.Response.class),
            examples = @ExampleObject(
                name = "입고 완료 예시",
                value = """
                    {
                      "id": 1,
                      "title": "클린 아키텍처",
                      "author": "로버트 C. 마틴",
                      "publisher": "인사이트",
                      "isbn": "9788966262472",
                      "requesterId": "user-123",
                      "requesterName": "홍길동",
                      "status": "RECEIVED",
                      "adminComments": "도서관 정책에 부합하여 승인합니다",
                      "approvedAt": "2024-12-18T10:30:00",
                      "approvedBy": "admin-001",
                      "receivedAt": "2024-12-20T14:00:00",
                      "receivedBy": "admin-002",
                      "createdAt": "2024-12-18T09:00:00",
                      "updatedAt": "2024-12-20T14:00:00"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 - APPROVED 상태가 아닌 주문은 입고 불가",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    {
                      "error": "Invalid state transition",
                      "message": "승인된 주문만 입고 처리할 수 있습니다",
                      "currentStatus": "PENDING"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패"
    ),
    @ApiResponse(
        responseCode = "403",
        description = "권한 부족 - 관리자 권한이 필요합니다"
    ),
    @ApiResponse(
        responseCode = "404",
        description = "주문 요청을 찾을 수 없음"
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류"
    )
})
```

### Phase 3: Parameter 어노테이션 추가

각 파라미터에 대해 상세한 @Parameter 어노테이션을 추가합니다.

```java
// 예시
@Parameter(
    description = "주문 요청 ID - 실제 존재하는 주문 ID를 입력하세요",
    example = "1",
    required = true
)
@PathVariable Long id

@Parameter(
    description = "주문 상태 필터 - PENDING(대기), APPROVED(승인), REJECTED(거부), RECEIVED(입고)",
    example = "PENDING"
)
@RequestParam(required = false) BookOrder.BookOrderStatus status
```

### Phase 4: 테스트 가능한 curl 명령어 작성

실제 DB 값을 사용한 테스트 가능한 curl 명령어를 작성합니다.

```bash
# 1. 도서 주문 요청 생성
curl -X POST http://localhost:8080/api/v1/book-orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "title": "클린 아키텍처",
    "author": "로버트 C. 마틴",
    "publisher": "인사이트",
    "isbn": "9788966262472"
  }'

# 2. 내 주문 목록 조회
curl -X GET "http://localhost:8080/api/v1/book-orders/my?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. 모든 주문 조회 (관리자)
curl -X GET "http://localhost:8080/api/v1/book-orders?status=PENDING&page=0&size=20" \
  -H "Authorization: Bearer ADMIN_TOKEN"

# 4. 주문 상세 조회
curl -X GET http://localhost:8080/api/v1/book-orders/1 \
  -H "Authorization: Bearer YOUR_TOKEN"

# 5. 주문 승인 (관리자)
curl -X POST http://localhost:8080/api/v1/book-orders/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{
    "comments": "도서관 정책에 부합하여 승인합니다"
  }'

# 6. 주문 거부 (관리자)
curl -X POST http://localhost:8080/api/v1/book-orders/1/reject \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{
    "comments": "현재 도서관 예산 부족으로 거부합니다"
  }'

# 7. 도서 입고 처리 (관리자)
curl -X POST http://localhost:8080/api/v1/book-orders/1/receive \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

## 구현 우선순위 (MVP)

### Must Have (현재 세션)
1. ✅ Phase 1: DTO Schema 문서화 - 모든 필드에 @Schema 추가
2. ✅ Phase 2: Controller 엔드포인트 상세 문서화 - BookController 패턴 적용
3. ✅ Phase 3: Parameter 어노테이션 추가
4. ✅ Phase 4: 테스트 curl 명령어 작성

### Should Have (검증)
1. Swagger UI에서 실제 테스트 수행
2. 모든 예시가 정상 작동하는지 검증
3. 에러 케이스 검증

### Nice to Have (향후)
1. 인증/인가 통합
2. 커스텀 예외 클래스 추가
3. 추가 비즈니스 규칙 (중복 주문 방지 등)

## 구현 체크리스트

### DTO 문서화
- [ ] BookOrderDto.Request - 모든 필드에 @Schema 추가
- [ ] BookOrderDto.Response - 모든 필드에 @Schema 추가
- [ ] BookOrderDto.Action - @Schema 추가

### Controller 문서화
- [ ] POST /api/v1/book-orders - 상세 문서화
- [ ] GET /api/v1/book-orders/my - 상세 문서화
- [ ] GET /api/v1/book-orders - 상세 문서화
- [ ] GET /api/v1/book-orders/{id} - 상세 문서화
- [ ] POST /api/v1/book-orders/{id}/approve - 상세 문서화
- [ ] POST /api/v1/book-orders/{id}/reject - 상세 문서화
- [ ] POST /api/v1/book-orders/{id}/receive - 상세 문서화

### 각 엔드포인트마다 확인
- [ ] Summary가 50자 이하인가?
- [ ] Description에 4개 섹션 (개요, 주요 파라미터, 응답 데이터, 제약사항) 포함되었는가?
- [ ] Request Body에 최소 2개 이상의 @ExampleObject가 있는가?
- [ ] 모든 HTTP 상태 코드 (200/201, 400, 401, 403, 404, 422, 500)에 대한 @ApiResponse가 있는가?
- [ ] 에러 응답 예시가 구체적인가?
- [ ] @Parameter 어노테이션에 description과 example이 있는가?

## 성공 기준

1. **문서 완성도**: BookController와 동일한 수준의 상세 문서화
2. **테스트 가능성**: Swagger UI에서 Try it out으로 바로 테스트 가능
3. **개발자 경험**: 문서만 보고도 API 사용 방법을 완전히 이해 가능
4. **일관성**: 프로젝트 전체 API 문서화 패턴과 일치

## 예상 소요 시간

- Phase 1 (DTO 문서화): 30분
- Phase 2 (Controller 문서화): 2시간
- Phase 3 (Parameter 어노테이션): 30분
- Phase 4 (curl 명령어): 30분
- 총 예상 시간: 약 3.5시간

## 참고 사항

- BookController.java를 참고 패턴으로 사용
- BookLoanController.java도 참고 (Action 엔드포인트 패턴)
- 실제 DB에 있는 값을 사용하여 예시 작성
- 한국어/영어 혼용 문서화 (Description은 한국어, Field name은 영어)
