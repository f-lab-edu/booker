# Issue: OAuth2 기반 인증/인가 시스템 구현

## 1. 개요

### 문제 정의
- 중앙화된 인증 서버를 통한 토큰 기반 stateless 인증 구현
- 역할 기반 접근 제어(RBAC)를 통한 세밀한 권한 관리

## 2. 시스템 아키텍처

### 2.1 네트워크 구성
```
External Access → http://localhost:8083 (Keycloak)
                ↓
Browser → Spring Boot App (localhost:8084) → Keycloak Container (keycloak:8083)
```

### 2.2 OAuth2 플로우
![OAuth2 Flow](./wiki/01_keycloak/diagram/KEYCLOAK_DOCKER_NETWORKING.png)

**주요 단계:**
1. 클라이언트가 인증 요청 (Authorization Code Grant)
2. Keycloak 로그인 페이지로 리다이렉트
3. 사용자 인증 후 Authorization Code 반환
4. Access Token 교환
5. JWT 토큰 검증 및 사용자 정보 추출
6. 역할 기반 API 접근 제어

## 핵심 특징

**1. 컴퓨터 공학적 본질**
- **인증(Authentication)**: "누구인가?" - Keycloak을 통한 사용자 신원 확인
- **인가(Authorization)**: "무엇을 할 수 있는가?" - JWT 토큰의 역할 정보로 접근 제어
- **무상태성(Stateless)**: JWT 토큰으로 서버 확장성 확보

https://apidog.com/kr/blog/oauth-vs-jwt-2/

**2. 해결하는 핵심 문제**
- **중앙화된 인증**: 여러 서비스가 하나의 인증 서버를 공유
- **토큰 기반 보안**: 세션 의존성 제거로 분산 서버 환경 최적화
- **표준 준수**: OAuth2/OpenID Connect 표준을 따른 상호 운용성

**3. 구현 구조**
- **부모 이슈**: OAuth2 인증/인가 시스템 전체
- **자식 이슈**: 
  - Keycloak 서버 구성
  - JWT 토큰 검증
  - 다중 Grant Type 지원
  - 역할 기반 접근 제어

### 기술 스택
- **Keycloak**: 표준 OAuth2/OpenID Connect 인증 서버
- **Spring Security lib + Spring boot**: JWT 토큰 검증 및 인가



## 3. 부모 이슈: OAuth2 인증/인가 시스템

### 3.1 핵심 요구사항
- **인증 서버 분리**: 애플리케이션과 독립된 Keycloak 인증 서버
- **토큰 기반 인증**: Stateless JWT 토큰으로 확장성 확보
- **다중 Grant Type 지원**: Client Credentials, Authorization Code, Password Grant
- **역할 기반 접근 제어**: ADMIN/USER 역할을 통한 API 보안

### 3.2 기술 스택
```gradle
// OAuth2 Resource Server
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.springframework.boot:spring-boot-starter-security'

// JWT 처리
implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
implementation 'io.jsonwebtoken:jjwt-impl:0.11.5'
implementation 'io.jsonwebtoken:jjwt-jackson:0.11.5'

// Keycloak Admin Client
implementation 'org.keycloak:keycloak-admin-client:22.0.1'
```

## 4. 자식 이슈들

### 4.1 자식 이슈 #1: Keycloak 인증 서버 구성

**목표**: 중앙화된 OAuth2 인증 서버 구축

**구현 내용**:
- PostgreSQL 기반 Keycloak 데이터 저장소
- Realm(`myrealm`) 및 Client(`springboot-client`) 자동 생성
- 사용자/역할 관리 시스템

```yaml
# docker-compose.yml 주요 구성
keycloak:
  image: quay.io/keycloak/keycloak:latest
  environment:
    - KC_DB=postgres
    - KC_HOSTNAME_STRICT=false
    - KC_HTTP_ENABLED=true
    - KEYCLOAK_ADMIN=keycloak_admin
```

**검증 기준**:
- Keycloak Admin Console 접근 가능
- Realm/Client 자동 생성 완료
- Health Check 통과

### 4.2 자식 이슈 #2: JWT 토큰 검증 및 인가

**목표**: Spring Security를 통한 JWT 토큰 검증 및 역할 기반 접근 제어

**핵심 메커니즘**:
- **토큰 검증**: Keycloak JWK Set을 통한 서명 검증
- **역할 추출**: JWT Claims에서 `realm_access.roles` 추출
- **메소드 보안**: `@PreAuthorize`를 통한 세밀한 권한 제어

```java
// SecurityConfig 주요 설정
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

**검증 기준**:
- Client Credentials Token으로 API 접근 성공
- User Token으로 역할 기반 접근 제어 동작
- 유효하지 않은 토큰 차단

### 4.3 자식 이슈 #3: 다중 Grant Type 지원

**목표**: 다양한 인증 시나리오 지원

**Grant Types**:
1. **Client Credentials**: 서비스 간 통신
   ```bash
   curl -X POST "http://localhost:8083/realms/myrealm/protocol/openid-connect/token" \
     -d "grant_type=client_credentials" \
     -d "client_id=springboot-client" \
     -d "client_secret=springboot-secret"
   ```

2. **Password Grant**: 직접 사용자 인증 (개발용)
   ```bash
   curl -X POST "http://localhost:8083/realms/myrealm/protocol/openid-connect/token" \
     -d "grant_type=password" \
     -d "username=testuser" \
     -d "password=testuser"
   ```

**검증 기준**:
- 각 Grant Type별 토큰 발급 성공
- 발급된 토큰의 역할 정보 포함 확인
- API 접근 시 적절한 권한 검증

### 4.4 자식 이슈 #4: 역할 기반 접근 제어 (RBAC)

**목표**: 세밀한 권한 관리 시스템

**역할 구조**:
- **ADMIN**: 모든 CRUD 작업 허용
- **USER**: 읽기 전용 접근

**구현 방식**:
- Keycloak Realm Roles 활용
- JWT Claims를 통한 역할 정보 전달
- Spring Security Expression을 통한 메소드 레벨 보안

```java
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<Book> createBook(@RequestBody Book book) {
    // ADMIN 역할만 접근 가능
}

@PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
public ResponseEntity<List<Book>> getAllBooks() {
    // ADMIN, USER 역할 모두 접근 가능
}
```

## 5. 테스트 시나리오

### 5.1 인증 테스트
```bash
# Client Credentials Token 획득
./test-api.sh

# 결과: ✅ Client credentials token received successfully
# 역할 확인: ["ADMIN", "USER"]
```

### 5.2 API RBAC 접근 테스트
```bash
# CRUD 작업 테스트
./test-book-api.sh

# 결과: 
# ✅ GET request successful (HTTP 200)
# ✅ POST request successful (HTTP 200)
# ✅ PUT request successful (HTTP 200)
# ✅ DELETE request successful (HTTP 200)
```

## 6. 기술적 고려사항

### 6.1 보안
- **토큰 만료**: Access Token 짧은 생명주기로 보안 강화
- **HTTPS**: 프로덕션 환경에서 필수 (현재 개발용 HTTP)
- **Secret 관리**: 환경변수를 통한 민감정보 관리

### 6.2 성능
- **토큰 캐싱**: JWK Set 캐싱으로 검증 성능 최적화
- **Connection Pool**: Keycloak과의 연결 관리
- **Stateless**: JWT 기반 무상태 인증으로 확장성 확보

### 6.3 모니터링
- **Health Check**: Keycloak 서버 상태 확인
- **로그 집중화**: 인증/인가 실패 로그 모니터링
- **메트릭 수집**: 토큰 발급/검증 성능 지표

## 7. 완료 기준 (Definition of Done)

- [x] Keycloak 인증 서버 정상 동작
- [x] JWT 토큰 발급 및 검증 성공
- [x] 역할 기반 API 접근 제어 동작
- [x] 자동화된 테스트 스크립트 통과 -> test-api.sh 를 통해 테스트 검증
- [x] Docker Compose 환경에서 모든 서비스 정상 기동
- [x] API 문서화 (Swagger) 완료
- [x] Swagger api 토큰 발급 및 세션 관리 기능 추가
- [ ] 로그아웃 api 만들기 (브라우져 쿠키 삭제 + 키클락 해당 유저 세션 정보 삭제)
- [ ] 역할 기반 API 접근 제어 동작 실패 케이스 테스트
- [ ] Social Login: Google 소셜 로그인 연동