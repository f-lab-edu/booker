#!/bin/sh
set -e
set -x

echo "[keycloak-init] apk update"
apk update
echo "[keycloak-init] apk add jq"
apk add --no-cache jq

echo "[keycloak-init] 환경 변수 설정"
KEYCLOAK_URL="http://keycloak:8083"
ADMIN_USER="keycloak_admin"
ADMIN_PASS="keycloak_admin_password123!"
REALM="myrealm"
CLIENT_ID="springboot-client"
CLIENT_SECRET="springboot-secret"
REDIRECT_URIS="[\"http://localhost:8083/login/oauth2/code/keycloak\",\"http://localhost:8084/*\"]"
USER_NAME="testuser"
USER_PASS="testuser"

# Define roles as space-separated string
ROLES="ADMIN USER"

# Function to check curl response
check_curl_response() {
    local response=$1
    local error_message=$2
    if [ -z "$response" ] || [ "$response" = "null" ]; then
        echo "Error: $error_message"
        exit 1
    fi
}

echo "[keycloak-init] Keycloak 서버 대기..."
# Wait for Keycloak to be ready
max_attempts=30
attempt=1
while [ $attempt -le $max_attempts ]; do
    if curl -s -f -o /dev/null "$KEYCLOAK_URL/realms/master"; then
        echo "Keycloak is ready!"
        break
    fi
    echo "Waiting for Keycloak to be ready (attempt $attempt/$max_attempts)..."
    sleep 10
    attempt=$((attempt + 1))
done

if [ $attempt -gt $max_attempts ]; then
    echo "Timeout waiting for Keycloak to be ready"
    exit 1
fi

# Wait a bit more to ensure Keycloak is fully initialized
sleep 10

echo "[keycloak-init] admin token 요청"
# 2. Get admin token
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d 'grant_type=password' \
  -d 'client_id=admin-cli' \
  | jq -r .access_token)

check_curl_response "$TOKEN" "Failed to get admin token"

echo "[keycloak-init] realm 생성"
# 3. Create realm
REALM_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"realm":"'$REALM'","enabled":true}')

HTTP_CODE=$(echo "$REALM_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
    echo "Failed to create realm. HTTP code: $HTTP_CODE"
    exit 1
fi

echo "[keycloak-init] client 생성"
# 4. Create client
CLIENT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "'$CLIENT_ID'",
    "secret": "'$CLIENT_SECRET'",
    "enabled": true,
    "protocol": "openid-connect",
    "publicClient": false,
    "redirectUris": '$REDIRECT_URIS',
    "webOrigins": ["http://localhost:8084"],
    "rootUrl": "http://localhost:8084",
    "baseUrl": "/",
    "standardFlowEnabled": true,
    "directAccessGrantsEnabled": true,
    "serviceAccountsEnabled": true,
    "authorizationServicesEnabled": true,
    "clientAuthenticatorType": "client-secret"
  }')

HTTP_CODE=$(echo "$CLIENT_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
    echo "Failed to create client. HTTP code: $HTTP_CODE"
    exit 1
fi

echo "[keycloak-init] roles 생성"
# Create roles
for role in $ROLES; do
    echo "Creating role: $role"
    ROLE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"name":"'$role'"}')
    
    HTTP_CODE=$(echo "$ROLE_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        echo "Failed to create role $role. HTTP code: $HTTP_CODE"
        exit 1
    fi
done

echo "[keycloak-init] user 생성"
# 5. Create user
USER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "'$USER_NAME'",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "'$USER_PASS'",
      "temporary": false
    }]
  }')

HTTP_CODE=$(echo "$USER_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
    echo "Failed to create user. HTTP code: $HTTP_CODE"
    exit 1
fi

# Get user ID
USER_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq -r '.[] | select(.username=="'$USER_NAME'") | .id')

check_curl_response "$USER_ID" "Failed to get user ID"

echo "[keycloak-init] user role 할당"
# Get role IDs and assign them
for role in $ROLES; do
    echo "Getting role ID for: $role"
    ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/roles/$role" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      | jq -r '.id')
    
    check_curl_response "$ROLE_ID" "Failed to get role ID for $role"
    
    echo "Assigning role $role to user"
    ASSIGN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users/$USER_ID/role-mappings/realm" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '[{"id":"'$ROLE_ID'","name":"'$role'"}]')
    
    HTTP_CODE=$(echo "$ASSIGN_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "204" ]; then
        echo "Failed to assign role $role to user. HTTP code: $HTTP_CODE"
        exit 1
    fi
done

echo "[keycloak-init] service account role 할당"
# Get service account ID
SERVICE_ACCOUNT_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq -r '.[] | select(.clientId=="'$CLIENT_ID'") | .id')

check_curl_response "$SERVICE_ACCOUNT_ID" "Failed to get service account ID"

# Get service account user ID
SERVICE_USER_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/clients/$SERVICE_ACCOUNT_ID/service-account-user" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq -r '.id')

check_curl_response "$SERVICE_USER_ID" "Failed to get service account user ID"

# Assign roles to service account
for role in $ROLES; do
    echo "Getting role ID for service account: $role"
    ROLE_ID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/roles/$role" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      | jq -r '.id')
    
    check_curl_response "$ROLE_ID" "Failed to get role ID for $role"
    
    echo "Assigning role $role to service account"
    ASSIGN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users/$SERVICE_USER_ID/role-mappings/realm" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '[{"id":"'$ROLE_ID'","name":"'$role'"}]')
    
    HTTP_CODE=$(echo "$ASSIGN_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "204" ]; then
        echo "Failed to assign role $role to service account. HTTP code: $HTTP_CODE"
        exit 1
    fi
done

echo "[keycloak-init] user 생성 확인"
# 6. Verify user creation
USER_INFO=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/users?username=$USER_NAME" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

if echo "$USER_INFO" | jq -e '. | length > 0' > /dev/null; then
    echo "[keycloak-init] ✅ User '$USER_NAME' created successfully"
else
    echo "[keycloak-init] ❌ Failed to verify user '$USER_NAME' creation"
    exit 1
fi

echo "[keycloak-init] Keycloak realm, client, roles, and user created successfully" 