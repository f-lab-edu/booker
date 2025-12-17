#!/bin/bash

# API Test Configuration
export BASE_URL="http://localhost:8084"
export API_VERSION="v1"
export CONTENT_TYPE="Content-Type: application/json"

# Test User Credentials
export TEST_USER_ID="test-user"
export TEST_USER_NAME="Test User"
export TEST_USER_EMAIL="test@example.com"

# Admin Credentials
export ADMIN_USER_ID="admin"
export ADMIN_USER_NAME="Admin User"

# Color codes for output
export GREEN='\033[0;32m'
export RED='\033[0;31m'
export YELLOW='\033[1;33m'
export NC='\033[0m' # No Color

# Helper functions
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

print_test_header() {
    echo ""
    echo "========================================="
    echo "$1"
    echo "========================================="
}

# Test if server is running
check_server() {
    print_info "Checking if server is running at ${BASE_URL}..."

    if curl -s -f "${BASE_URL}/swagger-ui/index.html" > /dev/null; then
        print_success "Server is running"
        return 0
    else
        print_error "Server is not running. Please start the server first."
        return 1
    fi
}

# Make API request and print response
api_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    echo ""
    print_info "${description}"
    echo "Request: ${method} ${BASE_URL}${endpoint}"

    if [ -n "$data" ]; then
        echo "Data: ${data}"
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X ${method} \
            -H "${CONTENT_TYPE}" \
            -d "${data}" \
            "${BASE_URL}${endpoint}")
    else
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X ${method} \
            "${BASE_URL}${endpoint}")
    fi

    http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d':' -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS/d')

    echo "Status: ${http_status}"
    echo "Response:"
    echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"

    if [ "$http_status" -ge 200 ] && [ "$http_status" -lt 300 ]; then
        print_success "Request successful"
        echo "$body"
        return 0
    else
        print_error "Request failed with status ${http_status}"
        return 1
    fi
}

echo "Configuration loaded successfully"
echo "Base URL: ${BASE_URL}"
echo "API Version: ${API_VERSION}"
