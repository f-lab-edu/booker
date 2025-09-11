#!/bin/bash

# 이벤트 참여 부하 테스트 실행 스크립트

echo "=== 이벤트 참여 부하 테스트 시작 ==="

# Docker Compose 서비스 상태 확인
echo "Docker 서비스 상태 확인 중..."
if ! docker-compose ps | grep -q "Up"; then
    echo "Docker Compose 서비스가 실행되지 않았습니다. 먼저 서비스를 시작해주세요:"
    echo "docker-compose up -d"
    exit 1
fi

# Spring Boot 애플리케이션 헬스체크
echo "Spring Boot 애플리케이션 상태 확인 중..."
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/actuator/health)
if [ "$HEALTH_CHECK" != "200" ]; then
    echo "Spring Boot 애플리케이션이 준비되지 않았습니다. (Status: $HEALTH_CHECK)"
    echo "애플리케이션이 완전히 시작될 때까지 잠시 기다려주세요."
    exit 1
fi

# InfluxDB 상태 확인
echo "InfluxDB 상태 확인 중..."
INFLUX_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8086/ping)
if [ "$INFLUX_CHECK" != "204" ]; then
    echo "InfluxDB가 준비되지 않았습니다. (Status: $INFLUX_CHECK)"
    exit 1
fi

echo "모든 서비스가 준비되었습니다. 부하 테스트를 시작합니다..."

# K6 부하 테스트 실행
docker run --rm -i \
  --network="host" \
  -v "$(pwd)/k6-script:/scripts" \
  -e BASE_URL=http://localhost:8084 \
  grafana/k6:latest run \
  --out influxdb=http://localhost:8086/myk6db \
  /scripts/event-participation-load-test.js

echo ""
echo "=== 부하 테스트 완료 ==="
echo ""
echo "📊 결과 확인:"
echo "  - Grafana 대시보드: http://localhost:3000"
echo "  - InfluxDB 직접 접근: http://localhost:8086"
echo "  - Spring Boot 메트릭: http://localhost:8084/actuator/prometheus"
echo ""
echo "💡 Grafana에서 K6 메트릭을 확인하려면:"
echo "  1. http://localhost:3000 접속 (admin/admin)"
echo "  2. 'Event Participation Monitoring' 대시보드 열기"
echo "  3. K6 관련 패널에서 테스트 결과 확인"