#!/bin/bash

# Booker 웹 클라이언트 실행 스크립트
# Docker Compose를 사용하여 booker-client를 빌드하고 실행합니다.

set -e  # 에러 발생시 스크립트 중단

# .env 파일 로드
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    set -a
    source .env
    set +a
else
    echo "Warning: .env file not found. Please create one with GOOGLE_CLIENT_ID and other required variables."
fi

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

IMAGE_NAME="booker-booker-client"

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}   Booker 웹 클라이언트 시작${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""

# 1. 기존 컨테이너 확인 및 처리
echo -e "${YELLOW}[1/4] 기존 컨테이너 확인 중...${NC}"
if docker ps --format '{{.Names}}' | grep -q '^booker-client$'; then
    echo "booker-client 컨테이너가 실행 중입니다."
    echo -e "${BLUE}컨테이너를 재시작합니다...${NC}"
    docker restart booker-client
    echo -e "${GREEN}✓ 컨테이너 재시작 완료${NC}"
    echo ""

    # 재시작 후 바로 상태 확인으로 이동
    echo -e "${YELLOW}[4/4] 서비스 상태 확인 중...${NC}"
    sleep 2

    if docker ps --format '{{.Names}}' | grep -q '^booker-client$'; then
        echo -e "${GREEN}✓ booker-client 컨테이너가 정상적으로 실행 중입니다!${NC}"
        echo ""
        echo -e "${GREEN}=====================================${NC}"
        echo -e "${GREEN}   접속 정보${NC}"
        echo -e "${GREEN}=====================================${NC}"
        echo -e "웹 클라이언트: ${GREEN}http://localhost:3000${NC}"
        echo -e "API 서버:      ${GREEN}http://localhost:8084${NC}"
        echo -e "${GREEN}=====================================${NC}"
        echo ""
        echo "로그를 확인하려면: docker logs -f booker-client"
        echo "컨테이너를 중지하려면: docker stop booker-client"
        exit 0
    else
        echo -e "${RED}✗ 컨테이너 재시작 실패${NC}"
        exit 1
    fi
elif docker ps -a --format '{{.Names}}' | grep -q '^booker-client$'; then
    echo "중지된 booker-client 컨테이너를 제거합니다..."
    docker rm booker-client 2>/dev/null || true
    echo -e "${GREEN}✓ 기존 컨테이너 제거 완료${NC}"
else
    echo "기존 컨테이너가 없습니다."
fi
echo ""

# 2. 필요한 서비스 확인 (springboot)
echo -e "${YELLOW}[2/4] 의존 서비스 확인 중...${NC}"
if ! docker ps --format '{{.Names}}' | grep -q '^springboot$'; then
    echo -e "${RED}⚠ Warning: springboot 서비스가 실행되지 않았습니다.${NC}"
    echo "API 연동을 위해서는 먼저 springboot 서비스를 시작해야 합니다."
    echo ""
    read -p "전체 스택을 시작하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "전체 스택을 시작합니다..."
        docker-compose up -d
    else
        echo "booker-client만 단독으로 시작합니다 (API 연동 불가)."
    fi
else
    echo -e "${GREEN}✓ springboot 서비스 실행 중${NC}"
fi
echo ""

# 3. 이미지 재빌드 및 컨테이너 실행
echo -e "${YELLOW}[3/4] 이미지 빌드 및 컨테이너 실행 중...${NC}"
echo "최신 코드 변경사항을 반영하기 위해 이미지를 재빌드합니다..."
docker-compose up -d --build booker-client

# 실행 결과 확인
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 컨테이너 실행 완료${NC}"
else
    echo -e "${RED}✗ 실행 실패${NC}"
    exit 1
fi
echo ""

# 4. 서비스 상태 확인
echo -e "${YELLOW}[4/4] 서비스 상태 확인 중...${NC}"
sleep 3  # 컨테이너 시작 대기

if docker ps --format '{{.Names}}' | grep -q '^booker-client$'; then
    echo -e "${GREEN}✓ booker-client 컨테이너가 정상적으로 실행 중입니다!${NC}"
    echo ""
    echo -e "${GREEN}=====================================${NC}"
    echo -e "${GREEN}   접속 정보${NC}"
    echo -e "${GREEN}=====================================${NC}"
    echo -e "웹 클라이언트: ${GREEN}http://localhost:3000${NC}"
    echo -e "API 서버:      ${GREEN}http://localhost:8084${NC}"
    echo -e "${GREEN}=====================================${NC}"
    echo ""
    echo "로그를 확인하려면:"
    echo "  docker logs -f booker-client"
    echo ""
    echo "컨테이너를 중지하려면:"
    echo "  docker stop booker-client"
    echo ""

    # 로그 표시 옵션
    read -p "실시간 로그를 보시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo ""
        echo -e "${YELLOW}로그 출력 중... (Ctrl+C로 종료)${NC}"
        echo ""
        docker logs -f booker-client
    fi
else
    echo -e "${RED}✗ 컨테이너 시작 실패${NC}"
    echo "로그를 확인하세요:"
    docker logs booker-client
    exit 1
fi
