#!/bin/bash

# Load configuration
source "$(dirname "$0")/00-config.sh"

print_test_header "Testing Authentication API (/api/v1/auth)"

# Check if server is running
check_server || exit 1

# Note: This test requires a valid Google ID Token
# For actual testing, you need to obtain a real token from Google OAuth
print_info "Note: Google token verification requires a valid ID token from Google OAuth"
print_info "This test is skipped in automated testing"

# Test 1: Google Token Verification (with dummy token - will fail)
echo ""
print_test_header "Test 1: Google Token Verification (Dummy Token)"

DUMMY_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

api_request POST "/api/v1/auth/google/verify" \
    "{\"idToken\": \"${DUMMY_TOKEN}\"}" \
    "Verifying Google ID Token (Expected to fail with 401)"

print_info "To test with real Google token:"
print_info "1. Go to https://developers.google.com/oauthplayground"
print_info "2. Select 'Google OAuth2 API v2'"
print_info "3. Authorize and get ID token"
print_info "4. Use the token in the request"

echo ""
print_success "Auth API tests completed"
print_info "Manual testing with real Google token is required for full verification"
