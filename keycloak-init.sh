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
REDIRECT_URIS="[\"http://localhost:8083/realms/myrealm/broker/google/endpoint\",\"http://localhost:3000/*\",\"http://localhost:8084/*\"]"
USER_NAME="testuser"
USER_PASS="testuser"

# Google OAuth 설정 (환경변수로 주입받거나 기본값 사용)
# 실제 사용시 Google Cloud Console에서 OAuth 2.0 클라이언트 ID를 생성하고 아래 값들을 설정하세요
# https://console.cloud.google.com/apis/credentials
GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}"
GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}"

# Google OAuth 정보 확인
if [ -n "$GOOGLE_CLIENT_ID" ] && [ -n "$GOOGLE_CLIENT_SECRET" ] && [ "$GOOGLE_CLIENT_ID" != "your-google-client-id" ]; then
    echo ""
    echo "🔑 Google OAuth 설정 감지됨!"
    echo "Client ID: ${GOOGLE_CLIENT_ID:0:20}..."
    echo ""
else
    echo ""
    echo "🔧 Google OAuth 설정이 필요합니다!"
    echo ""
    echo "1. Google Cloud Console에 접속: https://console.cloud.google.com/apis/credentials"
    echo "2. OAuth 2.0 클라이언트 ID 생성"
    echo "3. 승인된 리디렉션 URI 추가: http://localhost:8083/realms/myrealm/broker/google/endpoint"
    echo "4. 환경변수 설정 후 다시 실행:"
    echo "   export GOOGLE_CLIENT_ID='your-google-client-id'"
    echo "   export GOOGLE_CLIENT_SECRET='your-google-client-secret'"
    echo ""
    echo "또는 docker-compose.yml에서 환경변수 설정 후 실행:"
    echo "   GOOGLE_CLIENT_ID=your-id GOOGLE_CLIENT_SECRET=your-secret docker-compose up"
    echo ""
    echo "Google OAuth 없이 계속 진행합니다..."
    echo ""
fi

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

# Get client ID for updating settings if client already exists
CLIENT_ID_UUID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq -r '.[] | select(.clientId=="'$CLIENT_ID'") | .id')

check_curl_response "$CLIENT_ID_UUID" "Failed to get client ID"

# Update client settings to ensure direct access grants are enabled
echo "[keycloak-init] client 설정 업데이트"
UPDATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$KEYCLOAK_URL/admin/realms/$REALM/clients/$CLIENT_ID_UUID" \
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

HTTP_CODE=$(echo "$UPDATE_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" != "204" ]; then
    echo "Failed to update client settings. HTTP code: $HTTP_CODE"
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
    "firstName": "Test",
    "lastName": "User",
    "email": "testuser@example.com",
    "requiredActions": [],
    "credentials": [{
      "type": "password",
      "value": "'$USER_PASS'",
      "temporary": false
    }],
    "attributes": {
      "locale": ["en"]
    }
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

echo "[keycloak-init] Google Identity Provider 설정"
# Google Identity Provider 생성
if [ -n "$GOOGLE_CLIENT_ID" ] && [ -n "$GOOGLE_CLIENT_SECRET" ] && [ "$GOOGLE_CLIENT_ID" != "your-google-client-id" ]; then
    echo "🔑 Google OAuth 설정 중..."
    echo "Client ID: ${GOOGLE_CLIENT_ID:0:20}..."
    
    # Create Google Identity Provider
    GOOGLE_IDP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/identity-provider/instances" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "alias": "google",
        "providerId": "google",
        "enabled": true,
        "updateProfileFirstLoginMode": "on",
        "trustEmail": true,
        "storeToken": false,
        "addReadTokenRoleOnCreate": false,
        "authenticateByDefault": false,
        "linkOnly": false,
        "firstBrokerLoginFlowAlias": "first broker login",
        "config": {
          "clientId": "'$GOOGLE_CLIENT_ID'",
          "clientSecret": "'$GOOGLE_CLIENT_SECRET'",
          "syncMode": "IMPORT",
          "useJwksUrl": "true"
        }
      }')
    
    HTTP_CODE=$(echo "$GOOGLE_IDP_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        echo "❌ Google Identity Provider 생성 실패. HTTP code: $HTTP_CODE"
        echo "Response: $GOOGLE_IDP_RESPONSE"
        exit 1
    fi
    
    echo "✅ Google Identity Provider 생성 완료"
    
    # Create email attribute mapper for Google
    EMAIL_MAPPER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/identity-provider/instances/google/mappers" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "google-email-mapper",
        "identityProviderAlias": "google",
        "identityProviderMapper": "oidc-user-attribute-idp-mapper",
        "config": {
          "syncMode": "INHERIT",
          "user.attribute": "email",
          "claim": "email"
        }
      }')
    
    HTTP_CODE=$(echo "$EMAIL_MAPPER_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        echo "⚠️  Google 이메일 매퍼 생성 실패. HTTP code: $HTTP_CODE"
    else
        echo "✅ Google 이메일 매퍼 생성 완료"
    fi
    
    # Create first name attribute mapper for Google
    FIRSTNAME_MAPPER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/identity-provider/instances/google/mappers" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "google-firstname-mapper",
        "identityProviderAlias": "google",
        "identityProviderMapper": "oidc-user-attribute-idp-mapper",
        "config": {
          "syncMode": "INHERIT",
          "user.attribute": "firstName",
          "claim": "given_name"
        }
      }')
    
    HTTP_CODE=$(echo "$FIRSTNAME_MAPPER_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        echo "⚠️  Google 이름 매퍼 생성 실패. HTTP code: $HTTP_CODE"
    else
        echo "✅ Google 이름 매퍼 생성 완료"
    fi
    
    # Create last name attribute mapper for Google
    LASTNAME_MAPPER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/identity-provider/instances/google/mappers" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "google-lastname-mapper",
        "identityProviderAlias": "google",
        "identityProviderMapper": "oidc-user-attribute-idp-mapper",
        "config": {
          "syncMode": "INHERIT",
          "user.attribute": "lastName",
          "claim": "family_name"
        }
      }')
    
    HTTP_CODE=$(echo "$LASTNAME_MAPPER_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        echo "⚠️  Google 성 매퍼 생성 실패. HTTP code: $HTTP_CODE"
    else
        echo "✅ Google 성 매퍼 생성 완료"
    fi
    
    # Create role mapper for Google users (assign USER role to all Google users)
    ROLE_MAPPER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/identity-provider/instances/google/mappers" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "google-user-role-mapper",
        "identityProviderAlias": "google",
        "identityProviderMapper": "oidc-hardcoded-role-idp-mapper",
        "config": {
          "syncMode": "INHERIT",
          "role": "USER"
        }
      }')
    
    HTTP_CODE=$(echo "$ROLE_MAPPER_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "409" ]; then
        echo "⚠️  Google 역할 매퍼 생성 실패. HTTP code: $HTTP_CODE"
    else
        echo "✅ Google 역할 매퍼 생성 완료"
    fi
    
    echo ""
    echo "🎉 Google OAuth 설정이 완료되었습니다!"
    echo "📍 Keycloak 로그인 페이지에서 'google' 버튼을 통해 Google 로그인이 가능합니다."
    echo "🌐 로그인 URL: http://localhost:8083/realms/myrealm/account"
    echo ""
    
else
    echo "⏭️  Google OAuth 설정을 건너뜁니다."
    echo "   Google OAuth를 사용하려면 GOOGLE_CLIENT_ID와 GOOGLE_CLIENT_SECRET 환경변수를 설정하세요."
fi

echo "[keycloak-init] ✅ Keycloak 초기화 완료!"
echo ""
echo "🔗 접속 정보:"
echo "   - Keycloak Admin: http://localhost:8083/admin (keycloak_admin / keycloak_admin_password123!)"
echo "   - Keycloak User: http://localhost:8083/realms/myrealm/account"
echo "   - Spring Boot API: http://localhost:8084/swagger-ui.html"
echo "   - Test User: testuser / testuser"
echo "" 