#!/bin/bash

echo "🔧 Google OAuth 설정 도우미"
echo "=========================="
echo ""

# .env 파일이 있으면 자동으로 로드
if [ -f ".env" ]; then
    echo "📁 .env 파일을 발견했습니다. 환경변수를 로드합니다..."
    export $(grep -v '^#' .env | xargs)
    echo ""
fi

# Google OAuth 설정 확인
if [ -n "$GOOGLE_CLIENT_ID" ] && [ -n "$GOOGLE_CLIENT_SECRET" ] && [ "$GOOGLE_CLIENT_ID" != "your-google-client-id" ]; then
    echo "✅ Google OAuth 환경변수가 설정되어 있습니다."
    echo "Client ID: ${GOOGLE_CLIENT_ID:0:20}..."
    echo ""
    echo "🚀 Docker Compose 실행:"
    echo "docker-compose up -d"
    echo ""
    echo "또는 개별 서비스 재시작:"
    echo "docker-compose restart keycloak-init"
    
else
    echo "❌ Google OAuth 환경변수가 설정되지 않았습니다."
    echo ""
    echo "📋 설정 방법:"
    echo ""
    echo "1. Google Cloud Console 접속:"
    echo "   https://console.cloud.google.com/apis/credentials"
    echo ""
    echo "2. '사용자 인증 정보 만들기' > 'OAuth 2.0 클라이언트 ID' 선택"
    echo ""
    echo "3. 애플리케이션 유형: '웹 애플리케이션' 선택"
    echo ""
    echo "4. 승인된 리디렉션 URI 추가:"
    echo "   http://localhost:8083/realms/myrealm/broker/google/endpoint"
    echo ""
    echo "5. 생성된 클라이언트 ID와 클라이언트 보안 비밀번호를 복사"
    echo ""
    echo "6. 환경변수 설정:"
    echo "   export GOOGLE_CLIENT_ID='여기에_클라이언트_ID_입력'"
    echo "   export GOOGLE_CLIENT_SECRET='여기에_클라이언트_시크릿_입력'"
    echo ""
    echo "7. Docker Compose 실행:"
    echo "   docker-compose up -d"
    echo ""
    echo "또는 .env 파일 생성:"
    echo "   echo 'GOOGLE_CLIENT_ID=여기에_클라이언트_ID_입력' > .env"
    echo "   echo 'GOOGLE_CLIENT_SECRET=여기에_클라이언트_시크릿_입력' >> .env"
    echo "   docker-compose up -d"
    echo ""
    
    read -p "환경변수를 직접 입력하시겠습니까? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo ""
        read -p "Google Client ID: " NEW_CLIENT_ID
        read -p "Google Client Secret: " NEW_CLIENT_SECRET
        
        if [ -n "$NEW_CLIENT_ID" ] && [ -n "$NEW_CLIENT_SECRET" ]; then
            echo ""
            echo "✅ 환경변수 설정 완료!"
            echo ""
            echo "다음 명령어로 Docker Compose를 실행하세요:"
            echo "GOOGLE_CLIENT_ID='$NEW_CLIENT_ID' GOOGLE_CLIENT_SECRET='$NEW_CLIENT_SECRET' docker-compose up -d"
            echo ""
            echo "또는 .env 파일 업데이트:"
            echo "GOOGLE_CLIENT_ID=$NEW_CLIENT_ID" > .env
            echo "GOOGLE_CLIENT_SECRET=$NEW_CLIENT_SECRET" >> .env
            echo "✅ .env 파일이 업데이트되었습니다!"
            echo "이제 'docker-compose up -d' 명령어로 실행하세요."
        else
            echo "❌ 올바른 값을 입력해주세요."
        fi
    fi
fi

echo ""
echo "📚 참고 정보:"
echo "- Keycloak Admin: http://localhost:8083/admin"
echo "- Keycloak User: http://localhost:8083/realms/myrealm/account" 
echo "- Spring Boot API: http://localhost:8084/swagger-ui.html"
echo "- Test Client: client-example.html"
echo "" 