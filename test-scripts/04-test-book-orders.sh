#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Testing Book Order API (/api/v1/book-orders)"

# Check if server is running
check_server || exit 1

# Test 1: Create a book order
print_test_header "Test 1: Create Book Order"

ORDER_DATA='{
  "title": "Domain-Driven Design",
  "author": "Eric Evans",
  "isbn": "9780321125217",
  "publisher": "Addison-Wesley",
  "quantity": 3,
  "reason": "팀 학습용으로 필요합니다"
}'

CREATE_RESPONSE=$(api_request POST "/api/v1/book-orders?userId=${TEST_USER_ID}&username=${TEST_USER_NAME}" \
    "${ORDER_DATA}" \
    "Creating a book order for user ${TEST_USER_NAME}")

ORDER_ID=$(echo "$CREATE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -n "$ORDER_ID" ]; then
    print_success "Order created with ID: ${ORDER_ID}"
else
    print_error "Failed to extract order ID from response"
    ORDER_ID="1"  # Fallback ID for testing
fi

# Test 2: Get my book orders
print_test_header "Test 2: Get My Book Orders"

api_request GET "/api/v1/book-orders/my?userId=${TEST_USER_ID}&page=0&size=20" "" \
    "Getting book orders for user ${TEST_USER_ID}"

# Test 3: Get all book orders (admin)
print_test_header "Test 3: Get All Book Orders (Admin)"

api_request GET "/api/v1/book-orders?page=0&size=20" "" \
    "Getting all book orders"

# Test 4: Get book orders by status
print_test_header "Test 4: Get Book Orders by Status (PENDING)"

api_request GET "/api/v1/book-orders?status=PENDING&page=0&size=20" "" \
    "Getting PENDING book orders"

# Test 5: Get book order details
print_test_header "Test 5: Get Book Order Details"

api_request GET "/api/v1/book-orders/${ORDER_ID}" "" \
    "Getting details for order ${ORDER_ID}"

# Test 6: Approve book order (admin)
print_test_header "Test 6: Approve Book Order (Admin)"

APPROVE_DATA='{
  "comment": "검토 완료. 주문 승인합니다."
}'

api_request POST "/api/v1/book-orders/${ORDER_ID}/approve?userId=${ADMIN_USER_ID}" \
    "${APPROVE_DATA}" \
    "Approving order ${ORDER_ID} by admin"

# Test 7: Mark as received (admin)
print_test_header "Test 7: Mark as Received (Admin)"

api_request POST "/api/v1/book-orders/${ORDER_ID}/receive?userId=${ADMIN_USER_ID}" "" \
    "Marking order ${ORDER_ID} as received"

# Test 8: Create another order for rejection test
print_test_header "Test 8: Create Another Order for Rejection Test"

ORDER_DATA_2='{
  "title": "The Pragmatic Programmer",
  "author": "Andrew Hunt",
  "isbn": "9780135957059",
  "publisher": "Addison-Wesley",
  "quantity": 2,
  "reason": "개인 학습용"
}'

CREATE_RESPONSE_2=$(api_request POST "/api/v1/book-orders?userId=${TEST_USER_ID}&username=${TEST_USER_NAME}" \
    "${ORDER_DATA_2}" \
    "Creating second book order")

ORDER_ID_2=$(echo "$CREATE_RESPONSE_2" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

# Test 9: Reject book order (admin)
print_test_header "Test 9: Reject Book Order (Admin)"

REJECT_DATA='{
  "comment": "예산 부족으로 주문을 거부합니다."
}'

if [ -n "$ORDER_ID_2" ]; then
    api_request POST "/api/v1/book-orders/${ORDER_ID_2}/reject?userId=${ADMIN_USER_ID}" \
        "${REJECT_DATA}" \
        "Rejecting order ${ORDER_ID_2} by admin"
else
    print_error "Second order ID not available, skipping reject test"
fi

# Test 10: Get orders with different statuses
print_test_header "Test 10: Get Approved Orders"

api_request GET "/api/v1/book-orders?status=APPROVED&page=0&size=20" "" \
    "Getting APPROVED book orders"

print_test_header "Test 11: Get Rejected Orders"

api_request GET "/api/v1/book-orders?status=REJECTED&page=0&size=20" "" \
    "Getting REJECTED book orders"

print_test_header "Test 12: Get Received Orders"

api_request GET "/api/v1/book-orders?status=RECEIVED&page=0&size=20" "" \
    "Getting RECEIVED book orders"

# Test 13: Test sorting
print_test_header "Test 13: Test Sorting (by createdAt desc)"

api_request GET "/api/v1/book-orders?page=0&size=20&sort=createdAt,desc" "" \
    "Getting book orders sorted by createdAt descending"

echo ""
print_success "Book Order API tests completed"
echo "Created Order IDs: ${ORDER_ID}, ${ORDER_ID_2}"
print_info "Order ${ORDER_ID}: PENDING -> APPROVED -> RECEIVED"
print_info "Order ${ORDER_ID_2}: PENDING -> REJECTED"
