#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Testing Book Loan API (/api/v1/loans)"

# Check if server is running
check_server || exit 1

# Prerequisite: Ensure we have a book to borrow
print_info "Prerequisite: Getting a book ID for testing"
BOOKS_RESPONSE=$(curl -s "${BASE_URL}/api/v1/books?page=0&size=1")
BOOK_ID=$(echo "$BOOKS_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('data', [{}])[0].get('id', '1'))" 2>/dev/null || echo "1")
print_info "Using Book ID: ${BOOK_ID}"

# Test 1: Create a loan (borrow a book)
print_test_header "Test 1: Create Book Loan"

LOAN_DATA="{
  \"bookId\": ${BOOK_ID}
}"

CREATE_RESPONSE=$(api_request POST "/api/v1/loans?userId=${TEST_USER_ID}" "${LOAN_DATA}" "Creating a book loan for user ${TEST_USER_ID}")
LOAN_ID=$(echo "$CREATE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -n "$LOAN_ID" ]; then
    print_success "Loan created with ID: ${LOAN_ID}"
else
    print_error "Failed to extract loan ID from response"
    LOAN_ID="1"  # Fallback ID for testing
fi

# Test 2: Get my loans
print_test_header "Test 2: Get My Loans"

api_request GET "/api/v1/loans?userId=${TEST_USER_ID}" "" "Getting loans for user ${TEST_USER_ID}"

# Test 3: Get loan by ID
print_test_header "Test 3: Get Loan Details"

api_request GET "/api/v1/loans/${LOAN_ID}?userId=${TEST_USER_ID}" "" "Getting loan details for loan ${LOAN_ID}"

# Test 4: Get my loans with status filter
print_test_header "Test 4: Get My Loans with Status Filter"

api_request GET "/api/v1/loans?userId=${TEST_USER_ID}&status=ACTIVE" "" "Getting ACTIVE loans for user ${TEST_USER_ID}"

# Test 5: Extend loan
print_test_header "Test 5: Extend Loan"

api_request POST "/api/v1/loans/${LOAN_ID}/extend?userId=${TEST_USER_ID}" "" "Extending loan ${LOAN_ID}"

# Test 6: Try to extend again (should fail if there are waiters)
print_test_header "Test 6: Try to Extend Again"

api_request POST "/api/v1/loans/${LOAN_ID}/extend?userId=${TEST_USER_ID}" "" "Attempting to extend loan again (may fail)"

# Test 7: Return book (commented out to preserve test data)
print_test_header "Test 7: Return Book"
print_info "Return test is skipped to preserve test data"
# api_request POST "/api/v1/loans/${LOAN_ID}/return?userId=${TEST_USER_ID}" "" "Returning book for loan ${LOAN_ID}"

# Create additional loan for testing
print_test_header "Creating Additional Test Loan"

if [ "$BOOK_ID" != "1" ]; then
    # Try to get another book
    BOOKS_RESPONSE2=$(curl -s "${BASE_URL}/api/v1/books?page=0&size=2")
    BOOK_ID_2=$(echo "$BOOKS_RESPONSE2" | python3 -c "import sys, json; data=json.load(sys.stdin); books=data.get('data', []); print(books[1].get('id', '2') if len(books) > 1 else '2')" 2>/dev/null || echo "2")

    LOAN_DATA_2="{
      \"bookId\": ${BOOK_ID_2}
    }"

    api_request POST "/api/v1/loans?userId=${TEST_USER_ID}" "${LOAN_DATA_2}" "Creating second loan"
fi

# Test pagination
print_test_header "Test 8: Test Pagination"

api_request GET "/api/v1/loans?userId=${TEST_USER_ID}&page=0&size=10&sort=createdAt,desc" "" "Getting loans with pagination (page 0, size 10)"

echo ""
print_success "Book Loan API tests completed"
echo "Created Loan ID: ${LOAN_ID}"
