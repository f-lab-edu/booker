# Google OAuth2 Implementation Progress

**Date**: December 16, 2025
**Status**: ✅ Core Implementation Complete - Ready for Testing

---

## Implementation Summary

Google OAuth2 로그인 기능이 성공적으로 구현되었습니다. 클라이언트 측에서 인증을 관리하는 방식으로, Spring Security 없이 간단하게 구현했습니다.

### Architecture
```
사용자 클릭 "Login"
  ↓
Google OAuth 팝업 표시
  ↓
사용자가 Google 계정 선택
  ↓
Google ID Token 받음 (JWT)
  ↓
Next.js → Spring Boot에 토큰 전송
  ↓
Spring Boot가 Google에서 토큰 검증
  ↓
사용자 정보 반환
  ↓
Next.js localStorage에 저장
  ↓
로그인 완료! ✅
```

---

## Completed Changes

### Backend (Spring Boot) ✅

#### 1. Dependencies Added (`build.gradle`)
```gradle
// Google OAuth2 ID Token Validation
implementation 'com.google.api-client:google-api-client:2.2.0'
```

#### 2. New Files Created

**`GoogleTokenValidator.java`**
- Location: `src/main/java/com/bookerapp/core/infrastructure/security/`
- Purpose: Google ID Token 검증
- 기능:
  - Google의 공개 키로 토큰 서명 검증
  - 이메일 인증 여부 확인
  - 사용자 정보 추출

**`AuthController.java`**
- Location: `src/main/java/com/bookerapp/core/presentation/controller/`
- Endpoint: `POST /api/v1/auth/google/verify`
- 기능:
  - Google ID Token 받아서 검증
  - 사용자 정보 반환 (userId, email, name, picture)
  - 인증 실패 시 401 반환

**DTOs Created:**
- `GoogleLoginRequest.java` - 로그인 요청 DTO
- `AuthResponse.java` - 인증 응답 DTO

#### 3. Configuration (`application.yml`)
```yaml
google:
  oauth:
    client-id: ${GOOGLE_OAUTH_CLIENT_ID:your-client-id.apps.googleusercontent.com}
```

### Frontend (Next.js) ✅

#### 1. Dependencies Added
```bash
npm install @react-oauth/google
```

#### 2. Files Modified/Created

**`AuthContext.tsx` (완전히 재작성)**
- Google OAuth 지원으로 업데이트
- 기능:
  - `loginWithGoogle()` - Google 로그인 처리
  - Google ID Token을 백엔드로 전송
  - 사용자 정보 localStorage에 저장
  - `logout()` - 로그아웃 처리
  - `GoogleAuthProvider` wrapper 컴포넌트 추가

**`providers.tsx`**
- `AuthProvider` → `GoogleAuthProvider`로 변경
- Google OAuth Provider로 앱 전체를 감싸기

**`Header.tsx` (완전히 재작성)**
- Google Login 버튼이 있는 모달 추가
- 로그인 상태에 따라 UI 변경:
  - 비로그인: "Login" 버튼
  - 로그인: 프로필 사진 + 이름 + "Logout" 버튼
- Google Login 컴포넌트 통합:
  - One Tap 지원
  - 깔끔한 팝업 UI
  - Google 브랜드 로그인 버튼

#### 3. Environment Configuration
**`.env.local` (새로 생성)**
```env
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
NEXT_PUBLIC_API_BASE_URL=http://localhost:8084
```

---

## What's Left to Do

### 1. Google Cloud Console 설정 🔴 **필수**

다음 단계를 완료해야 합니다:

1. **Google Cloud Console 접속**
   - URL: https://console.cloud.google.com/

2. **프로젝트 생성 또는 선택**

3. **OAuth 2.0 Client ID 생성**
   - APIs & Services → Credentials
   - Create Credentials → OAuth 2.0 Client ID
   - Application type: Web application
   - Name: BOOKER

4. **Authorized JavaScript origins 추가**
   ```
   http://localhost:3000
   ```

5. **Authorized redirect URIs** (필요 없음, 팝업 방식 사용)

6. **Client ID 복사**
   - 형식: `xxxxx.apps.googleusercontent.com`

7. **환경 변수 설정**

   **Backend** (`booker-server`):
   ```bash
   export GOOGLE_OAUTH_CLIENT_ID="your-client-id.apps.googleusercontent.com"
   ```

   **Frontend** (`booker-client/.env.local`):
   ```
   NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   ```

### 2. Testing Checklist 🧪

테스트할 항목:

- [ ] Frontend 서버 시작: `cd booker-client && npm run dev`
- [ ] Backend 서버 시작: `cd booker-server && ./gradlew bootRun`
- [ ] Login 버튼 클릭 → 모달 표시
- [ ] Google Login 버튼 클릭 → Google OAuth 팝업
- [ ] Google 계정 선택 및 권한 승인
- [ ] 리다이렉트 후 로그인 완료
- [ ] Header에 사용자 이름과 프로필 사진 표시
- [ ] Logout 버튼 클릭 → 로그아웃
- [ ] 새로고침 후 로그인 상태 유지 확인

### 3. Optional Improvements (나중에) 💡

- [ ] Token refresh 로직 추가 (Google tokens expire in 1 hour)
- [ ] Protected routes 추가 (로그인 필요한 페이지)
- [ ] Error handling 개선 (네트워크 오류, 토큰 만료 등)
- [ ] Loading states 추가
- [ ] User profile dropdown menu

---

## File Structure

### Created/Modified Files

```
booker-server/
├── build.gradle                                     [MODIFIED]
├── src/main/java/com/bookerapp/core/
│   ├── infrastructure/security/
│   │   └── GoogleTokenValidator.java                [NEW]
│   ├── domain/model/
│   │   ├── auth/
│   │   │   └── GoogleUser.java                      [NEW]
│   │   └── dto/auth/
│   │       ├── GoogleLoginRequest.java              [NEW]
│   │       └── AuthResponse.java                    [NEW]
│   └── presentation/controller/
│       └── AuthController.java                      [NEW]
└── src/main/resources/
    └── application.yml                              [MODIFIED]

booker-client/
├── package.json                                     [MODIFIED]
├── .env.local                                       [NEW]
├── src/
│   ├── lib/auth/
│   │   └── AuthContext.tsx                          [MODIFIED]
│   ├── app/
│   │   └── providers.tsx                            [MODIFIED]
│   └── components/layout/
│       └── Header.tsx                               [MODIFIED]
```

---

## Key Technical Decisions

### 1. ❌ Spring Security를 사용하지 않음
**이유:**
- 클라이언트 측에서 인증 상태 관리
- 간단한 토큰 검증만 필요
- 복잡한 세션 관리 불필요
- 빠른 구현과 유지보수 용이

### 2. ✅ localStorage 사용
**선택 이유:**
- 클라이언트 측 인증 관리에 적합
- 새로고침 후 로그인 상태 유지
- 간단한 구현
- Google ID Token은 1시간 후 만료 (보안)

### 3. ✅ @react-oauth/google 사용
**선택 이유:**
- Google Identity Services 기반 (최신)
- React 전용 공식 wrapper
- One Tap 지원
- 간단한 API

### 4. ✅ HttpOnly Cookies 대신 localStorage
**이유:**
- 클라이언트 측 관리 방식
- API 호출 시 Authorization header로 전송 가능
- 백엔드 세션 관리 불필요

---

## Security Considerations

### 현재 구현된 보안:
- ✅ Google이 토큰 서명 검증
- ✅ 이메일 인증 확인
- ✅ CORS 설정 (application level)
- ✅ 토큰 만료 시간 (Google 관리: 1시간)

### 나중에 추가할 보안:
- [ ] HTTPS in production (필수)
- [ ] Token refresh mechanism
- [ ] Rate limiting on auth endpoint
- [ ] CSRF protection (if using cookies)

---

## How to Test

### 1. Backend 실행
```bash
cd booker-server

# 환경 변수 설정 (Google Client ID 필요)
export GOOGLE_OAUTH_CLIENT_ID="your-client-id.apps.googleusercontent.com"

# 실행
./gradlew bootRun
```

Backend가 `http://localhost:8084`에서 실행됨

### 2. Frontend 실행
```bash
cd booker-client

# .env.local 파일에 Client ID 설정 (이미 생성됨)
# NEXT_PUBLIC_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com

# 실행
npm run dev
```

Frontend가 `http://localhost:3000`에서 실행됨

### 3. 테스트 시나리오

1. **브라우저에서 `http://localhost:3000` 열기**

2. **Header의 "Login" 버튼 클릭**
   - 모달이 나타남

3. **"Continue with Google" 버튼 클릭**
   - Google OAuth 팝업 표시
   - Google 계정 선택
   - 권한 승인 (이메일, 프로필 정보)

4. **로그인 성공 확인**
   - 모달이 닫힘
   - Header에 프로필 사진과 이름 표시
   - "Logout" 버튼으로 변경

5. **페이지 새로고침**
   - 로그인 상태 유지됨

6. **Logout 버튼 클릭**
   - 로그아웃 됨
   - "Login" 버튼으로 다시 변경

---

## API Endpoints

### POST /api/v1/auth/google/verify
Google ID Token을 검증합니다.

**Request:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```

**Response (Success - 200):**
```json
{
  "userId": "123456789",
  "email": "user@gmail.com",
  "name": "홍길동",
  "picture": "https://lh3.googleusercontent.com/...",
  "authenticated": true
}
```

**Response (Failure - 401):**
```json
{
  "authenticated": false
}
```

---

## Troubleshooting

### 문제: "NEXT_PUBLIC_GOOGLE_CLIENT_ID is not set" 경고
**해결:** `.env.local` 파일에 Client ID 설정

### 문제: Google 팝업이 차단됨
**해결:** 브라우저 팝업 차단 해제

### 문제: "Failed to validate Google token"
**해결:**
1. Backend에 Client ID 환경 변수 설정 확인
2. Google Cloud Console에서 Client ID가 유효한지 확인
3. Authorized origins에 `http://localhost:3000` 추가 확인

### 문제: CORS 오류
**해결:** Spring Boot WebConfig에서 CORS 설정 확인

---

## Next Steps

1. **Google Cloud Console 설정 완료** ← 가장 중요!
2. **환경 변수 설정**
3. **테스트 실행**
4. **문제 발생 시 로그 확인**

설정을 완료하면 바로 테스트 가능합니다!
