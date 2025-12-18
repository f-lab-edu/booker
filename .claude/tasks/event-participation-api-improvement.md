# Event Participation API URL Design & Documentation Improvement Plan

## Task Overview
Review and improve the Event Participation APIs to follow RESTful best practices and enhance Swagger documentation.

## Current API Structure Analysis

### Current Endpoints
```
GET    /api/v1/events/participation/cas/retry-count          - CAS 재시도 횟수 조회
POST   /api/v1/events/participation/synchronized             - 이벤트 참여 신청 (Synchronized)
POST   /api/v1/events/participation/cas                      - 이벤트 참여 신청 (CAS)
POST   /api/v1/events/participation/cas/reset-retry-count    - CAS 재시도 횟수 초기화
```

### Identified Issues

#### 1. URL Structure Problems
- **Issue**: Implementation details ("synchronized", "cas") are exposed in the URL path
  - **Why it's wrong**: RESTful APIs should be implementation-agnostic. The concurrency control mechanism (synchronized vs CAS) is an internal implementation detail, not a resource characteristic
  - **Impact**: If we add more concurrency strategies (e.g., Redis distributed lock, database row lock), we'd need new URLs, breaking backward compatibility

- **Issue**: Retry count management URLs are nested incorrectly
  - Current: `/api/v1/events/participation/cas/retry-count`
  - **Why it's wrong**: "retry-count" is not a sub-resource of "participation". It's a monitoring/admin resource
  - **Impact**: Confusing resource hierarchy, mixing business logic with monitoring concerns

#### 2. RESTful Best Practices Violations
- **Resource vs Action**: URLs should represent resources, not actions
  - ✅ Good: `/api/v1/events/{eventId}/participations` (resource)
  - ❌ Bad: `/api/v1/events/participation/synchronized` (action + implementation)

- **Nested Resource Structure**: Missing event ID in URL
  - Current: Event ID is in request body
  - Best Practice: Event should be in URL path as it's the parent resource
  - Better: `/api/v1/events/{eventId}/participations`

#### 3. Documentation Gaps
Comparing with BookLoanController (reference standard):
- ✅ BookLoan has: Structured descriptions (개요, 주요 파라미터, 응답 데이터, 제약사항)
- ❌ EventParticipation missing: Complete request/response examples, error responses, field-level descriptions
- ✅ BookLoan has: Multiple response examples (success cases, waiting list)
- ❌ EventParticipation missing: Error response documentation (400, 404, 409, 422)
- ✅ BookLoan has: Complete @ApiResponses with examples
- ❌ EventParticipation missing: Comprehensive error documentation

## Proposed Improvements

### Phase 1: URL Design Refactoring

#### Option A: Query Parameter Approach (RECOMMENDED)
Move concurrency strategy to query parameter:

```
POST   /api/v1/events/{eventId}/participations?strategy=synchronized
POST   /api/v1/events/{eventId}/participations?strategy=cas
POST   /api/v1/events/{eventId}/participations                       (default strategy)

GET    /api/v1/events/{eventId}/participations/{participationId}
GET    /api/v1/events/{eventId}/participations                       (list with filters)
DELETE /api/v1/events/{eventId}/participations/{participationId}

# Monitoring endpoints (separate concern)
GET    /api/v1/monitoring/cas/retry-count
POST   /api/v1/monitoring/cas/retry-count/reset
```

**Advantages:**
- ✅ RESTful resource-based URLs
- ✅ Event ID clearly shows parent-child relationship
- ✅ Implementation detail (strategy) is optional
- ✅ Easy to add new strategies without URL changes
- ✅ Monitoring concerns separated from business logic
- ✅ Follows BookLoan pattern: `/api/v1/loans/{loanId}`

**Disadvantages:**
- ⚠️ Requires controller refactoring
- ⚠️ Query parameter might be ignored by clients (need clear documentation)

#### Option B: Header-Based Approach
Use HTTP header for concurrency strategy:

```
POST   /api/v1/events/{eventId}/participations
Header: X-Concurrency-Strategy: cas | synchronized

GET    /api/v1/events/{eventId}/participations/{participationId}
```

**Advantages:**
- ✅ Clean URLs
- ✅ Strategy is truly optional (can have server default)
- ✅ More aligned with HTTP standards (Accept headers, etc.)

**Disadvantages:**
- ❌ Less discoverable in Swagger UI
- ❌ Cannot test easily with browser
- ❌ Less common pattern, may confuse API consumers

#### Option C: Minimal Change (Path Parameter)
Keep current structure but improve resource naming:

```
POST   /api/v1/events/{eventId}/participations/synchronized
POST   /api/v1/events/{eventId}/participations/cas
```

**Advantages:**
- ✅ Minimal code changes
- ✅ Easy to test in Swagger

**Disadvantages:**
- ❌ Still exposes implementation detail
- ❌ Scalability issues with more strategies
- ❌ Not truly RESTful

### Phase 2: Swagger Documentation Enhancement

Following BookLoanController pattern, add:

#### 1. Structured Operation Descriptions
```java
@Operation(summary = "이벤트 참여 신청", description = """
    ## 개요
    이벤트에 참여를 신청합니다. 최대 참여 인원 초과 시 자동으로 대기 목록에 등록됩니다.

    ## 주요 파라미터
    - `eventId`: 참여할 이벤트의 고유 ID (Path Parameter, 필수)
    - `strategy`: 동시성 제어 전략 (Query Parameter, 선택)
      - `synchronized`: Java synchronized 방식 (기본값, 안전하지만 성능 제한)
      - `cas`: Compare-And-Swap 방식 (높은 처리량, 낙관적 잠금)
    - `memberId`: 참여자 회원 ID (Request Body, 필수)

    ## 응답 데이터
    - `participationId`: 참여 신청 ID
    - `status`: 참여 상태 (CONFIRMED: 확정, WAITING: 대기)
    - `waitingNumber`: 대기 순서 (WAITING 상태인 경우에만 표시)
    - `message`: 처리 결과 메시지

    ## 동시성 제어 전략
    ### Synchronized (기본값)
    - Java synchronized 키워드 사용
    - 한 번에 하나의 스레드만 처리
    - 안정적이지만 처리량 제한

    ### CAS (Compare-And-Swap)
    - Optimistic Lock 사용
    - 충돌 시 자동 재시도 (최대 10회)
    - 높은 동시 처리 성능
    - 충돌이 적은 환경에서 최적

    ## 제약사항
    - 인증 필요: 현재는 test-user로 테스트 중
    - 동일 회원은 동일 이벤트에 중복 신청 불가
    - 최대 참여자 수 초과 시 자동으로 대기 목록 등록
    - 이벤트 종료 후에는 신청 불가
    """)
```

#### 2. Complete Request/Response Examples
```java
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "참여 신청 성공",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = EventParticipationDto.Response.class),
            examples = {
                @ExampleObject(name = "즉시 확정", summary = "참여 인원 여유로 즉시 확정", value = """
                    {
                      "participationId": 1,
                      "status": "CONFIRMED",
                      "waitingNumber": null,
                      "message": "이벤트 참여가 확정되었습니다."
                    }
                    """),
                @ExampleObject(name = "대기 등록", summary = "최대 인원 초과로 대기 목록 등록", value = """
                    {
                      "participationId": 2,
                      "status": "WAITING",
                      "waitingNumber": 3,
                      "message": "대기 목록에 등록되었습니다. 현재 대기 순서: 3번"
                    }
                    """)
            })),
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "error": "Bad Request",
                  "message": "이벤트가 이미 종료되었습니다."
                }
                """))),
    @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "error": "Not Found",
                  "message": "이벤트를 찾을 수 없습니다: 999"
                }
                """))),
    @ApiResponse(responseCode = "409", description = "이미 참여 신청한 이벤트",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "error": "Conflict",
                  "message": "이미 참여 신청한 이벤트입니다."
                }
                """))),
    @ApiResponse(responseCode = "422", description = "유효성 검증 실패",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "error": "Validation Failed",
                  "details": [
                    {
                      "field": "memberId",
                      "message": "회원 ID는 필수입니다"
                    }
                  ]
                }
                """))),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
})
```

#### 3. Enhanced DTO Schema Documentation
```java
@Schema(name = "EventParticipationRequest", description = "이벤트 참여 신청 요청")
public static class Request {
    @Schema(
        description = "참여할 이벤트의 고유 ID - 실제 DB에 존재하는 이벤트 ID를 입력하세요",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "이벤트 ID는 필수입니다")
    private Long eventId;

    @Schema(
        description = "참여자 회원 ID - 이메일 형식 또는 UUID 형식",
        example = "member001",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 3,
        maxLength = 100
    )
    @NotBlank(message = "회원 ID는 필수입니다")
    private String memberId;

    @Schema(
        description = "참여자 이름",
        example = "김철수",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "참여자 이름은 필수입니다")
    private String memberName;

    @Schema(
        description = "참여자 이메일 주소 - 참여 확정/대기 알림 전송용",
        example = "member001@test.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
        format = "email"
    )
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수입니다")
    private String memberEmail;
}

@Schema(name = "EventParticipationResponse", description = "이벤트 참여 신청 응답")
public static class Response {
    @Schema(description = "참여 신청 고유 ID", example = "1")
    private Long participationId;

    @Schema(
        description = "참여 상태 - CONFIRMED(확정), WAITING(대기), CANCELLED(취소)",
        example = "CONFIRMED",
        allowableValues = {"CONFIRMED", "WAITING", "CANCELLED"}
    )
    private String status;

    @Schema(
        description = "대기 순서 번호 - WAITING 상태인 경우에만 표시됨",
        example = "3",
        nullable = true
    )
    private Integer waitingNumber;

    @Schema(
        description = "처리 결과 메시지 - 사용자에게 표시할 친화적인 메시지",
        example = "이벤트 참여가 확정되었습니다."
    )
    private String message;
}
```

### Phase 3: Testing & Validation

#### Test Cases to Document
1. **정상 참여 신청** (CONFIRMED)
   ```bash
   curl -X POST "http://localhost:8080/api/v1/events/1/participations?strategy=cas" \
     -H "Content-Type: application/json" \
     -d '{
       "eventId": 1,
       "memberId": "member001",
       "memberName": "김철수",
       "memberEmail": "member001@test.com"
     }'
   ```

2. **대기 목록 등록** (WAITING)
3. **중복 신청 오류** (409 Conflict)
4. **존재하지 않는 이벤트** (404 Not Found)
5. **유효성 검증 실패** (422 Unprocessable Entity)
6. **CAS 재시도 카운트 조회**
7. **재시도 카운트 초기화**

## Implementation Phases

### Phase 1: Analysis & Planning (Current)
- [x] Analyze current API structure
- [x] Identify issues and violations
- [x] Propose improvement options
- [ ] **USER APPROVAL NEEDED** - Choose URL design approach

### Phase 2: URL Design (After Approval)
- [ ] Refactor controller endpoints
- [ ] Update service layer if needed
- [ ] Move monitoring endpoints to separate controller
- [ ] Update route registration

### Phase 3: Documentation Enhancement
- [ ] Add structured descriptions to all operations
- [ ] Complete @ApiResponses with all status codes
- [ ] Add request/response examples
- [ ] Enhance DTO schema documentation
- [ ] Add validation annotations with custom messages

### Phase 4: Testing & Validation
- [ ] Test all endpoints with curl
- [ ] Verify Swagger UI displays correctly
- [ ] Document test cases with examples
- [ ] Create integration test suite

### Phase 5: Review & Finalization
- [ ] Compare with BookLoanController standard
- [ ] Ensure consistency across all endpoints
- [ ] Update API documentation
- [ ] Commit changes

## Decision Points

### 🔴 DECISION REQUIRED: URL Design Approach
Please review the three options above and choose:
- **Option A (RECOMMENDED)**: Query parameter strategy
- **Option B**: Header-based strategy
- **Option C**: Minimal change with path parameter

**Recommendation**: Option A
- Most RESTful
- Best scalability
- Follows industry standards
- Clear separation of concerns
- Matches BookLoan pattern

## Success Criteria
- [ ] All endpoints follow RESTful principles
- [ ] Implementation details hidden from URLs
- [ ] Complete Swagger documentation matching BookLoanController quality
- [ ] All error cases documented with examples
- [ ] Request/Response DTOs fully annotated
- [ ] Curl test examples provided
- [ ] Monitoring endpoints properly separated

## References
- **Standard Reference**: `/booker-server/src/main/java/com/bookerapp/core/presentation/controller/BookLoanController.java`
- **Current Implementation**: `/booker-server/src/main/java/com/bookerapp/core/presentation/controller/EventParticipationController.java`
- **Entity Models**: `Event.java`, `EventParticipation.java`
- **DTOs**: `EventParticipationDto.java`

## Next Steps
1. **Await user decision** on URL design approach
2. Proceed with chosen implementation
3. Complete documentation enhancement
4. Test and validate all endpoints
5. Create comprehensive curl test suite
