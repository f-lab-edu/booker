#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to get token
get_user_token() {
    local TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8083/realms/myrealm/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=springboot-client" \
        -d "client_secret=springboot-secret" \
        -d "username=testuser" \
        -d "password=testuser")
    
    echo "$TOKEN_RESPONSE" | jq -r '.access_token'
}

# Function to make API request and display result
make_request() {
    local METHOD=$1
    local ENDPOINT=$2
    local TOKEN=$3
    local DATA=$4

    echo -e "\n${YELLOW}Testing $METHOD $ENDPOINT${NC}"
    
    if [ -n "$DATA" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -X $METHOD "http://localhost:8084$ENDPOINT" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$DATA")
    else
        RESPONSE=$(curl -s -w "\n%{http_code}" -X $METHOD "http://localhost:8084$ENDPOINT" \
            -H "Authorization: Bearer $TOKEN")
    fi

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✅ $METHOD request successful (HTTP $HTTP_CODE)${NC}"
    else
        echo -e "${RED}❌ $METHOD request failed (HTTP $HTTP_CODE)${NC}"
    fi
    echo "Response body:"
    echo "$BODY"
}

echo -e "${YELLOW}🔑 Getting token for testuser...${NC}"
TOKEN=$(get_user_token)

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo -e "${RED}❌ Failed to get user token${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Token received successfully${NC}"
echo -e "${BLUE}🔍 Checking roles in token:${NC}"
echo $TOKEN | jq -R 'split(".") | .[1] | @base64d | fromjson | .realm_access.roles' 2>/dev/null

# Test GET /api/books (Get all books)
make_request "GET" "/api/books" "$TOKEN"

# Test GET /api/books/{id} (Get book by ID)
make_request "GET" "/api/books/1" "$TOKEN"

# Test POST /api/books (Create a new book)
make_request "POST" "/api/books" "$TOKEN" '{"title": "Test Book", "author": "Test Author"}'

# Test PUT /api/books/{id} (Update a book)
make_request "PUT" "/api/books/1" "$TOKEN" '{"title": "Updated Book", "author": "Updated Author"}'

# Test DELETE /api/books/{id} (Delete a book)
make_request "DELETE" "/api/books/1" "$TOKEN" 