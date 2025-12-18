# BookOrder API - curl 테스트 명령어 모음

## 서버 실행
```bash
# Spring Boot 서버 시작 (booker-server 디렉토리에서)
./gradlew bootRun
# 또는
./mvnw spring-boot:run
```

서버가 http://localhost:8080 에서 실행됩니다.

---

## 1. POST /api/v1/book-orders - 도서 주문 요청 생성

### 성공 케이스 (모든 필드 포함)
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders?userId=user123&username=홍길동" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Effective Java",
    "author": "Joshua Bloch",
    "publisher": "Addison-Wesley",
    "isbn": "9780134685991"
  }'
```

### 성공 케이스 (필수 필드만)
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders?userId=user123&username=홍길동" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Design Patterns"
  }'
```

### 에러 케이스 - 필수 필드 누락 (400)
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders?userId=test&username=test" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 에러 케이스 - 제목 길이 초과 (422)
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders?userId=test&username=test" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "This is a very long book title that exceeds thirty characters limit"
  }'
```

---

## 2. GET /api/v1/book-orders/my - 내 도서 주문 요청 목록 조회

### 기본 조회 (최신순, 페이지 0, 크기 20)
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders/my?userId=user123&page=0&size=20&sort=createdAt,desc" \
  -H "accept: application/json"
```

### 페이지 1 조회 (두 번째 페이지)
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders/my?userId=user123&page=1&size=10" \
  -H "accept: application/json"
```

---

## 3. GET /api/v1/book-orders - 모든 도서 주문 요청 목록 조회 (관리자용)

### 전체 주문 조회
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders?page=0&size=20&sort=createdAt,desc" \
  -H "accept: application/json"
```

### PENDING 상태만 필터링
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders?status=PENDING&page=0&size=20" \
  -H "accept: application/json"
```

### APPROVED 상태만 필터링
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders?status=APPROVED&page=0&size=20" \
  -H "accept: application/json"
```

### REJECTED 상태만 필터링
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders?status=REJECTED&page=0&size=20" \
  -H "accept: application/json"
```

### RECEIVED 상태만 필터링
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders?status=RECEIVED&page=0&size=20" \
  -H "accept: application/json"
```

---

## 4. GET /api/v1/book-orders/{id} - 도서 주문 요청 상세 조회

### 성공 케이스 (ID 1 조회)
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders/1" \
  -H "accept: application/json"
```

### 에러 케이스 - 존재하지 않는 ID (404)
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders/999" \
  -H "accept: application/json"
```

---

## 5. POST /api/v1/book-orders/{id}/approve - 도서 주문 요청 승인

### 성공 케이스 - 코멘트 포함 승인
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders/1/approve?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": "예산 승인됨. 2주 내 입고 예정"
  }'
```

### 성공 케이스 - 코멘트 없이 승인
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders/1/approve?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": null
  }'
```

### 에러 케이스 - PENDING 아닌 상태 승인 시도 (400)
```bash
# ID 1이 이미 APPROVED 상태일 때
curl -X POST "http://localhost:8080/api/v1/book-orders/1/approve?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": "재승인 시도"
  }'
```

---

## 6. POST /api/v1/book-orders/{id}/reject - 도서 주문 요청 거부

### 성공 케이스 - 예산 부족 사유
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders/2/reject?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": "예산 부족으로 다음 분기에 재검토 예정"
  }'
```

### 성공 케이스 - 중복 도서 사유
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders/2/reject?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": "이미 도서관에 동일 도서가 소장되어 있습니다"
  }'
```

### 성공 케이스 - 부적합 도서 사유
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders/2/reject?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": "도서관 장서 정책에 부합하지 않는 도서입니다"
  }'
```

### 에러 케이스 - PENDING 아닌 상태 거부 시도 (400)
```bash
# ID 2가 이미 REJECTED 상태일 때
curl -X POST "http://localhost:8080/api/v1/book-orders/2/reject?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": "재거부 시도"
  }'
```

---

## 7. POST /api/v1/book-orders/{id}/receive - 도서 입고 처리

### 성공 케이스
```bash
curl -X POST "http://localhost:8080/api/v1/book-orders/1/receive?userId=admin" \
  -H "accept: application/json"
```

### 에러 케이스 - APPROVED 아닌 상태 입고 시도 (400)
```bash
# ID 3이 PENDING 상태일 때
curl -X POST "http://localhost:8080/api/v1/book-orders/3/receive?userId=admin" \
  -H "accept: application/json"
```

---

## 전체 프로세스 테스트 시나리오

### 시나리오 1: 정상 승인 → 입고 프로세스

```bash
# 1. 도서 주문 생성 (PENDING)
ORDER_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/book-orders?userId=user123&username=홍길동" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Architecture",
    "author": "Robert C. Martin",
    "publisher": "Prentice Hall",
    "isbn": "9780134494166"
  }')

# ID 추출
ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id')
echo "생성된 주문 ID: $ORDER_ID"

# 2. 주문 상세 조회 (상태: PENDING 확인)
curl -X GET "http://localhost:8080/api/v1/book-orders/$ORDER_ID" \
  -H "accept: application/json" | jq '.status'

# 3. 주문 승인 (PENDING → APPROVED)
curl -X POST "http://localhost:8080/api/v1/book-orders/$ORDER_ID/approve?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": "예산 승인됨"
  }' | jq '.status'

# 4. 입고 처리 (APPROVED → RECEIVED)
curl -X POST "http://localhost:8080/api/v1/book-orders/$ORDER_ID/receive?userId=admin" \
  -H "accept: application/json" | jq '.status'

# 5. 최종 상태 확인
curl -X GET "http://localhost:8080/api/v1/book-orders/$ORDER_ID" \
  -H "accept: application/json" | jq '.'
```

### 시나리오 2: 거부 프로세스

```bash
# 1. 도서 주문 생성 (PENDING)
REJECT_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/book-orders?userId=user456&username=김철수" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Out of Print Book",
    "author": "Unknown Author"
  }')

# ID 추출
REJECT_ID=$(echo "$REJECT_RESPONSE" | jq -r '.id')
echo "생성된 주문 ID: $REJECT_ID"

# 2. 주문 거부 (PENDING → REJECTED)
curl -X POST "http://localhost:8080/api/v1/book-orders/$REJECT_ID/reject?userId=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "comments": "절판 도서로 구입 불가"
  }' | jq '.'
```

---

## 팁

### 응답을 보기 좋게 포맷팅 (jq 사용)
```bash
curl -X GET "http://localhost:8080/api/v1/book-orders/1" \
  -H "accept: application/json" | jq '.'
```

### HTTP 상태 코드 확인
```bash
curl -w "\nHTTP Status: %{http_code}\n" \
  -X GET "http://localhost:8080/api/v1/book-orders/1" \
  -H "accept: application/json"
```

### 요청과 응답 모두 상세히 보기
```bash
curl -v -X GET "http://localhost:8080/api/v1/book-orders/1" \
  -H "accept: application/json"
```

### 자동화 테스트 스크립트 실행
```bash
# book-order-api-tests.sh 스크립트 실행
cd .claude/test-scripts
./book-order-api-tests.sh
```
