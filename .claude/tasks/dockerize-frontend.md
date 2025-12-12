# 프론트엔드 도커화 작업

## 목표
Next.js 프론트엔드 애플리케이션을 도커 컨테이너로 실행하도록 설정

## 현재 상황 분석

### 기존 환경
- **프레임워크**: Next.js 16.0.10
- **실행 방식**: `npm run dev` (로컬 개발 서버)
- **포트**: 3001
- **API 엔드포인트**: `http://localhost:8080` (설정상), 실제는 `http://localhost:8084`로 보임
- **의존성**: React 19, TanStack Query, Zustand, Tailwind CSS 등

### 기존 Docker 인프라
- **docker-compose.yml**: MySQL, SpringBoot, Prometheus, Grafana, InfluxDB, K6
- **SpringBoot 서비스**: 포트 8084
- **참고할 Dockerfile**: booker-server/Dockerfile (multi-stage build 패턴 사용)

## 구현 계획

### 1. Next.js Dockerfile 생성
**위치**: `/booker-client/Dockerfile`

**전략**: Multi-stage build
- **Stage 1 (deps)**: 의존성 설치
- **Stage 2 (builder)**: 프로덕션 빌드
- **Stage 3 (runner)**: 최소 런타임 이미지

**주요 설정**:
- Node.js 20 Alpine 이미지 사용 (경량화)
- Next.js standalone output 활성화 (이미지 크기 최소화)
- 환경변수를 통한 API URL 설정

### 2. .dockerignore 생성
**위치**: `/booker-client/.dockerignore`

**제외 항목**:
- `node_modules`
- `.next`
- `npm-debug.log`
- `.git`
- `README.md`

### 3. next.config.mjs 수정
**변경사항**:
- `output: 'standalone'` 추가 (Docker 최적화)
- 환경변수 처리 개선

### 4. docker-compose.yml 업데이트
**추가할 서비스**:
```yaml
nextjs:
  build:
    context: booker-client
    dockerfile: Dockerfile
  container_name: booker_nextjs
  ports:
    - "3000:3000"
  environment:
    - NEXT_PUBLIC_API_BASE_URL=http://localhost:8084
  depends_on:
    - springboot
  restart: unless-stopped
```

**고려사항**:
- 프론트엔드는 브라우저에서 실행되므로 API URL은 `localhost:8084` 유지
- 만약 SSR API 호출이 있다면 내부 통신용 URL 별도 설정 필요

### 5. 환경변수 관리
**.env.local** (선택사항):
- 로컬 개발용 환경변수
- Docker에서는 docker-compose.yml의 environment로 주입

## 실행 방법

### 개발 모드 (기존 방식 유지 가능)
```bash
cd booker-client
npm run dev
```

### Docker로 프로덕션 빌드 실행
```bash
docker-compose up nextjs
```

### 전체 스택 실행
```bash
docker-compose up
```

## 예상 이점
1. **환경 일관성**: 개발/프로덕션 환경 통일
2. **배포 용이성**: 컨테이너 기반 배포
3. **의존성 격리**: 로컬 환경 오염 방지
4. **확장성**: 수평 확장 용이

## 주의사항
1. **개발 모드 hot-reload**: Docker 개발 모드는 별도 설정 필요 (현재는 프로덕션 빌드)
2. **빌드 시간**: 초기 빌드는 시간 소요 (이후 캐싱으로 개선)
3. **포트 충돌**: 현재 실행 중인 npm dev 서버 종료 필요

## 태스크 체크리스트
- [x] 현재 구조 분석
- [x] Dockerfile 작성
- [x] .dockerignore 작성
- [x] next.config.mjs 수정
- [x] docker-compose.yml 업데이트
- [x] 빌드 및 실행 테스트
- [x] 문서화

## 완료된 작업 내용

### 1. Dockerfile 생성 (/booker-client/Dockerfile)
- Multi-stage build 적용 (deps, builder, runner)
- Node.js 20 Alpine 이미지 사용
- 최종 이미지 크기 최적화
- 보안: 비root 사용자(nextjs) 실행
- TypeScript 타입 에러 수정: `ShaderCanvas.tsx`의 `useRef<number>()` → `useRef<number | undefined>(undefined)`
- public 디렉토리 없음을 반영하여 COPY 단계 수정

### 2. .dockerignore 생성 (/booker-client/.dockerignore)
- node_modules, .next, 로그 파일, IDE 설정 등 제외
- 빌드 컨텍스트 크기 최소화

### 3. next.config.mjs 수정
- `output: 'standalone'` 추가 (Docker 최적화)
- API URL 수정: `http://localhost:8080` → `http://localhost:8084`

### 4. docker-compose.yml 업데이트
- nextjs 서비스 추가
- 포트: 3001:3000 (Grafana 충돌 회피)
- 리소스 제한: CPU 0.5, Memory 512M
- springboot 서비스 의존성 설정

### 5. 빌드 및 실행 결과
- 빌드 시간: ~44초
- 이미지 생성 완료: `booker-nextjs`
- 컨테이너 실행 상태: ✅ Running
- 접속 테스트: ✅ HTTP 200 OK
- 실행 URL: http://localhost:3001

### 6. 발견된 이슈 및 해결
1. **TypeScript 에러**: `useRef<number>()`에 초기값 필요 → `undefined` 추가
2. **public 디렉토리 없음**: Dockerfile COPY 단계에서 제거
3. **포트 충돌**: Grafana(3000) 충돌 → 3001로 변경

## 최종 실행 방법

### 프론트엔드만 실행
```bash
docker-compose up nextjs
```

### 전체 스택 실행
```bash
docker-compose up
```

### 백그라운드 실행
```bash
docker-compose up -d
```

### 로그 확인
```bash
docker logs -f booker_nextjs
```

### 중지
```bash
docker-compose down
```

## 성과
✅ Next.js 프론트엔드가 Docker 컨테이너로 성공적으로 실행됨
✅ Multi-stage build로 최적화된 프로덕션 이미지 생성
✅ 기존 백엔드(SpringBoot, MySQL) 인프라와 통합
✅ 개발 환경 일관성 확보

## 참고자료
- Next.js Docker 공식 문서: https://nextjs.org/docs/deployment#docker-image
- Next.js standalone output: https://nextjs.org/docs/advanced-features/output-file-tracing
