# Docker 웹 클라이언트 실행 스크립트 구현 계획

## 현재 상황 분석

### booker-client 프로젝트 현황
- **프레임워크**: Next.js (next-env.d.ts 확인됨)
- **디렉토리 구조**:
  - `src/` (hooks, stores, types)
  - `.next/` (빌드 결과물)
  - `node_modules/` (의존성 설치됨)
- **누락된 파일**:
  - `package.json` (gitignore에 포함되었거나 미생성)
  - `Dockerfile` (없음)
  - `.dockerignore` (없음)

### 기존 인프라
- `docker-compose.yml` 존재 (postgres, mysql, keycloak, springboot, prometheus, grafana 등)
- booker-client 서비스는 docker-compose.yml에 미포함

## 구현 계획 (MVP 접근)

### 1. package.json 확인 및 생성
**목적**: Next.js 프로젝트의 의존성 및 스크립트 정의

**작업**:
- booker-client 디렉토리에 package.json이 gitignore에 포함되어 있는지 확인
- 없다면 기본 Next.js package.json 템플릿 생성
- 필요한 의존성: next, react, react-dom, typescript 등
- 빌드 스크립트: build, start, dev

**선택사항**:
- 사용자에게 기존 package.json 정보 확인 (git history, 백업 등)

### 2. Dockerfile 생성
**목적**: Next.js 애플리케이션의 도커 이미지 생성

**작업**:
- Multi-stage 빌드 사용 (빌드 단계 + 실행 단계)
- 베이스 이미지: node:18-alpine 또는 node:20-alpine
- 빌드 최적화:
  - 의존성 캐싱 (.dockerignore 활용)
  - 프로덕션 빌드
- 환경 변수 지원 (.env.local 활용)
- 포트 노출: 3000 (Next.js 기본 포트)

**Dockerfile 구조**:
```dockerfile
# 1단계: 의존성 설치 및 빌드
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 2단계: 실행 환경
FROM node:18-alpine AS runner
WORKDIR /app
ENV NODE_ENV production
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json
EXPOSE 3000
CMD ["npm", "start"]
```

### 3. .dockerignore 생성
**목적**: 불필요한 파일 제외로 빌드 속도 향상

**작업**:
- node_modules, .next, .git 등 제외
- 개발 파일 제외 (.env.local은 포함 여부 결정)

### 4. 쉘 스크립트 생성
**목적**: 간편한 도커 빌드 및 실행

**작업**:
- 스크립트 이름: `start-client.sh` (루트 디렉토리)
- 기능:
  1. 이전 컨테이너 정리 (선택사항)
  2. 도커 이미지 빌드
  3. 컨테이너 실행 (포트 매핑, 환경 변수 주입)
  4. 로그 출력

**스크립트 구조**:
```bash
#!/bin/bash

# 설정
IMAGE_NAME="booker-client"
CONTAINER_NAME="booker-client-app"
PORT=3001  # 호스트 포트 (3000은 grafana가 사용 중)

# 기존 컨테이너 중지 및 제거
docker stop $CONTAINER_NAME 2>/dev/null
docker rm $CONTAINER_NAME 2>/dev/null

# 이미지 빌드
echo "Building Docker image..."
docker build -t $IMAGE_NAME ./booker-client

# 컨테이너 실행
echo "Starting container..."
docker run -d \
  --name $CONTAINER_NAME \
  -p $PORT:3000 \
  --env-file ./booker-client/.env.local \
  $IMAGE_NAME

echo "Client is running at http://localhost:$PORT"
docker logs -f $CONTAINER_NAME
```

### 5. (선택사항) docker-compose.yml에 서비스 추가
**목적**: 전체 스택 통합 관리

**작업**:
- booker-client 서비스 추가
- springboot 서비스와 연결
- 네트워크 설정

## 실행 순서

1. package.json 상태 확인 (사용자에게 질문)
2. Dockerfile 생성
3. .dockerignore 생성
4. start-client.sh 스크립트 생성
5. 스크립트 실행 권한 부여 (chmod +x)
6. 테스트 실행

## 예상 이슈 및 해결방안

### 이슈 1: package.json 없음
**해결**:
- 사용자에게 확인 후 기본 템플릿 생성
- 또는 git history에서 복구

### 이슈 2: 환경 변수 관리
**해결**:
- .env.local 파일 활용
- 도커 실행 시 --env-file 옵션 사용

### 이슈 3: 포트 충돌
**해결**:
- 3000 포트는 grafana가 사용 중
- 호스트 포트를 3001로 매핑

## 테스트 계획

1. 도커 이미지 빌드 성공 확인
2. 컨테이너 실행 및 포트 접근 확인
3. Next.js 앱 정상 로드 확인
4. 환경 변수 주입 확인

## 추가 고려사항

- **보안**: .env 파일을 git에 커밋하지 않도록 확인
- **최적화**: Next.js standalone 모드 활용 가능
- **모니터링**: 헬스체크 추가 가능
- **확장성**: docker-compose 통합 고려

---

## 구현 완료 내역 (2025-12-13)

### 완료된 작업

1. **Git history에서 파일 복구** ✓
   - 커밋 `3d50731`에서 모든 필요한 파일 발견 및 복구
   - package.json, Dockerfile, 설정 파일들 모두 복구 완료

2. **복구된 파일 목록** ✓
   - `package.json` - Next.js 16, React 19, TypeScript 5.9 등
   - `Dockerfile` - Multi-stage 빌드 (Node 20 Alpine)
   - `.dockerignore` - 빌드 최적화
   - `next.config.mjs` - Standalone 모드 활성화, API URL 설정
   - `.eslintrc.json` - Next.js ESLint 설정
   - `postcss.config.js` - Tailwind CSS 설정
   - `tailwind.config.js` - 커스텀 테마 (primary, violet, status colors)
   - `tsconfig.json` - TypeScript 설정
   - `.gitignore` - Next.js 표준 ignore 규칙
   - `src/` 디렉토리 전체 (app, components, lib, config 등)

3. **Docker Compose 통합** ✓
   - docker-compose.yml에 booker-client 서비스 추가 (docker-compose.yml:216)
   - 포트 매핑: 3001(host) → 3000(container)
   - 환경 변수 설정: NEXT_PUBLIC_API_BASE_URL=http://springboot:8084
   - springboot 서비스 의존성 설정
   - 자동 재시작 정책 적용

4. **실행 스크립트 생성** ✓
   - `start-client.sh` 생성 및 실행 권한 부여 (start-client.sh:1)
   - 컬러 출력으로 사용자 친화적 인터페이스
   - 기존 컨테이너 자동 정리
   - 의존 서비스 확인 (springboot)
   - 대화형 프롬프트 (전체 스택 시작, 로그 보기)
   - 실시간 로그 확인 옵션

### 사용 방법

#### 방법 1: 스크립트 사용 (권장)
```bash
./start-client.sh
```

#### 방법 2: Docker Compose 직접 사용
```bash
# 단독 실행
docker-compose up -d --build booker-client

# 전체 스택 실행
docker-compose up -d
```

#### 접속 URL
- 웹 클라이언트: http://localhost:3001
- API 서버: http://localhost:8084
- Grafana: http://localhost:3000

### 기술 스택 세부사항

- **Frontend**: Next.js 16 (App Router)
- **UI**: React 19, Tailwind CSS 3.4
- **상태관리**: Zustand 5.0, TanStack Query 5.90
- **폼**: React Hook Form 7.68, Zod 4.1
- **애니메이션**: Framer Motion 12.23
- **아이콘**: Lucide React 0.561
- **HTTP**: Axios 1.13
- **빌드**: TypeScript 5.9, ESLint 9.39

### 주요 기능

- 책 목록 조회 (src/app/books/page.tsx)
- 이벤트 관리 (src/app/events/page.tsx)
- 대출 관리 (src/app/my-loans/page.tsx)
- 프로필 (src/app/profile/page.tsx)
- 3D Shader 효과 Hero 배너 (src/components/hero/ShaderCanvas.tsx)
- 반응형 네비게이션 (src/components/navigation/)
- API 통합 (src/lib/api/)
- 인증 컨텍스트 (src/lib/auth/AuthContext.tsx)

### 다음 단계 제안

1. ~~의존성 설치 확인 (`npm ci` 실행 필요시)~~
   - Dockerfile이 자동으로 처리
2. 환경 변수 설정 확인 (.env.local)
3. API 서버 연동 테스트
4. 프로덕션 빌드 최적화 검증

---

## 실행 완료 (2025-12-13 05:13)

### 해결한 이슈

1. **package-lock.json 누락** ✓
   - Git history에서 복구 (223KB)
   - npm ci 빌드 성공

2. **TypeScript 타입 에러** ✓
   - 파일: `src/lib/api/client.ts:37`
   - 문제: HeadersInit 타입에 인덱스 접근 불가
   - 해결: `Record<string, string>` 명시적 타입 지정
   ```typescript
   const headers: Record<string, string> = {
     'Content-Type': 'application/json',
     ...(options?.headers as Record<string, string>),
   };
   ```

3. **포트 충돌** ✓
   - 기존 `booker_nextjs` 컨테이너가 3001 포트 사용 중
   - 컨테이너 중지 및 제거 후 재시작

### 실행 결과

- **컨테이너 상태**: Up and Running ✓
- **포트**: 0.0.0.0:3001->3000/tcp ✓
- **HTTP 응답**: 200 OK ✓
- **메모리 사용량**: 26.07 MiB ✓
- **시작 시간**: 115ms ✓

### 접속 확인

```bash
# HTTP 상태 확인
curl http://localhost:3001
# → HTTP 200 OK

# 로그 확인
docker logs booker-client
# → ✓ Ready in 115ms
```

### 실행 중인 서비스

- booker-client: http://localhost:3001
- springboot: http://localhost:8084, http://localhost:8085
- grafana: http://localhost:3000
