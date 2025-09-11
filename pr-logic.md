# Pull Request: 이벤트 참여 동시성 제어 로직 구현

## 🎯 PR 개요
이벤트 참여 시스템에 대한 동시성 제어 메커니즘을 구현하여 대용량 트래픽 상황에서도 데이터 일관성과 안정성을 보장합니다. 4가지 동시성 제어 전략을 구현하고 성능을 비교 분석할 수 있는 테스트 환경을 제공합니다.

## 🔧 주요 변경사항

### 1. 동시성 제어 서비스 구현 (4가지)

#### Synchronized 방식 (`SynchronizedEventParticipationService`)
- **구현 방식**: Java `synchronized` 키워드 활용
- **특징**: 
  - 단일 JVM 환경에서만 동작
  - 구현이 간단하고 직관적
  - 메서드 레벨 동기화로 스레드 안전성 보장
- **성능**: 동시성이 제한되어 처리량이 낮음
- **사용 사례**: 단일 서버 환경, 동시성 요구사항이 낮은 경우

#### CAS (Compare-And-Swap) 방식 (`CasEventParticipationService`)
```java
@Retryable(
    value = {OptimisticLockException.class},
    maxAttempts = 10,
    backoff = @Backoff(delay = 10)
)
public EventParticipationDto.Response participateInEvent(EventParticipationDto.Request request)
```
- **구현 방식**: `@Retryable` 어노테이션과 낙관적 락 활용
- **특징**:
  - Lock-free 알고리즘 구현
  - 높은 동시성과 성능
  - 자동 재시도 메커니즘 (최대 10회)
- **성능**: 충돌이 적은 환경에서 최고 성능
- **사용 사례**: 높은 동시성이 필요하고 충돌 빈도가 낮은 경우

#### 낙관적 락 (`OptimisticLockEventParticipationService`)
```java
@Version
@Column(name = "version", nullable = false)
private Long version = 0L;  // BaseEntity에 추가
```
- **구현 방식**: JPA `@Version` 필드와 OptimisticLockException 처리
- **특징**:
  - 데이터베이스 레벨 버전 관리
  - 충돌 시 수동 재시도 로직 (최대 10회, 백오프 전략)
  - 분산 환경 지원
- **성능**: 중간 수준, 충돌 발생 시 재시도 오버헤드
- **사용 사례**: 분산 환경에서 충돌이 적은 경우

#### 비관적 락 (`PessimisticLockEventParticipationService`)
```java
Event event = entityManager.find(Event.class, request.getEventId(), LockModeType.PESSIMISTIC_WRITE);
```
- **구현 방식**: JPA `LockModeType.PESSIMISTIC_WRITE` 활용
- **특징**:
  - 데이터베이스 레벨 배타적 락
  - 데드락 방지를 위한 순차 처리
  - 가장 높은 데이터 일관성 보장
- **성능**: 가장 낮음, 순차 처리로 인한 대기 시간 발생
- **사용 사례**: 데이터 일관성이 최우선이고 충돌이 빈번한 경우

### 2. 도메인 모델 개선

#### BaseEntity 확장
```java
@Version
@Column(name = "version", nullable = false)
private Long version = 0L;
```
- 낙관적 락을 위한 버전 필드 추가
- 모든 엔티티에 자동 적용되는 공통 기능

#### Event 엔티티 비즈니스 로직
```java
public boolean isFullyBooked() {
    return getConfirmedParticipants().size() >= maxParticipants;
}

public void promoteFromWaitingList() {
    // 대기자를 확정 참가자로 승격하는 로직
}
```
- 이벤트 참여 상태 관리
- 대기 순번 자동 관리
- 참가자 승격 로직

### 3. 포괄적인 테스트 환경

#### 성능 비교 테스트 (`EventParticipationPerformanceComparisonTest`)
```java
@Test
@DisplayName("동시성 제어 방식별 종합 성능 비교 테스트")
void comprehensivePerformanceComparisonTest() throws InterruptedException {
    // 워밍업 + 실제 측정으로 정확한 성능 비교
    // 평균, 최소, 최대 처리 시간 측정
    // 상대적 성능 비교 (Synchronized 대비)
}
```

#### 개별 서비스 테스트
- `OptimisticLockEventParticipationServiceTest`: 낙관적 락 동작 검증
- `PessimisticLockEventParticipationServiceTest`: 비관적 락 순차 처리 검증
- `EventParticipationConcurrencyTest`: 기존 동시성 테스트 확장

#### 테스트 시나리오
- **정확성 검증**: 동시 요청 시 정확히 maxParticipants만큼만 확정
- **대기 순번 정확성**: 대기자 순번이 1부터 연속적으로 부여
- **중복 참여 방지**: 동일 사용자의 중복 신청 차단
- **성능 측정**: 각 방식별 처리 시간과 재시도/락 사용 횟수 측정

### 4. 통계 및 모니터링

#### 재시도 통계
```java
// CAS 방식
public int getRetryCount() { return retryCounter.get(); }

// 낙관적 락 방식  
public int getRetryCount() { return retryCounter.get(); }

// 비관적 락 방식
public int getLockCount() { return lockCount.get(); }
```

## 📊 성능 특성 분석

### 예상 성능 순서 (처리량 기준)
1. **CAS** > **낙관적 락** > **Synchronized** > **비관적 락**

### 동시성 레벨별 적합성
- **낮은 동시성 (1-10 사용자)**: Synchronized
- **중간 동시성 (10-100 사용자)**: 낙관적 락
- **높은 동시성 (100+ 사용자)**: CAS
- **충돌 빈발 환경**: 비관적 락

### 환경별 추천 전략
- **단일 서버**: Synchronized → 낙관적 락 → CAS
- **분산 환경**: 낙관적 락 → CAS → 비관적 락
- **MSA 환경**: CAS → 낙관적 락

## 🧪 테스트 결과

### 테스트 환경
- 참가자 수: 10명
- 동시 요청 수: 50명  
- 워밍업 라운드: 3회
- 측정 라운드: 5회

### 검증 항목
✅ **데이터 일관성**: 모든 방식에서 정확히 10명만 확정, 40명 대기
✅ **대기 순번 정확성**: 1~40번 연속 부여
✅ **중복 참여 방지**: 동일 사용자 재신청 시 차단
✅ **동시성 안전성**: 경쟁 조건 없이 안전한 처리

## 🔄 확장 계획

### 단계별 발전 방향
1. **Redis 분산 락**: 완전한 분산 환경 지원
2. **이벤트 대기열**: 메시지 큐를 활용한 비동기 처리
3. **실시간 모니터링**: 처리 현황 대시보드
4. **자동 전략 선택**: 부하 상황에 따른 동적 전략 변경

### 성능 최적화
- 데이터베이스 인덱스 최적화
- 커넥션 풀 튜닝
- 캐시 전략 도입

## 📋 체크리스트
- [x] 4가지 동시성 제어 방식 구현
- [x] 포괄적인 테스트 케이스 작성
- [x] 성능 비교 테스트 환경 구축
- [x] 통계 수집 및 모니터링 기능
- [x] 도메인 모델 개선 (버전 필드 추가)
- [x] 코드 문서화 및 주석
- [ ] 실제 환경 성능 테스트 수행
- [ ] 부하 테스트 결과 분석
- [ ] 운영 환경 배포 가이드 작성

## 🚀 배포 가이드

### 데이터베이스 마이그레이션
```sql
-- BaseEntity version 필드 추가
ALTER TABLE event ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE event_participation ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
-- 기타 엔티티들도 동일하게 적용
```

### 애플리케이션 설정
```yaml
spring:
  jpa:
    properties:
      hibernate:
        show_sql: false  # 운영환경에서는 false
        format_sql: false
        use_sql_comments: false
```

### 모니터링 메트릭
- 각 방식별 처리 시간 평균/최대값
- 재시도/락 사용 횟수
- 실패율 및 타임아웃 발생률
- 동시 사용자 수별 성능 변화