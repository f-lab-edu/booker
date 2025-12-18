#!/bin/bash

# BookOrder API 테스트 스크립트
# 사용법: ./book-order-api-tests.sh
# 서버가 http://localhost:8080 에서 실행 중이어야 합니다

BASE_URL="http://localhost:8080/api/v1/book-orders"

echo "================================"
echo "BookOrder API 테스트 시작"
echo "================================"
echo ""

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. POST /api/v1/book-orders - 도서 주문 요청 생성
echo -e "${YELLOW}1. POST /book-orders - 도서 주문 요청 생성${NC}"
echo "Request:"
cat << 'EOF'
{
  "title": "Effective Java",
  "author": "Joshua Bloch",
  "publisher": "Addison-Wesley",
  "isbn": "9780134685991"
}
EOF

RESPONSE=$(curl -s -X POST "$BASE_URL?userId=user123&username=홍길동" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Effective Java",
    "author": "Joshua Bloch",
    "publisher": "Addison-Wesley",
    "isbn": "9780134685991"
  }')

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ 성공${NC}"
  echo "Response:"
  echo "$RESPONSE" | jq '.'
  # ID 추출 (다음 테스트에 사용)
  ORDER_ID=$(echo "$RESPONSE" | jq -r '.id')
  echo "생성된 주문 ID: $ORDER_ID"
else
  echo -e "${RED}✗ 실패${NC}"
fi
echo ""

# 2. GET /api/v1/book-orders/my - 내 도서 주문 요청 목록 조회
echo -e "${YELLOW}2. GET /book-orders/my - 내 도서 주문 요청 목록 조회${NC}"
curl -s -X GET "$BASE_URL/my?userId=user123&page=0&size=20&sort=createdAt,desc" \
  -H "accept: application/json" | jq '.'
echo ""

# 3. GET /api/v1/book-orders - 모든 도서 주문 요청 목록 조회 (관리자용)
echo -e "${YELLOW}3. GET /book-orders - 모든 도서 주문 요청 목록 조회${NC}"
curl -s -X GET "$BASE_URL?page=0&size=20&sort=createdAt,desc" \
  -H "accept: application/json" | jq '.'
echo ""

# 3-1. GET /api/v1/book-orders?status=PENDING - PENDING 필터링
echo -e "${YELLOW}3-1. GET /book-orders?status=PENDING - 검토 대기 주문만 조회${NC}"
curl -s -X GET "$BASE_URL?status=PENDING&page=0&size=20" \
  -H "accept: application/json" | jq '.'
echo ""

# 4. GET /api/v1/book-orders/{id} - 도서 주문 요청 상세 조회
if [ -n "$ORDER_ID" ]; then
  echo -e "${YELLOW}4. GET /book-orders/$ORDER_ID - 도서 주문 요청 상세 조회${NC}"
  curl -s -X GET "$BASE_URL/$ORDER_ID" \
    -H "accept: application/json" | jq '.'
  echo ""
fi

# 5. POST /api/v1/book-orders/{id}/approve - 도서 주문 요청 승인
if [ -n "$ORDER_ID" ]; then
  echo -e "${YELLOW}5. POST /book-orders/$ORDER_ID/approve - 도서 주문 요청 승인${NC}"
  echo "Request:"
  cat << 'EOF'
{
  "comments": "예산 승인됨. 2주 내 입고 예정"
}
EOF

  curl -s -X POST "$BASE_URL/$ORDER_ID/approve?userId=admin" \
    -H "Content-Type: application/json" \
    -d '{
      "comments": "예산 승인됨. 2주 내 입고 예정"
    }' | jq '.'
  echo ""
fi

# 6. POST /api/v1/book-orders/{id}/reject - 도서 주문 요청 거부 (새로운 주문 생성 후 테스트)
echo -e "${YELLOW}6. POST /book-orders - 거부 테스트용 주문 생성${NC}"
REJECT_RESPONSE=$(curl -s -X POST "$BASE_URL?userId=user456&username=김철수" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Out of Stock Book",
    "author": "Test Author"
  }')

REJECT_ORDER_ID=$(echo "$REJECT_RESPONSE" | jq -r '.id')
echo "거부 테스트용 주문 ID: $REJECT_ORDER_ID"
echo ""

if [ -n "$REJECT_ORDER_ID" ] && [ "$REJECT_ORDER_ID" != "null" ]; then
  echo -e "${YELLOW}6-1. POST /book-orders/$REJECT_ORDER_ID/reject - 도서 주문 요청 거부${NC}"
  echo "Request:"
  cat << 'EOF'
{
  "comments": "예산 부족으로 다음 분기에 재검토 예정"
}
EOF

  curl -s -X POST "$BASE_URL/$REJECT_ORDER_ID/reject?userId=admin" \
    -H "Content-Type: application/json" \
    -d '{
      "comments": "예산 부족으로 다음 분기에 재검토 예정"
    }' | jq '.'
  echo ""
fi

# 7. POST /api/v1/book-orders/{id}/receive - 도서 입고 처리
if [ -n "$ORDER_ID" ]; then
  echo -e "${YELLOW}7. POST /book-orders/$ORDER_ID/receive - 도서 입고 처리${NC}"
  curl -s -X POST "$BASE_URL/$ORDER_ID/receive?userId=admin" \
    -H "accept: application/json" | jq '.'
  echo ""
fi

# 에러 케이스 테스트
echo -e "${YELLOW}=== 에러 케이스 테스트 ===${NC}"
echo ""

# 8. 404 - 존재하지 않는 ID
echo -e "${YELLOW}8. GET /book-orders/999 - 존재하지 않는 ID (404 Expected)${NC}"
curl -s -X GET "$BASE_URL/999" \
  -H "accept: application/json" | jq '.'
echo ""

# 9. 400 - 필수 필드 누락
echo -e "${YELLOW}9. POST /book-orders - 필수 필드 누락 (400 Expected)${NC}"
echo "Request: { } (title 누락)"
curl -s -X POST "$BASE_URL?userId=test&username=test" \
  -H "Content-Type: application/json" \
  -d '{}' | jq '.'
echo ""

# 10. 422 - 유효성 검증 실패
echo -e "${YELLOW}10. POST /book-orders - 제목 길이 초과 (422 Expected)${NC}"
LONG_TITLE="This is a very long book title that exceeds the maximum allowed length of 30 characters"
curl -s -X POST "$BASE_URL?userId=test&username=test" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"$LONG_TITLE\"
  }" | jq '.'
echo ""

echo "================================"
echo "BookOrder API 테스트 완료"
echo "================================"
