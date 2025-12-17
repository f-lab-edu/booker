# API Design Audit Report

## 목적
Booker API의 Swagger 문서화 및 URL 설계를 검수하고, 개선사항을 도출합니다.

## 검수 일자
2025-12-17

## 검수 기준
- RESTful URL 설계 원칙
- Swagger/OpenAPI 문서화 완성도
- Error Response 정의
- Schema 정의 품질
- 일관성 (Naming, Structure)

---

## 전체 API 목록

### 1. Book API (`/api/v1/books`)
| Method | Endpoint | Summary | 상태 |
|--------|----------|---------|------|
| POST | `/api/v1/books` | 도서 생성 | ⚠️ |
| GET | `/api/v1/books/{id}` | 도서 조회 | ⚠️ |
| GET | `/api/v1/books` | 도서 검색 | ⚠️ |
| PUT | `/api/v1/books/{id}` | 도서 수정 | ⚠️ |
| DELETE | `/api/v1/books/{id}` | 도서 삭제 | ⚠️ |

**평가:**
- ✅ RESTful URL 설계 적절
- ✅ PageResponse 사용 (일관성)
- ⚠️ Summary만 있고 Description 부족
- ❌ Error responses 문서화 없음
- ❌ 구조화된 설명 없음 (개요/파라미터/응답/제약사항)

### 2. Book Loan API (`/api/v1/loans`)
| Method | Endpoint | Summary | 상태 |
|--------|----------|---------|------|
| POST | `/api/v1/loans` | 도서 대출 신청 | ✅ |
| POST | `/api/v1/loans/{loanId}/return` | 도서 반납 신청 | ✅ |
| POST | `/api/v1/loans/{loanId}/extend` | 대출 기간 연장 | ✅ |
| GET | `/api/v1/loans` | 내 대출 목록 조회 | ✅ |
| GET | `/api/v1/loans/{loanId}` | 대출 상세 조회 | ✅ |

**평가:**
- ✅ RESTful URL 설계 적절
- ✅ Description 구조화 (제약 조건, 오류 예시 포함)
- ✅ PageResponse 사용
- ⚠️ userId를 query parameter로 받음 (임시 구현)
- ❌ Error responses (@ApiResponses) 문서화 없음

### 3. Book Order API (`/api/v1/book-orders`)
| Method | Endpoint | Summary | 상태 |
|--------|----------|---------|------|
| POST | `/api/v1/book-orders` | 도서 주문 요청 생성 | ✅ |
| GET | `/api/v1/book-orders/my` | 내 도서 주문 요청 목록 조회 | ✅ |
| GET | `/api/v1/book-orders` | 모든 도서 주문 요청 목록 조회 | ✅ |
| GET | `/api/v1/book-orders/{id}` | 도서 주문 요청 상세 조회 | ✅ |
| POST | `/api/v1/book-orders/{id}/approve` | 도서 주문 요청 승인 | ✅ |
| POST | `/api/v1/book-orders/{id}/reject` | 도서 주문 요청 거부 | ✅ |
| POST | `/api/v1/book-orders/{id}/receive` | 도서 입고 처리 | ✅ |

**평가:**
- ✅ RESTful URL 설계 적절 (상태 전환 API 명확)
- ✅ Description 매우 상세 (상태 흐름, 예시 포함)
- ✅ PageResponse 사용
- ⚠️ userId, username을 query parameter로 받음 (임시 구현)
- ❌ Error responses 문서화 없음

### 4. Event API (`/api/v1/events`)
| Method | Endpoint | Summary | 상태 |
|--------|----------|---------|------|
| POST | `/api/v1/events` | 이벤트 생성 | ✅ |
| PUT | `/api/v1/events/{id}` | 이벤트 수정 | ✅ |
| DELETE | `/api/v1/events/{id}` | 이벤트 삭제 | ✅ |
| POST | `/api/v1/events/{id}/participants` | 이벤트 참가자 추가 | ⚠️ |
| DELETE | `/api/v1/events/{id}/participants/{memberId}` | 이벤트 참가자 제거 | ⚠️ |
| GET | `/api/v1/events` | 이벤트 목록 조회 | ✅ |
| GET | `/api/v1/events/{id}` | 이벤트 상세 조회 | ✅ |

**평가:**
- ✅ RESTful URL 설계 적절
- ✅ Description 상세 (필터링, 페이징, 정렬 설명)
- ✅ PageResponse 사용
- ⚠️ userId, username, email을 query parameter로 받음
- ⚠️ 관리자용 API인데 권한 체크 없음
- ❌ Error responses 문서화 없음

### 5. Event Participation API (`/api/v1/events/participation`)
| Method | Endpoint | Summary | 상태 |
|--------|----------|---------|------|
| POST | `/api/v1/events/participation/synchronized` | 이벤트 참여 신청 (Synchronized) | ✅ |
| POST | `/api/v1/events/participation/cas` | 이벤트 참여 신청 (CAS) | ✅ |
| GET | `/api/v1/events/participation/cas/retry-count` | CAS 재시도 횟수 조회 | ✅ |
| POST | `/api/v1/events/participation/cas/reset-retry-count` | CAS 재시도 횟수 초기화 | ✅ |

**평가:**
- ✅ URL 설계 적절 (동시성 제어 방식별 구분)
- ✅ Description 매우 상세 (동시성 제어 방식, 성능 특징 설명)
- ⚠️ userId를 query parameter로 받음
- ❌ Error responses 문서화 없음
- 💡 학습/테스트 목적의 API로 설계가 잘 되어 있음

### 6. WorkLog API (`/api/v1/work-logs`)
| Method | Endpoint | Summary | 상태 |
|--------|----------|---------|------|
| POST | `/api/v1/work-logs` | 작업 로그 생성 | ✅✅ |
| GET | `/api/v1/work-logs` | 작업 로그 목록 조회 | ✅✅ |
| GET | `/api/v1/work-logs/{id}` | 작업 로그 원본 조회 | ✅✅ |

**평가:**
- ✅ RESTful URL 설계 적절
- ✅ Description 구조화 (## 개요, ## 주요 파라미터, ## 응답 데이터, ## 제약사항)
- ✅ @ApiResponses 정의 (200, 400, 404, 500)
- ✅ Schema annotations 완벽 (examples, requiredMode)
- ✅ **이 컨트롤러가 가장 잘 문서화되어 있음 - 참고 표준**

### 7. Auth API (`/api/v1/auth`)
| Method | Endpoint | Summary | 상태 |
|--------|----------|---------|------|
| POST | `/api/v1/auth/google/verify` | Google Token 검증 | ⚠️ |

**평가:**
- ✅ URL 설계 적절
- ⚠️ Summary만 있고 Description 부족
- ❌ Error responses 문서화 없음
- ❌ Request/Response Schema 문서화 부족

### 8. LoadTest API (`/api/load-test`)
| Method | Endpoint | Summary | 상태 |
|--------|----------|---------|------|
| POST | `/api/load-test/participate/optimistic` | - | ❌ |
| POST | `/api/load-test/participate/pessimistic` | - | ❌ |
| POST | `/api/load-test/participate/cas` | - | ❌ |
| POST | `/api/load-test/participate/synchronized` | - | ❌ |
| GET | `/api/load-test/health` | - | ❌ |
| POST | `/api/load-test/setup` | - | ❌ |
| POST | `/api/load-test/cleanup` | - | ❌ |

**평가:**
- ❌ URL이 `/api/v1`을 따르지 않음 (일관성 없음)
- ❌ Swagger @Operation annotations 없음
- ❌ Description 없음
- ❌ Error responses 문서화 없음
- 💡 내부 테스트용이지만 문서화 필요

---

## 주요 발견 사항

### 1. URL 설계 이슈

#### ❌ 일관성 위배
- **LoadTestController**: `/api/load-test`
  - **권장**: `/api/v1/load-test` (버전 포함)

#### ✅ 좋은 설계
- 모든 리소스가 복수형 명사 사용 (`/books`, `/loans`, `/events`)
- 계층 구조 명확 (`/events/participation`)
- 상태 전환 API가 명확 (`/loans/{id}/return`, `/book-orders/{id}/approve`)

### 2. Query vs Path Parameter 사용

#### ⚠️ 개선 필요
**임시 인증 방식 (userId를 query parameter로 전달):**
```java
@RequestParam(required = false, defaultValue = "test-user") String userId
```
- BookLoanController, BookOrderController, EventController, EventParticipationController에서 공통 사용
- **문제**: 실제 운영 환경에서는 보안 이슈
- **권장**: JWT 토큰 기반 인증으로 전환 필요

#### ✅ 좋은 사용
- **필터링**: `?type=WORKSHOP`, `?status=APPROVED`
- **페이징**: `?page=0&size=20&sort=createdAt,desc`
- **리소스 식별**: `/{id}`, `/{loanId}`

### 3. Swagger 문서화 품질

#### ✅ 우수 (WorkLogController 참고)
```java
@Operation(summary = "작업 로그 생성", description = """
    ## 개요
    새로운 작업 로그를 Markdown 형식으로 생성합니다.

    ## 주요 파라미터
    - `title`: 작업 로그 제목 (필수)
    - `content`: Markdown 형식의 본문 내용 (필수)

    ## 응답 데이터
    생성된 작업 로그의 전체 정보와 고유 ID를 반환합니다.

    ## 제약사항
    - 제목, 내용, 작성자는 필수 입력 항목입니다
    """)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "작업 로그 생성 성공", ...),
    @ApiResponse(responseCode = "400", description = "잘못된 요청", ...)
})
```

#### ⚠️ 개선 필요
- BookController: Summary만 있음 (Description 없음)
- AuthController: 간단한 Description만 있음
- LoadTestController: @Operation 자체가 없음

### 4. Error Response 문서화

#### ✅ 우수
- WorkLogController: 200, 201, 400, 404, 500 모두 정의

#### ❌ 부족
- 대부분의 컨트롤러: Error responses 정의 없음
- 사용자가 어떤 에러가 발생할 수 있는지 알 수 없음

### 5. Schema 정의

#### ✅ 우수
```java
@Schema(description = "작업 로그 제목",
        example = "Spring Boot API 개발",
        requiredMode = Schema.RequiredMode.REQUIRED)
@NotBlank(message = "제목은 필수입니다")
private String title;
```

#### ⚠️ 개선 필요
- 대부분의 DTO에서 @Schema annotations 부족
- examples 부족

---

## 개선 권장사항

### 우선순위 1: 긴급 (일관성 및 기본 품질)

#### 1.1 LoadTestController URL 일관성
```java
// Before
@RequestMapping("/api/load-test")

// After
@RequestMapping("/api/v1/load-test")
```

#### 1.2 모든 컨트롤러에 Error Responses 추가
```java
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
    @ApiResponse(responseCode = "401", description = "인증 실패"),
    @ApiResponse(responseCode = "404", description = "리소스 없음"),
    @ApiResponse(responseCode = "500", description = "서버 오류")
})
```

### 우선순위 2: 중요 (문서화 품질)

#### 2.1 모든 @Operation에 구조화된 Description 추가
WorkLogController 스타일 적용:
```markdown
## 개요
[1-2 문장으로 API의 비즈니스 목적 설명]

## 주요 파라미터
- `param1`: [파라미터의 비즈니스 의미]

## 응답 데이터
[응답 구조 설명]

## 제약사항
- [인증 요구사항]
- [권한 요구사항]
```

#### 2.2 Schema Annotations 강화
모든 DTO 필드에:
- `description` (한/영 병기)
- `example` (실제 사용 가능한 값)
- `requiredMode` 명시

### 우선순위 3: 개선 (보안 및 아키텍처)

#### 3.1 인증 방식 개선
현재 임시 구현:
```java
@RequestParam(required = false, defaultValue = "test-user") String userId
```

권장 구현:
```java
@AuthenticationPrincipal UserDetails userDetails
```

#### 3.2 중앙화된 Error Response 관리
FastAPI 스타일의 COMMON_RESPONSES 패턴 도입 고려

---

## 정상 플로우 정의

### 1. 도서 대출 플로우
```
1. 사용자 로그인 (Google OAuth)
   POST /api/v1/auth/google/verify

2. 도서 검색
   GET /api/v1/books?title=스프링&page=0&size=20

3. 도서 대출 신청
   POST /api/v1/loans
   Body: { "bookId": 1 }

4. 내 대출 목록 확인
   GET /api/v1/loans?userId=test-user

5. 대출 기간 연장
   POST /api/v1/loans/1/extend

6. 도서 반납
   POST /api/v1/loans/1/return
```

### 2. 도서 주문 플로우
```
1. 도서 주문 요청 생성
   POST /api/v1/book-orders
   Body: {
     "title": "Clean Code",
     "author": "Robert Martin",
     "reason": "팀 학습용"
   }

2. 내 주문 목록 확인
   GET /api/v1/book-orders/my?userId=test-user

3. 관리자가 주문 승인
   POST /api/v1/book-orders/1/approve
   Body: { "comment": "승인합니다" }

4. 관리자가 입고 처리
   POST /api/v1/book-orders/1/receive
```

### 3. 이벤트 참여 플로우
```
1. 이벤트 목록 조회
   GET /api/v1/events?type=TECH_TALK&page=0&size=20

2. 이벤트 상세 조회
   GET /api/v1/events/1

3. 이벤트 참여 신청 (CAS 방식)
   POST /api/v1/events/participation/cas
   Body: {
     "eventId": 1,
     "participantId": "user123",
     "participantName": "홍길동",
     "participantEmail": "hong@example.com"
   }

4. CAS 재시도 횟수 확인
   GET /api/v1/events/participation/cas/retry-count
```

### 4. 작업 로그 플로우
```
1. 작업 로그 생성
   POST /api/v1/work-logs
   Body: {
     "title": "API 개발",
     "content": "# 작업 내용\n\n- 엔드포인트 추가",
     "author": "개발자",
     "tags": ["DEVELOPMENT"]
   }

2. 작업 로그 목록 조회
   GET /api/v1/work-logs?tags=DEVELOPMENT

3. 작업 로그 원본 조회
   GET /api/v1/work-logs/{id}
```

---

## 결론

### 강점
1. ✅ RESTful URL 설계가 전반적으로 양호
2. ✅ PageResponse 사용으로 페이징 일관성 확보
3. ✅ WorkLogController의 우수한 문서화 (참고 표준)
4. ✅ 상태 전환 API가 명확하고 직관적

### 개선 필요
1. ❌ Error responses 문서화 부족
2. ⚠️ Description 구조화 필요
3. ⚠️ Schema annotations 강화 필요
4. ❌ LoadTestController URL 일관성 위배
5. ⚠️ 임시 인증 방식 (운영 전 개선 필요)

### 다음 단계
1. 우선순위 1 개선사항 적용
2. curl 테스트 스크립트 작성 및 실행
3. 실제 Swagger UI에서 검증
4. 인증 방식 개선 계획 수립
