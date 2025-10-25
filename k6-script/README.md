# K6 동시성 제어 메커니즘 로드 테스트

이 디렉토리는 Booker 애플리케이션의 다양한 동시성 제어 메커니즘을 비교하기 위한 K6 로드 테스트 스크립트를 포함합니다.

## 테스트 대상

다음 4가지 동시성 제어 메커니즘을 비교합니다:

1. **Optimistic Locking** (`OptimisticLockEventParticipationService`)
   - JPA `@Version`을 사용한 낙관적 잠금
   - 충돌 시 재시도 로직 포함

2. **Pessimistic Locking** (`PessimisticLockEventParticipationService`)
   - 데이터베이스 레벨 비관적 잠금
   - `@Lock(LockModeType.PESSIMISTIC_WRITE)` 사용

3. **Compare-and-Swap (CAS)** (`CasEventParticipationService`)
   - 원자적 비교 후 교체 연산
   - `@Retryable`을 통한 재시도 메커니즘

4. **Synchronized** (`SynchronizedEventParticipationService`)
   - JVM 레벨 `synchronized` 키워드
   - 애플리케이션 레벨 동기화

## 파일 구조

```
k6-script/
├── concurrency-load-test.js     # 메인 K6 테스트 스크립트
├── run-concurrency-test.sh      # 테스트 실행 스크립트
├── book-loan-load-test.js        # 기존 도서 대출 테스트
└── README.md                     # 이 파일
```

## 사전 요구사항

### 1. K6 설치

```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo apt update && sudo apt install k6

# Windows (Chocolatey)
choco install k6
```

### 2. 서버 실행

테스트를 실행하기 전에 Booker 서버가 실행되어 있어야 합니다:

```bash
# Docker Compose로 전체 스택 실행
docker-compose up -d

# 또는 Spring Boot 서버만 실행
cd booker-server && ./gradlew bootRun
```

### 3. 테스트 데이터 준비

로드 테스트는 기본적으로 `eventId: 1`을 사용합니다. 테스트 전에 해당 이벤트가 존재하는지 확인하세요.

## 테스트 실행

### 방법 1: 실행 스크립트 사용 (권장)

```bash
# 기본 테스트 실행
./run-concurrency-test.sh

# 사전 확인만 수행
./run-concurrency-test.sh --dry-run

# 이전 결과 파일 정리
./run-concurrency-test.sh --clean

# 도움말 보기
./run-concurrency-test.sh --help
```

### 방법 2: K6 직접 실행

```bash
# 기본 실행
k6 run concurrency-load-test.js

# 결과를 JSON으로 저장
k6 run --out json=results.json concurrency-load-test.js

# HTML 리포트 생성
k6 run concurrency-load-test.js
```

## 테스트 시나리오

테스트는 다음과 같은 시나리오로 구성됩니다:

### 1. 낮은 부하 (Low Load)
- **VUs**: 5개
- **지속시간**: 30초
- **목적**: 기본 기능 검증

### 2. 중간 부하 (Medium Load)
- **VUs**: 20개
- **지속시간**: 60초
- **시작시간**: 35초 후
- **목적**: 일반적인 부하 상황에서의 성능 측정

### 3. 높은 부하 (High Load)
- **VUs**: 50개
- **지속시간**: 120초
- **시작시간**: 100초 후
- **목적**: 동시성 문제가 발생하는 상황에서의 성능 비교

### 4. 스파이크 부하 (Spike Load)
- **VUs**: 0 → 100 → 0
- **패턴**: 10초 증가, 30초 유지, 10초 감소
- **시작시간**: 225초 후
- **목적**: 순간적인 트래픽 급증 상황 테스트

## 메트릭 및 임계값

### 응답시간 임계값
- **전체**: 95%가 5초 이내
- **Optimistic**: 95%가 3초 이내
- **Pessimistic**: 95%가 5초 이내
- **CAS**: 95%가 3초 이내
- **Synchronized**: 95%가 2초 이내

### 실패율 임계값
- **전체**: 5% 미만

### 수집되는 메트릭
- **응답시간**: 각 서비스별 응답시간 분포
- **처리량**: 초당 처리 요청 수
- **오류율**: 실패한 요청의 비율
- **재시도 횟수**: Optimistic Locking과 CAS의 재시도 횟수

## 결과 분석

### 1. 실시간 모니터링

테스트 중 Grafana 대시보드에서 실시간 메트릭을 확인할 수 있습니다:
- **URL**: http://localhost:3000
- **사용자명/비밀번호**: admin/admin

### 2. 테스트 결과 파일

테스트 완료 후 `results/` 디렉토리에 다음 파일들이 생성됩니다:
- **JSON 결과**: 상세 메트릭 데이터
- **HTML 리포트**: 시각적 결과 요약
- **로그 파일**: 실행 로그

### 3. 재시도 메트릭

Optimistic Locking과 CAS 서비스의 재시도 횟수를 확인할 수 있습니다:

```bash
# 재시도 횟수 조회
curl http://localhost:8084/api/load-test/metrics/optimistic
curl http://localhost:8084/api/load-test/metrics/cas

# 재시도 카운터 리셋
curl -X POST http://localhost:8084/api/load-test/reset/optimistic
curl -X POST http://localhost:8084/api/load-test/reset/cas
```

## 성능 비교 포인트

### 1. 응답시간 (Response Time)
- 각 메커니즘의 평균/중간값/95% 응답시간 비교
- 부하 증가에 따른 응답시간 변화

### 2. 처리량 (Throughput)
- 초당 처리할 수 있는 요청 수
- 동시 사용자 수 증가에 따른 처리량 변화

### 3. 재시도 빈도
- Optimistic Locking의 충돌로 인한 재시도 횟수
- CAS의 실패로 인한 재시도 횟수

### 4. 리소스 사용률
- CPU, 메모리 사용률 (Grafana에서 확인)
- 데이터베이스 연결 및 잠금 상태

### 5. 확장성 (Scalability)
- 부하 증가에 따른 성능 저하 패턴
- 임계점 식별

## 예상 결과

일반적으로 다음과 같은 특성을 보일 것으로 예상됩니다:

1. **Synchronized**: 낮은 부하에서는 빠르지만 확장성 제한
2. **Pessimistic**: 안정적이지만 상대적으로 느림
3. **Optimistic**: 중간 수준의 성능, 높은 부하에서 재시도 증가
4. **CAS**: 높은 성능이지만 구현 복잡성

## 문제 해결

### 1. 서버 연결 실패
```bash
# 서버 상태 확인
curl http://localhost:8084/actuator/health
curl http://localhost:8084/api/load-test/health
```