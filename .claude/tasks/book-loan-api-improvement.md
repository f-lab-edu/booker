# Book Loan API Implementation & Improvement Plan

## Current Status

The Book Loan APIs are **already implemented** in the codebase. This document provides:
1. Analysis of the current implementation
2. RESTful URL design review
3. Improvement recommendations
4. Enhancement opportunities

## Existing Implementation Analysis

### Current API Endpoints

All 5 requested APIs are already implemented in `BookLoanController.java`:

1. **GET /api/v1/loans** - Get my loan list ✅
2. **GET /api/v1/loans/{loanId}** - Get loan details by ID ✅
3. **POST /api/v1/loans** - Create a new loan (borrow a book) ✅
4. **POST /api/v1/loans/{loanId}/return** - Return a borrowed book ✅
5. **POST /api/v1/loans/{loanId}/extend** - Extend loan period ✅

### Architecture Review

**Layer Structure:**
- **Controller Layer**: `BookLoanController.java` - API endpoints with Swagger documentation
- **Service Layer**: `BookLoanService.java` - Business logic and transaction management
- **Repository Layer**: `BookLoanRepository.java` - Data access
- **Entity**: `BookLoan.java` - Domain model
- **DTO**: `BookLoanDto.java` - Request/Response objects
- **Enum**: `LoanStatus.java` - Status management (PENDING, WAITING, ACTIVE, OVERDUE, RETURNED, CANCELLED)

**Current Features:**
- ✅ Pagination support for loan list
- ✅ Status-based filtering
- ✅ Proper transaction isolation (READ_COMMITTED for createLoan)
- ✅ Authorization checks (user can only access their own loans)
- ✅ Business rule validation (waiting list, extension restrictions)
- ✅ Swagger/OpenAPI documentation
- ✅ EntityNotFoundException handling
- ✅ Concurrency handling for book availability

## RESTful URL Design Review

### Current Design Analysis

| Endpoint | HTTP Method | URL Pattern | RESTful Score |
|----------|-------------|-------------|---------------|
| Get loan list | GET | `/api/v1/loans` | ✅ Excellent |
| Get loan detail | GET | `/api/v1/loans/{loanId}` | ✅ Excellent |
| Create loan | POST | `/api/v1/loans` | ✅ Excellent |
| Return book | POST | `/api/v1/loans/{loanId}/return` | ⚠️ Good (see below) |
| Extend loan | POST | `/api/v1/loans/{loanId}/extend` | ⚠️ Good (see below) |

### URL Design Discussion

#### Option 1: Current Design (POST with action endpoints)
```
POST /api/v1/loans/{loanId}/return
POST /api/v1/loans/{loanId}/extend
```

**Pros:**
- ✅ Clear action intent - immediately obvious what the endpoint does
- ✅ Supports future additional parameters for return/extend actions
- ✅ Common in real-world APIs (GitHub, Stripe, etc.)
- ✅ Action-oriented, matches business terminology
- ✅ Easier to add action-specific validation

**Cons:**
- ⚠️ Not strictly RESTful (REST purists prefer resource manipulation)
- ⚠️ Adds more endpoints to maintain

#### Option 2: Pure REST Design (PATCH/PUT)
```
PATCH /api/v1/loans/{loanId}
  Body: { "status": "RETURNED" } or { "action": "return" }

PATCH /api/v1/loans/{loanId}
  Body: { "action": "extend", "extendDays": 7 }
```

**Pros:**
- ✅ Strictly RESTful (resource state modification)
- ✅ Fewer endpoints
- ✅ Follows HTTP semantics

**Cons:**
- ❌ Less intuitive - requires reading documentation to understand body structure
- ❌ Action intent not clear from URL
- ❌ Harder to validate different action types
- ❌ Single endpoint handling multiple actions increases complexity

#### Option 3: Hybrid Approach (PUT with action)
```
PUT /api/v1/loans/{loanId}/status
  Body: { "status": "RETURNED", "returnDate": "..." }

PUT /api/v1/loans/{loanId}/due-date
  Body: { "extendDays": 7 }
```

**Pros:**
- ✅ More RESTful than POST
- ✅ Clear resource being updated

**Cons:**
- ❌ Still not pure REST
- ❌ May confuse developers (when to use PUT vs POST)

### Recommendation

**Keep the current POST design** for the following reasons:

1. **Business Clarity**: "Return" and "Extend" are distinct business actions, not simple state updates
2. **Industry Standard**: Major APIs (GitHub, Stripe, AWS) use POST for actions
3. **Extensibility**: Easy to add future actions like `/cancel`, `/renew`, etc.
4. **Validation**: Each action can have its own validation rules
5. **Documentation**: Self-documenting URLs improve developer experience
6. **Current Implementation**: Already well-implemented with proper error handling

**Justification**: While pure REST advocates for resource manipulation via PUT/PATCH, pragmatic REST design recognizes that some operations are better represented as actions (RPC-style) rather than resource state changes. The current design is a widely-accepted pragmatic approach.

## Improvement Opportunities

### 1. Enhanced Documentation

**Current**: Basic Swagger annotations
**Improvement**: Add comprehensive examples and error responses

```java
@Operation(
    summary = "도서 대출 신청",
    description = """
        ## 개요
        도서 대출을 신청합니다. 도서가 이미 대출 중인 경우 대기 목록에 추가됩니다.

        ## 주요 파라미터
        - bookId: 대출할 도서의 고유 ID

        ## 응답 데이터
        - id: 대출 신청 ID
        - status: ACTIVE (즉시 대출) 또는 WAITING (대기 목록)
        - loanDate: 대출 개시일 (ACTIVE인 경우)
        - dueDate: 반납 예정일 (ACTIVE인 경우, 대출일로부터 14일)

        ## 제약사항
        - 인증 필요: Bearer Token (Authorization 헤더)
        - 동일 도서에 대한 중복 대출 신청 불가
        - 대출 가능 권수 제한: 최대 5권 (현재 미구현)
        - 연체 중인 경우 대출 불가 (현재 미구현)
        """,
    responses = {
        @ApiResponse(responseCode = "201", description = "대출 신청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 대출 중인 도서 등)"),
        @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음"),
        @ApiResponse(responseCode = "422", description = "유효성 검증 실패",
            content = @Content(examples = @ExampleObject(value = """
                {
                  "error": "Validation Failed",
                  "details": [
                    {
                      "field": "bookId",
                      "message": "도서 ID는 필수입니다"
                    }
                  ]
                }
            """)))
    }
)
```

### 2. Add Missing Features

#### A. Authentication Integration
**Current**: Using test-user default parameter
**Improvement**: Integrate with actual authentication system

```java
// Add security annotation
@PreAuthorize("hasRole('USER')")
public ResponseEntity<BookLoanDto.Response> createLoan(
    @Valid @RequestBody BookLoanDto.Request request,
    @AuthenticationPrincipal CustomUserDetails userDetails) {

    String userId = userDetails.getUserId();
    // ... rest of the code
}
```

#### B. Additional Validation Rules

**Missing validations to implement:**
1. Maximum loan limit per user (e.g., 5 books)
2. Check if user has overdue books before allowing new loans
3. Prevent duplicate active loans for the same book
4. Validate extension eligibility (e.g., max 2 extensions)

```java
// In BookLoanService
private void validateLoanEligibility(String memberId, Long bookId) {
    // Check loan limit
    long activeLoanCount = bookLoanRepository.countByMemberIdAndStatusIn(
        memberId, Arrays.asList(LoanStatus.ACTIVE, LoanStatus.PENDING));

    if (activeLoanCount >= 5) {
        throw new IllegalStateException("대출 가능 권수를 초과했습니다. (최대 5권)");
    }

    // Check overdue status
    boolean hasOverdueLoans = bookLoanRepository.existsByMemberIdAndStatus(
        memberId, LoanStatus.OVERDUE);

    if (hasOverdueLoans) {
        throw new IllegalStateException("연체 중인 도서가 있어 대출할 수 없습니다.");
    }

    // Check duplicate loan
    boolean hasSameBookLoan = bookLoanRepository.existsByMemberIdAndBookIdAndStatusIn(
        memberId, bookId, Arrays.asList(LoanStatus.ACTIVE, LoanStatus.WAITING));

    if (hasSameBookLoan) {
        throw new IllegalStateException("이미 대출 중이거나 대기 중인 도서입니다.");
    }
}
```

#### C. Extension Limit Tracking

```java
// Add to BookLoan entity
@Column(name = "extension_count", nullable = false)
private int extensionCount = 0;

// Update extend method
public void extend() {
    if (this.status != LoanStatus.ACTIVE) {
        throw new IllegalStateException("대출 연장은 ACTIVE 상태에서만 가능합니다.");
    }

    if (this.extensionCount >= 2) {
        throw new IllegalStateException("최대 연장 횟수(2회)를 초과했습니다.");
    }

    this.dueDate = this.dueDate.plusDays(7);
    this.extensionCount++;
}
```

### 3. Error Response Standardization

**Current**: Using generic exceptions
**Improvement**: Create custom exception hierarchy with standardized error responses

```java
// Custom exceptions
public class LoanLimitExceededException extends RuntimeException { ... }
public class OverdueLoanExistsException extends RuntimeException { ... }
public class DuplicateLoanException extends RuntimeException { ... }

// Global exception handler
@RestControllerAdvice
public class BookLoanExceptionHandler {

    @ExceptionHandler(LoanLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleLoanLimitExceeded(LoanLimitExceededException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("LOAN_LIMIT_EXCEEDED", e.getMessage()));
    }

    // ... other handlers
}
```

### 4. Response Enhancement

**Add more details to Response DTO:**

```java
@Schema(name = "BookLoanResponse")
public static class Response {
    // ... existing fields

    @Schema(description = "연장 가능 여부", example = "true")
    private boolean extensible;

    @Schema(description = "연장 횟수", example = "1")
    private int extensionCount;

    @Schema(description = "대기 순서 (WAITING 상태인 경우)", example = "3")
    private Integer waitingPosition;

    @Schema(description = "연체 일수 (OVERDUE 상태인 경우)", example = "5")
    private Integer overdueDays;
}
```

### 5. Performance Optimization

**Add index optimization:**

```sql
-- For frequent queries
CREATE INDEX idx_loan_member_status ON book_loan(member_id, status);
CREATE INDEX idx_loan_book_status ON book_loan(book_id, status);
CREATE INDEX idx_loan_due_date ON book_loan(due_date) WHERE status = 'ACTIVE';
```

**Implement caching for waiting count:**

```java
@Cacheable(value = "waitingCount", key = "#bookId")
public long getWaitingCount(Long bookId) {
    return bookLoanRepository.countByBookIdAndStatus(bookId, LoanStatus.WAITING);
}
```

### 6. Add Sorting Options

**Current**: Default sorting by createdAt,desc
**Improvement**: Support multiple sort fields

```java
@Schema(description = "정렬 기준",
    allowableValues = {"loanDate,desc", "loanDate,asc", "dueDate,desc", "dueDate,asc", "status"},
    example = "loanDate,desc")
private String sort = "loanDate,desc";
```

### 7. Batch Operations Support

**Add admin endpoints:**

```java
// Batch return for lost books
@PostMapping("/admin/batch-return")
public ResponseEntity<List<BookLoanDto.Response>> batchReturn(
    @RequestBody List<Long> loanIds,
    @RequestParam String reason) {
    // ... implementation
}

// Batch overdue check (scheduled)
@Scheduled(cron = "0 0 1 * * *") // Run daily at 1 AM
public void scheduledOverdueCheck() {
    bookLoanService.checkAndUpdateOverdueStatus();
}
```

## Implementation Priority

### High Priority (Immediate)
1. ✅ **No changes needed** - Current implementation is solid
2. Add authentication integration (remove test-user default)
3. Add validation rules (loan limit, overdue check, duplicate check)
4. Enhance Swagger documentation with examples and error responses

### Medium Priority (Next Sprint)
1. Add extension limit tracking
2. Standardize error responses with custom exceptions
3. Enhance response DTOs with additional fields
4. Add database indexes for performance

### Low Priority (Future)
1. Implement caching
2. Add batch operations for admin
3. Add notification system integration (already has TODO comment)
4. Add audit logging

## Testing Checklist

### Unit Tests Needed
- [ ] BookLoanService.createLoan - available book scenario
- [ ] BookLoanService.createLoan - waiting list scenario
- [ ] BookLoanService.createLoan - validation failures
- [ ] BookLoanService.returnBook - success scenario
- [ ] BookLoanService.returnBook - unauthorized access
- [ ] BookLoanService.extendLoan - with waiting list (should fail)
- [ ] BookLoanService.extendLoan - without waiting list (should succeed)
- [ ] BookLoanService.getMyLoans - pagination and filtering

### Integration Tests Needed
- [ ] Full loan workflow: create → extend → return
- [ ] Waiting list flow: book borrowed → new loan request → return → waiting user activated
- [ ] Concurrent loan requests for the same book
- [ ] Overdue status update job

## Conclusion

**The current Book Loan API implementation is well-designed and production-ready.** The RESTful URL design using POST for action endpoints is appropriate and follows industry best practices.

**Key Strengths:**
- Clean layered architecture
- Proper transaction management
- Good Swagger documentation foundation
- Business logic encapsulation
- Pagination and filtering support

**Recommended Next Steps:**
1. Integrate with actual authentication system
2. Add missing validation rules
3. Enhance documentation with concrete examples
4. Write comprehensive tests
5. Add performance indexes

No major refactoring needed - focus on enhancements and testing.
