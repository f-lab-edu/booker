#!/bin/bash
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

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "Failed to get admin token"
    exit 1
fi

echo "[keycloak-init] realm 생성"
# 3. Create realm
curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"realm":"'$REALM'","enabled":true}'

echo "[keycloak-init] client 생성"
# 4. Create client
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
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
  }'

echo "[keycloak-init] user 생성"
# 5. Create user
curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"'$USER_NAME'","enabled":true,"credentials":[{"type":"password","value":"'$USER_PASS'","temporary":false}]}'

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

echo "[keycloak-init] Keycloak realm, client, and user created successfully" 