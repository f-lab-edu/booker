#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🔑 Getting client credentials token...${NC}"
CLIENT_TOKEN=$(curl -s -X POST "http://localhost:8083/realms/myrealm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=springboot-client" \
  -d "client_secret=springboot-secret" | jq -r '.access_token')

if [ -z "$CLIENT_TOKEN" ] || [ "$CLIENT_TOKEN" == "null" ]; then
    echo -e "${RED}❌ Failed to get client credentials token${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Client credentials token received successfully${NC}"
echo -e "${BLUE}🔍 Checking roles in client credentials token:${NC}"
echo $CLIENT_TOKEN | jq -R 'split(".") | .[1] | @base64d | fromjson | .realm_access.roles' 2>/dev/null

echo -e "\n${YELLOW}🔑 Getting token for testuser...${NC}"
USER_TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8083/realms/myrealm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=springboot-client" \
  -d "client_secret=springboot-secret" \
  -d "username=testuser" \
  -d "password=testuser")

USER_TOKEN=$(echo "$USER_TOKEN_RESPONSE" | jq -r '.access_token')

if [ -z "$USER_TOKEN" ] || [ "$USER_TOKEN" == "null" ]; then
    echo -e "${RED}❌ Failed to get user token${NC}"
    echo -e "${RED}Error response:${NC}"
    echo "$USER_TOKEN_RESPONSE" | jq '.'
    exit 1
fi

echo -e "${GREEN}✅ User token received successfully${NC}"
echo -e "${BLUE}🔍 Checking roles in user token:${NC}"
echo $USER_TOKEN | jq -R 'split(".") | .[1] | @base64d | fromjson | .realm_access.roles' 2>/dev/null

echo -e "\n${YELLOW}🔄 Testing API endpoint with client credentials token...${NC}"
CLIENT_RESPONSE=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $CLIENT_TOKEN" http://localhost:8084/api/books)
CLIENT_HTTP_CODE=$(echo "$CLIENT_RESPONSE" | tail -n1)
CLIENT_BODY=$(echo "$CLIENT_RESPONSE" | sed '$d')

if [ "$CLIENT_HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ API request with client credentials successful (HTTP $CLIENT_HTTP_CODE)${NC}"
    echo "Response body:"
    echo "$CLIENT_BODY" | jq '.' 2>/dev/null || echo "$CLIENT_BODY"
else
    echo -e "${RED}❌ API request with client credentials failed (HTTP $CLIENT_HTTP_CODE)${NC}"
    echo "Response body:"
    echo "$CLIENT_BODY"
fi

echo -e "\n${YELLOW}🔄 Testing API endpoint with user token...${NC}"
USER_RESPONSE=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $USER_TOKEN" http://localhost:8084/api/books)
USER_HTTP_CODE=$(echo "$USER_RESPONSE" | tail -n1)
USER_BODY=$(echo "$USER_RESPONSE" | sed '$d')

if [ "$USER_HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ API request with user token successful (HTTP $USER_HTTP_CODE)${NC}"
    echo "Response body:"
    echo "$USER_BODY" | jq '.' 2>/dev/null || echo "$USER_BODY"
else
    echo -e "${RED}❌ API request with user token failed (HTTP $USER_HTTP_CODE)${NC}"
    echo "Response body:"
    echo "$USER_BODY"
fi 
