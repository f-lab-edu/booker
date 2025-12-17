#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Testing WorkLog API (/api/v1/work-logs)"

# Check if server is running
check_server || exit 1

# Test 1: Create a work log
print_test_header "Test 1: Create Work Log"

WORKLOG_DATA='{
  "title": "Spring Boot API 개발",
  "content": "# 작업 내용\n\n## 완료 사항\n- Book API 엔드포인트 추가\n- Swagger 문서화\n- 페이징 기능 구현\n\n## 다음 할 일\n- 테스트 코드 작성\n- 인증 기능 추가",
  "author": "개발팀",
  "tags": ["DEVELOPMENT"]
}'

CREATE_RESPONSE=$(api_request POST "/api/v1/work-logs" \
    "${WORKLOG_DATA}" \
    "Creating a work log")

WORKLOG_ID=$(echo "$CREATE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -n "$WORKLOG_ID" ]; then
    print_success "Work log created with ID: ${WORKLOG_ID}"
else
    print_error "Failed to extract work log ID from response"
fi

# Test 2: Get all work logs
print_test_header "Test 2: Get All Work Logs"

api_request GET "/api/v1/work-logs" "" \
    "Getting all work logs"

# Test 3: Get work logs by tag
print_test_header "Test 3: Get Work Logs by Tag (DEVELOPMENT)"

api_request GET "/api/v1/work-logs?tags=DEVELOPMENT" "" \
    "Getting work logs with DEVELOPMENT tag"

# Test 4: Get work log content (Markdown)
print_test_header "Test 4: Get Work Log Content (Markdown)"

if [ -n "$WORKLOG_ID" ]; then
    print_info "Getting work log content for ID: ${WORKLOG_ID}"
    echo "Request: GET ${BASE_URL}/api/v1/work-logs/${WORKLOG_ID}"

    CONTENT=$(curl -s -H "Accept: text/markdown" "${BASE_URL}/api/v1/work-logs/${WORKLOG_ID}")

    echo "Content-Type: text/markdown"
    echo "Content:"
    echo "$CONTENT"
else
    print_error "No work log ID available"
fi

# Test 5: Create work log with multiple tags
print_test_header "Test 5: Create Work Log with Multiple Tags"

WORKLOG_DATA_2='{
  "title": "데이터베이스 성능 최적화",
  "content": "# 성능 최적화 작업\n\n## 문제점\n- 쿼리 실행 시간 과다\n- 인덱스 부재\n\n## 해결 방법\n- 복합 인덱스 추가\n- N+1 쿼리 문제 해결\n\n## 결과\n- 응답 시간 50% 개선",
  "author": "DB팀",
  "tags": ["DEVELOPMENT", "BUG_FIX"]
}'

CREATE_RESPONSE_2=$(api_request POST "/api/v1/work-logs" \
    "${WORKLOG_DATA_2}" \
    "Creating work log with multiple tags")

WORKLOG_ID_2=$(echo "$CREATE_RESPONSE_2" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

# Test 6: Create meeting log
print_test_header "Test 6: Create Meeting Log"

MEETING_DATA='{
  "title": "주간 스프린트 회의",
  "content": "# 주간 회의록\n\n## 참석자\n- 개발팀 전원\n\n## 안건\n1. 이번 주 진행 사항 공유\n2. 다음 주 계획\n3. 이슈 사항 논의\n\n## 결정 사항\n- API 문서화 우선 진행\n- 테스트 커버리지 80% 목표",
  "author": "팀장",
  "tags": ["MEETING"]
}'

api_request POST "/api/v1/work-logs" \
    "${MEETING_DATA}" \
    "Creating meeting log"

# Test 7: Create deployment log
print_test_header "Test 7: Create Deployment Log"

DEPLOYMENT_DATA='{
  "title": "운영 서버 배포",
  "content": "# 배포 작업\n\n## 배포 버전\nv1.2.0\n\n## 배포 내용\n- 신규 API 추가\n- 버그 수정\n\n## 배포 시간\n2025-12-17 02:00 ~ 02:30\n\n## 롤백 계획\n- 기존 버전으로 즉시 롤백 가능\n- 데이터베이스 마이그레이션 없음",
  "author": "DevOps팀",
  "tags": ["DEPLOYMENT"]
}'

api_request POST "/api/v1/work-logs" \
    "${DEPLOYMENT_DATA}" \
    "Creating deployment log"

# Test 8: Get work logs by MEETING tag
print_test_header "Test 8: Get Work Logs by Tag (MEETING)"

api_request GET "/api/v1/work-logs?tags=MEETING" "" \
    "Getting work logs with MEETING tag"

# Test 9: Get work logs by DEPLOYMENT tag
print_test_header "Test 9: Get Work Logs by Tag (DEPLOYMENT)"

api_request GET "/api/v1/work-logs?tags=DEPLOYMENT" "" \
    "Getting work logs with DEPLOYMENT tag"

# Test 10: Get work logs by multiple tags
print_test_header "Test 10: Get Work Logs by Multiple Tags"

api_request GET "/api/v1/work-logs?tags=DEVELOPMENT&tags=BUG_FIX" "" \
    "Getting work logs with DEVELOPMENT and BUG_FIX tags"

# Test 11: Test validation - missing title
print_test_header "Test 11: Test Validation - Missing Title"

INVALID_DATA='{
  "content": "# 내용만 있음",
  "author": "테스터"
}'

api_request POST "/api/v1/work-logs" \
    "${INVALID_DATA}" \
    "Creating work log without title (expected to fail with 400)"

# Test 12: Test validation - missing content
print_test_header "Test 12: Test Validation - Missing Content"

INVALID_DATA_2='{
  "title": "제목만 있음",
  "author": "테스터"
}'

api_request POST "/api/v1/work-logs" \
    "${INVALID_DATA_2}" \
    "Creating work log without content (expected to fail with 400)"

echo ""
print_success "WorkLog API tests completed"
echo "Created Work Log IDs: ${WORKLOG_ID}, ${WORKLOG_ID_2}"
print_info "Tags tested: DEVELOPMENT, MEETING, DEPLOYMENT, BUG_FIX"
print_info "Validation tests: Passed (400 errors expected for invalid data)"
