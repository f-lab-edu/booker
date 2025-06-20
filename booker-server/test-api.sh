#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "🔑 Getting token from Keycloak..."
TOKEN=$(curl -s -X POST "http://localhost:8083/realms/myrealm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=springboot-client" \
  -d "client_secret=springboot-secret" | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo -e "${RED}❌ Failed to get token${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Token received successfully${NC}"
echo "🔄 Testing API endpoint..."

RESPONSE=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $TOKEN" http://localhost:8084/api/books)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ API request successful (HTTP $HTTP_CODE)${NC}"
    echo "Response body:"
    echo "$BODY" | jq '.'
else
    echo -e "${RED}❌ API request failed (HTTP $HTTP_CODE)${NC}"
    echo "Response body:"
    echo "$BODY"
fi 