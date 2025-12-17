#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Testing Book API (/api/v1/books)"

# Check if server is running
check_server || exit 1

# Test 1: Create a book
print_test_header "Test 1: Create Book"

BOOK_DATA='{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884",
  "publisher": "Prentice Hall",
  "publishedYear": 2008,
  "totalCopies": 5
}'

CREATE_RESPONSE=$(api_request POST "/api/v1/books" "${BOOK_DATA}" "Creating a new book")
BOOK_ID=$(echo "$CREATE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -n "$BOOK_ID" ]; then
    print_success "Book created with ID: ${BOOK_ID}"
else
    print_error "Failed to extract book ID from response"
    BOOK_ID="1"  # Fallback ID for testing
fi

# Test 2: Get book by ID
print_test_header "Test 2: Get Book by ID"

api_request GET "/api/v1/books/${BOOK_ID}" "" "Getting book with ID ${BOOK_ID}"

# Test 3: Search books
print_test_header "Test 3: Search Books"

api_request GET "/api/v1/books?page=0&size=20" "" "Searching all books (page 0, size 20)"

# Test 4: Search books with filters
print_test_header "Test 4: Search Books with Title Filter"

api_request GET "/api/v1/books?title=Clean&page=0&size=20" "" "Searching books with title containing 'Clean'"

# Test 5: Update book
print_test_header "Test 5: Update Book"

UPDATE_DATA='{
  "title": "Clean Code (Updated)",
  "author": "Robert C. Martin",
  "isbn": "9780132350884",
  "publisher": "Prentice Hall",
  "publishedYear": 2008,
  "totalCopies": 10
}'

api_request PUT "/api/v1/books/${BOOK_ID}" "${UPDATE_DATA}" "Updating book ${BOOK_ID}"

# Test 6: Delete book (commented out to preserve test data)
print_test_header "Test 6: Delete Book"
print_info "Delete test is skipped to preserve test data"
# api_request DELETE "/api/v1/books/${BOOK_ID}" "" "Deleting book ${BOOK_ID}"

# Create additional test books
print_test_header "Creating Additional Test Books"

BOOK2_DATA='{
  "title": "Effective Java",
  "author": "Joshua Bloch",
  "isbn": "9780134685991",
  "publisher": "Addison-Wesley",
  "publishedYear": 2018,
  "totalCopies": 3
}'

api_request POST "/api/v1/books" "${BOOK2_DATA}" "Creating second test book"

BOOK3_DATA='{
  "title": "Design Patterns",
  "author": "Gang of Four",
  "isbn": "9780201633610",
  "publisher": "Addison-Wesley",
  "publishedYear": 1994,
  "totalCopies": 4
}'

api_request POST "/api/v1/books" "${BOOK3_DATA}" "Creating third test book"

echo ""
print_success "Book API tests completed"
echo "Created Book ID: ${BOOK_ID}"
