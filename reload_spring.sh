#!/bin/bash

# Spring Boot 컨테이너만 다시 빌드하고 재시작 (캐시 사용)
# --no-cache 옵션을 제거하여 변경사항이 없는 레이어는 재사용하므로 빌드 속도가 빠릅니다.
docker-compose up -d --build springboot

# 불필요한 이미지 제거 (선택 사항)
docker image prune -f

echo "Spring Boot 애플리케이션이 재시작되었습니다."
