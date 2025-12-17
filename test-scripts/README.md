# Booker API Test Scripts

이 디렉토리에는 Booker API의 모든 엔드포인트를 테스트하는 curl 기반 셸 스크립트가 포함되어 있습니다.

## 사전 요구사항

1. **서버 실행**: Booker 서버가 실행 중이어야 합니다
   ```bash
   cd booker-server
   ./gradlew bootRun
   ```

2. **Python 3**: JSON 파싱을 위해 Python 3이 설치되어 있어야 합니다
   ```bash
   python3 --version
   ```

3. **curl**: HTTP 요청을 위해 curl이 설치되어 있어야 합니다
   ```bash
   curl --version
   ```

## 테스트 스크립트 구성

### 설정 파일
- `00-config.sh`: 공통 설정 및 헬퍼 함수

### 개별 API 테스트
1. `01-test-auth.sh`: 인증 API 테스트
2. `02-test-books.sh`: 도서 관리 API 테스트
3. `03-test-book-loans.sh`: 도서 대출 API 테스트
4. `04-test-book-orders.sh`: 도서 주문 API 테스트
5. `05-test-events.sh`: 이벤트 관리 API 테스트
6. `06-test-event-participation.sh`: 이벤트 참여 API 테스트
7. `07-test-work-logs.sh`: 작업 로그 API 테스트
8. `08-test-load-test.sh`: 부하 테스트 API 테스트

### 통합 테스트
- `99-run-all-tests.sh`: 모든 테스트를 순차적으로 실행

## 사용 방법

### 1. 실행 권한 부여
```bash
cd test-scripts
chmod +x *.sh
```

### 2. 개별 테스트 실행
```bash
# 도서 API 테스트
./02-test-books.sh

# 이벤트 API 테스트
./05-test-events.sh
```

### 3. 전체 테스트 실행
```bash
# 모든 API 테스트 실행
./99-run-all-tests.sh
```

## 테스트 시나리오

### 1. 도서 대출 플로우
```bash
./02-test-books.sh        # 도서 생성
./03-test-book-loans.sh   # 도서 대출 및 반납
```

**시나리오:**
1. 도서 생성 (Clean Code, Effective Java, Design Patterns)
2. 도서 검색 및 조회
3. 도서 대출 신청
4. 대출 목록 조회
5. 대출 기간 연장
6. 도서 반납

### 2. 도서 주문 플로우
```bash
./04-test-book-orders.sh
```

**시나리오:**
1. 사용자가 도서 주문 요청 생성
2. 내 주문 목록 조회
3. 관리자가 주문 승인
4. 관리자가 입고 처리
5. 또 다른 주문 생성 후 거부

### 3. 이벤트 참여 플로우
```bash
./05-test-events.sh              # 이벤트 생성
./06-test-event-participation.sh # 이벤트 참여
```

**시나리오:**
1. 이벤트 생성 (스터디, 워크샵, 기술 발표)
2. 이벤트 목록 조회 및 필터링
3. Synchronized 방식으로 참여
4. CAS 방식으로 참여
5. 동시성 제어 성능 비교

### 4. 작업 로그 플로우
```bash
./07-test-work-logs.sh
```

**시나리오:**
1. 개발 작업 로그 생성
2. 회의록 작성
3. 배포 로그 작성
4. 태그별 로그 조회
5. Markdown 원본 조회

### 5. 부하 테스트 플로우
```bash
./08-test-load-test.sh
```

**시나리오:**
1. 테스트 데이터 셋업
2. 4가지 동시성 제어 방식으로 참여
   - Optimistic Lock
   - Pessimistic Lock
   - CAS (Compare-And-Swap)
   - Synchronized
3. 성능 비교 및 분석

## 출력 형식

### 성공 케이스
```
✅ Request successful
Status: 200
Response:
{
  "id": 1,
  "title": "Clean Code",
  ...
}
```

### 실패 케이스
```
❌ Request failed with status 400
Status: 400
Response:
{
  "error": "Validation failed",
  ...
}
```

### 정보 메시지
```
ℹ️  Checking if server is running at http://localhost:8084...
```

## 설정 변경

### 서버 URL 변경
`00-config.sh` 파일에서 `BASE_URL` 수정:
```bash
export BASE_URL="http://your-server:port"
```

### 테스트 사용자 변경
`00-config.sh` 파일에서 사용자 정보 수정:
```bash
export TEST_USER_ID="custom-user"
export TEST_USER_NAME="Custom User"
export TEST_USER_EMAIL="custom@example.com"
```

## 주의사항

### 1. 데이터 보존
일부 삭제 테스트는 기본적으로 비활성화되어 있습니다:
- 도서 삭제
- 이벤트 삭제
- 대출 반납 (일부)

이를 활성화하려면 해당 스크립트에서 주석을 해제하세요.

### 2. 인증 테스트
Google OAuth 토큰 테스트는 실제 Google ID Token이 필요합니다:
1. [Google OAuth Playground](https://developers.google.com/oauthplayground) 접속
2. Google OAuth2 API v2 선택
3. Authorize 후 ID token 획득
4. 테스트 스크립트에 토큰 입력

### 3. 순서 의존성
일부 테스트는 순서에 의존합니다:
- 대출 테스트는 도서가 먼저 생성되어야 함
- 이벤트 참여 테스트는 이벤트가 먼저 생성되어야 함

전체 테스트 실행 시 `99-run-all-tests.sh`를 사용하면 자동으로 순서가 보장됩니다.

## 트러블슈팅

### 서버가 실행되지 않음
```
❌ Server is not running. Please start the server first.
```
**해결:** 서버를 먼저 실행하세요
```bash
cd booker-server
./gradlew bootRun
```

### Python이 설치되지 않음
```
python3: command not found
```
**해결:** Python 3를 설치하세요
```bash
# macOS
brew install python3

# Ubuntu/Debian
sudo apt-get install python3
```

### 권한 오류
```
Permission denied
```
**해결:** 실행 권한을 부여하세요
```bash
chmod +x *.sh
```

## 테스트 결과 확인

### Swagger UI에서 확인
```
http://localhost:8084/swagger-ui/index.html
```

### H2 Console에서 데이터 확인
```
http://localhost:8084/h2-console
```

## API 검수 보고서

상세한 API 설계 검수 결과는 다음 문서를 참고하세요:
```
.claude/tasks/api-design-audit.md
```

## 정상 플로우 정의

각 도메인별 정상 플로우는 검수 보고서의 "정상 플로우 정의" 섹션을 참고하세요.

## 개선 사항

현재 확인된 개선 필요 사항:
1. ❌ Error responses 문서화 부족
2. ⚠️ Description 구조화 필요
3. ❌ LoadTestController URL 일관성 (/api/v1 미포함)
4. ⚠️ 임시 인증 방식 (userId를 query parameter로 전달)

자세한 내용은 `api-design-audit.md`를 참고하세요.
