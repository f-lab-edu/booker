#!/bin/bash

# K6 로드 테스트 실행 스크립트
# 사용법: ./k6-script/run-load-test.sh

echo "🚀 K6 로드 테스트 시작..."

# Docker 네트워크 확인
if ! docker network ls | grep -q "booker_default"; then
    echo "❌ booker_default 네트워크가 없습니다. docker-compose up -d를 먼저 실행하세요."
    exit 1
fi

# Spring Boot 서버 상태 확인
if ! docker ps | grep -q "springboot"; then
    echo "❌ Spring Boot 서버가 실행 중이지 않습니다. docker-compose up -d를 먼저 실행하세요."
    exit 1
fi

echo "✅ 환경 확인 완료"
echo "📊 테스트 실행 중..."

# K6 실행
docker run --rm \
  --network booker_default \
  -v "$(pwd)/k6-script:/scripts" \
  grafana/k6:latest run /scripts/book-loan-load-test.js \
  --out influxdb=http://influxdb:8086/myk6db

echo "✨ 테스트 완료"
