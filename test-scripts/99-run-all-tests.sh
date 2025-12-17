#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Running All API Tests"

# Check if server is running
if ! check_server; then
    print_error "Server is not running. Please start the server first."
    print_info "Run: cd booker-server && ./gradlew bootRun"
    exit 1
fi

# Array of test scripts
TESTS=(
    "01-test-auth.sh"
    "02-test-books.sh"
    "03-test-book-loans.sh"
    "04-test-book-orders.sh"
    "05-test-events.sh"
    "06-test-event-participation.sh"
    "07-test-work-logs.sh"
    "08-test-load-test.sh"
)

# Track results
PASSED=0
FAILED=0
TOTAL=${#TESTS[@]}

# Run each test
for test in "${TESTS[@]}"; do
    echo ""
    echo "========================================="
    echo "Running: $test"
    echo "========================================="

    if bash "$(dirname "$0")/$test"; then
        ((PASSED++))
        print_success "$test completed successfully"
    else
        ((FAILED++))
        print_error "$test failed"
    fi

    # Add delay between tests
    sleep 2
done

# Summary
echo ""
echo "========================================="
echo "Test Summary"
echo "========================================="
echo "Total Tests: $TOTAL"
print_success "Passed: $PASSED"

if [ $FAILED -gt 0 ]; then
    print_error "Failed: $FAILED"
else
    print_success "Failed: $FAILED"
fi

echo ""

if [ $FAILED -eq 0 ]; then
    print_success "All tests completed successfully! 🎉"
    exit 0
else
    print_error "Some tests failed. Please check the logs above."
    exit 1
fi
