# Implement OAuth2 Authentication System with Keycloak

## 개요
Keycloak을 활용한 중앙화된 인증/인가 시스템을 구현했습니다. 주요 구현 내용은 다음과 같습니다:

1. **인증 서버 구성**
   - Keycloak + PostgreSQL 기반 인증 서버
   - Realm, Client, Role 자동 설정 스크립트

2. **Spring Security 통합**
   - OAuth2 Resource Server 설정
   - JWT 토큰 검증 및 역할 기반 접근 제어

3. **테스트 자동화**
   - 토큰 발급 테스트 (`test-api.sh`)
   - RBAC 기반 API 접근 테스트 (`test-book-api.sh`)

## 메인 리뷰어 지정 및 Due Date
- 메인 리뷰어: 
- Due Date: 

## 리뷰 시 참고 사항
1. **보안 관련**
   - Keycloak 설정의 보안성 검토 필요
   - JWT 토큰 검증 로직 리뷰
   - 환경변수를 통한 시크릿 관리 방식 검토

2. **테스트 커버리지**
   - 인증 실패 케이스 테스트 추가 필요
   - RBAC 접근 제어 테스트 시나리오 검토

## TODO
- [ ] Swagger UI에 토큰 인증 통합
- [ ] 실패 케이스에 대한 테스트 코드 추가
- [ ] 운영 환경 HTTPS 설정

## References
[1] [Keycloak Official Documentation](https://www.keycloak.org/documentation)
[2] [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
[3] [Secure Spring Boot REST API with Keycloak](https://medium.com/thefreshwrites/secure-spring-boot-rest-api-with-keycloak-2f9d8d99feb0)


## 체크리스트
- [x] PR 제목을 명령형으로 작성했습니다.
- [x] PR을 연관되는 github issue에 연결했습니다. (#123)
- [x] 리뷰 리퀘스트 전에 셀프 리뷰를 진행했습니다.
- [x] 변경사항에 대한 테스트코드를 추가했습니다.
  - `test-api.sh`: 토큰 발급 및 검증
  - `test-book-api.sh`:  RBAC 기반 API 접근 + 토큰 발급 및 검증
  ![test-book-api.sh](/wiki/01_keycloak/diagram/test-book-api-20250621.png)

## 고민 거리
1. **토큰 갱신 전략**
   - Refresh Token 도입 시점과 보안 영향도
   - 토큰 만료 시간 최적화 방안

2. swagger ui 에서 토큰 발급 연동
   - 쉘스크립트 curl -> 도커 네트워크에서 로그인확인
   - swagger ui 에서 토큰 발급 연동 디버깅 필요