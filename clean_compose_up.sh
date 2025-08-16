docker-compose stop springboot

docker-compose build --no-cache springboot

docker-compose up -d springboot

docker image prune

docker images | head -5

echo "Spring Boot 애플리케이션이 시작되었습니다."
echo "브라우저에서 http://localhost:8084/swagger-ui/index.html#/ 로 접속하세요."

# Spring Boot 컨테이너가 완전히 시작될 때까지 잠시 대기
sleep 10

# 크롬 브라우저로 Spring Boot 애플리케이션 접속
open -a "Google Chrome" http://localhost:8084/swagger-ui/index.html#/

# docker logs -f springboot
