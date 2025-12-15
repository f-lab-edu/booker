# Linux Terminal Developer Center - Implementation Plan

## 프로젝트 개요
웹 기반 리눅스 터미널 환경을 개발자센터에 추가하여, 사용자들이 브라우저에서 직접 리눅스 명령어를 실습하고 테스트할 수 있는 환경을 제공합니다.

## 기술 스택 (MVP)

### Core Libraries
- **Terminal UI**: `@xterm/xterm` v5.x+ (MIT License)
  - `@xterm/addon-fit` - 자동 리사이즈
  - `@xterm/addon-web-links` - 클릭 가능한 링크
- **Virtual Filesystem**: `@zenfs/core` (InMemory backend)
- **Command Execution**: Custom command parser (보안 강화)
- **Framework**: Next.js 15+ (App Router)

### 선택 이유
1. **보안**: 커스텀 파서로 허용된 명령어만 실행
2. **성능**: 가벼운 라이브러리, 클라이언트 사이드만 사용
3. **유지보수**: 업계 표준 라이브러리 사용 (VS Code도 xterm.js 사용)
4. **확장성**: 추후 WebContainers나 BrowserPod로 업그레이드 가능

## MVP 기능 범위

### Phase 1: 기본 터미널 UI (우선순위: 높음)
- [x] xterm.js 터미널 컴포넌트 생성
- [x] Next.js SSR 비활성화 설정
- [x] 기본 테마 및 스타일링 (다크 모드)
- [x] 프롬프트 표시 (user@booker:~$)

### Phase 2: 가상 파일시스템 (우선순위: 높음)
- [ ] ZenFS InMemory 파일시스템 통합
- [ ] 기본 디렉토리 구조 생성
  ```
  /
  ├── home/
  │   └── user/
  │       ├── documents/
  │       ├── projects/
  │       └── README.txt
  ├── etc/
  └── tmp/
  ```
- [ ] 샘플 파일 생성 (README, 예제 코드 등)

### Phase 3: 기본 명령어 구현 (우선순위: 높음)
1. **파일 시스템 탐색**
   - `ls` - 파일/디렉토리 목록 (옵션: -l, -a)
   - `cd` - 디렉토리 이동
   - `pwd` - 현재 경로 표시

2. **파일 조작**
   - `cat` - 파일 내용 표시
   - `mkdir` - 디렉토리 생성
   - `touch` - 빈 파일 생성
   - `rm` - 파일/디렉토리 삭제 (옵션: -r)
   - `cp` - 파일 복사
   - `mv` - 파일 이동/이름 변경

3. **유틸리티**
   - `echo` - 텍스트 출력
   - `clear` - 화면 클리어
   - `help` - 사용 가능한 명령어 목록
   - `whoami` - 현재 사용자 표시

### Phase 4: 향상된 기능 (우선순위: 중간)
- [ ] 명령어 히스토리 (↑↓ 키)
- [ ] Tab 자동완성
- [ ] 파이프 연산자 지원 (|)
- [ ] 리다이렉션 지원 (>, >>)
- [ ] 컬러 출력 지원

### Phase 5: UI/UX 개선 (우선순위: 낮음)
- [ ] 풀스크린 모드
- [ ] 폰트 크기 조절
- [ ] 테마 변경 (여러 컬러 스킴)
- [ ] 명령어 출력 복사 기능

## 구현 세부사항

### 1. 파일 구조
```
booker-client/src/
├── components/
│   └── terminal/
│       ├── LinuxTerminal.tsx        # 메인 터미널 컴포넌트
│       ├── useTerminal.ts           # 터미널 로직 훅
│       ├── CommandParser.ts         # 명령어 파싱
│       ├── FileSystemManager.ts     # 파일시스템 래퍼
│       └── commands/
│           ├── index.ts             # 명령어 레지스트리
│           ├── ls.ts
│           ├── cd.ts
│           ├── cat.ts
│           ├── pwd.ts
│           ├── mkdir.ts
│           ├── rm.ts
│           ├── echo.ts
│           └── help.ts
├── app/
│   └── developer/
│       └── terminal/
│           └── page.tsx             # 터미널 페이지
└── types/
    └── terminal.ts                  # 타입 정의
```

### 2. 컴포넌트 설계

#### LinuxTerminal.tsx
```typescript
'use client';

import { useEffect, useRef } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';

export function LinuxTerminal() {
  const terminalRef = useRef<HTMLDivElement>(null);
  const xtermRef = useRef<Terminal | null>(null);

  // Terminal 초기화, 명령어 처리, 입력 핸들링

  return <div ref={terminalRef} className="w-full h-full" />;
}
```

#### CommandParser.ts
```typescript
type CommandHandler = (args: string[], fs: FileSystem) => string | Promise<string>;

const ALLOWED_COMMANDS = ['ls', 'cd', 'cat', 'pwd', 'mkdir', 'rm', 'echo', 'clear', 'help'];

export class CommandParser {
  private commands: Map<string, CommandHandler>;

  execute(input: string): string {
    const [cmd, ...args] = input.trim().split(/\s+/);

    // Allowlist 체크
    if (!ALLOWED_COMMANDS.includes(cmd)) {
      return `bash: ${cmd}: command not found`;
    }

    // 명령어 실행
    const handler = this.commands.get(cmd);
    return handler ? handler(args, this.fs) : `Command not implemented: ${cmd}`;
  }
}
```

### 3. 보안 구현

#### Command Allowlisting
```typescript
const ALLOWED_COMMANDS = [
  'ls', 'cd', 'cat', 'pwd', 'mkdir', 'rm', 'cp', 'mv',
  'touch', 'echo', 'clear', 'help', 'whoami'
];

function isCommandAllowed(cmd: string): boolean {
  return ALLOWED_COMMANDS.includes(cmd);
}
```

#### Path Validation
```typescript
function validatePath(path: string): boolean {
  const normalized = normalizePath(path);

  // 디렉토리 트래버설 방지
  if (normalized.includes('..')) return false;

  // 가상 루트 외부 접근 방지
  if (!normalized.startsWith('/')) return false;

  return true;
}
```

#### Input Sanitization
```typescript
function sanitizeInput(input: string): string {
  // 특수 문자 제거/이스케이프
  return input
    .replace(/[;&|`$()]/g, '') // 위험한 쉘 메타문자 제거
    .trim();
}
```

### 4. Next.js 통합

#### app/developer/terminal/page.tsx
```typescript
import dynamic from 'next/dynamic';

// SSR 비활성화
const LinuxTerminal = dynamic(
  () => import('@/components/terminal/LinuxTerminal'),
  { ssr: false, loading: () => <div>Loading terminal...</div> }
);

export default function TerminalPage() {
  return (
    <main className="min-h-screen bg-black pt-20 pb-8 px-6">
      <div className="container mx-auto max-w-7xl">
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-white mb-3">Linux Terminal</h1>
          <p className="text-white/60">
            브라우저에서 리눅스 명령어를 실습하세요
          </p>
        </div>

        <div className="h-[calc(100vh-280px)] bg-gray-900 rounded-lg overflow-hidden border border-gray-700">
          <LinuxTerminal />
        </div>

        <div className="mt-8 p-4 bg-green-500/10 border border-green-500/20 rounded-lg">
          <p className="text-green-400 text-sm">
            💡 <strong>Tip:</strong> 'help' 명령어로 사용 가능한 명령어 목록을 확인하세요.
          </p>
        </div>
      </div>
    </main>
  );
}
```

### 5. 기본 디렉토리 구조 및 샘플 파일

```typescript
const initialFiles = {
  '/home/user/README.txt': `Welcome to BOOKER Linux Terminal!

This is a web-based Linux terminal simulator.
Try commands like: ls, cd, cat, pwd, mkdir, echo

Available commands:
- ls: List files and directories
- cd: Change directory
- cat: Display file contents
- pwd: Print working directory
- mkdir: Create directory
- rm: Remove files/directories
- echo: Print text
- clear: Clear screen
- help: Show available commands

Happy coding!`,

  '/home/user/projects/hello.js': `console.log('Hello from BOOKER!');`,

  '/home/user/projects/README.md': `# My Projects

This is a sample project directory.`,
};
```

## 구현 단계별 계획

### Week 1: 기본 터미널 UI (Day 1-2)
**Tasks:**
1. xterm.js 및 필요한 패키지 설치
   ```bash
   npm install @xterm/xterm @xterm/addon-fit @xterm/addon-web-links
   ```
2. LinuxTerminal 컴포넌트 생성
3. Next.js에서 SSR 비활성화 설정
4. 기본 스타일링 및 테마 적용
5. 프롬프트 표시 및 입력 처리

**예상 시간**: 8-10 시간

### Week 1: 파일시스템 통합 (Day 3-4)
**Tasks:**
1. @zenfs/core 설치
   ```bash
   npm install @zenfs/core
   ```
2. FileSystemManager 클래스 작성
3. InMemory 파일시스템 초기화
4. 기본 디렉토리 구조 생성
5. 샘플 파일 추가

**예상 시간**: 6-8 시간

### Week 2: 기본 명령어 구현 (Day 5-8)
**Tasks:**
1. CommandParser 클래스 작성
2. 각 명령어 핸들러 구현:
   - Day 5: ls, cd, pwd
   - Day 6: cat, mkdir, touch
   - Day 7: rm, cp, mv
   - Day 8: echo, clear, help, whoami
3. 보안 검증 (allowlist, path validation)
4. 에러 처리

**예상 시간**: 16-20 시간

### Week 3: 향상된 기능 (Day 9-12)
**Tasks:**
1. 명령어 히스토리 구현 (↑↓ 키)
2. Tab 자동완성 추가
3. 컬러 출력 지원
4. 명령어 옵션 파싱 개선 (-l, -a 등)

**예상 시간**: 12-16 시간

### Week 4: 테스트 및 통합 (Day 13-15)
**Tasks:**
1. 개발자센터 페이지 통합
2. 네비게이션 업데이트 (개발자센터 → Sandbox, Terminal)
3. 크로스 브라우저 테스트
4. 성능 최적화
5. 문서화

**예상 시간**: 10-12 시간

**총 예상 시간**: 52-66 시간 (약 2-3주)

## 보안 체크리스트

- [ ] Command allowlisting 구현
- [ ] Path validation 구현
- [ ] Input sanitization 구현
- [ ] XSS 방지 (eval() 사용 금지)
- [ ] 가상 파일시스템만 사용 (실제 파일 시스템 접근 차단)
- [ ] CSP 헤더 설정
- [ ] 명령어 실행 시간 제한 (무한 루프 방지)

## 성능 고려사항

1. **메모리 관리**
   - Terminal dispose() 호출 (컴포넌트 언마운트 시)
   - 파일시스템 메모리 제한 설정

2. **렌더링 최적화**
   - React.memo 사용
   - useMemo, useCallback 활용
   - 대용량 출력 시 페이지네이션

3. **번들 크기**
   - Dynamic import로 코드 스플리팅
   - Tree shaking 확인

## 테스트 계획

### 단위 테스트
- [ ] CommandParser 테스트
- [ ] 각 명령어 핸들러 테스트
- [ ] Path validation 테스트
- [ ] FileSystemManager 테스트

### 통합 테스트
- [ ] 명령어 체이닝 테스트
- [ ] 파일 생성/삭제 플로우 테스트
- [ ] 디렉토리 탐색 테스트

### 브라우저 테스트
- [ ] Chrome (최신)
- [ ] Firefox (최신)
- [ ] Safari (최신)
- [ ] Edge (최신)

### 보안 테스트
- [ ] Command injection 시도
- [ ] Path traversal 시도 (../)
- [ ] XSS 시도
- [ ] 특수 문자 입력 테스트

## 향후 확장 계획

### Phase 2 Features (추후 구현)
1. **파일 편집기**
   - vi/nano 스타일 텍스트 에디터
   - 파일 저장 및 편집

2. **네트워크 명령어**
   - curl (mock HTTP 요청)
   - ping (mock)

3. **프로세스 관리**
   - ps (mock)
   - kill (mock)

4. **패키지 관리**
   - apt-get (mock, 샘플 패키지)

5. **파일 지속성**
   - IndexedDB로 세션 간 파일 유지
   - Export/Import 기능

### 고급 기능 (장기)
- WebContainers 통합 (실제 Node.js 실행)
- 멀티 사용자 협업 기능
- 터미널 공유 기능
- 튜토리얼 모드 (단계별 가이드)

## 리스크 및 완화 전략

| 리스크 | 영향 | 확률 | 완화 전략 |
|--------|------|------|-----------|
| xterm.js SSR 이슈 | 높음 | 중간 | Dynamic import + "use client" 사용 |
| 브라우저 호환성 | 중간 | 낮음 | 주요 브라우저 테스트, polyfill 추가 |
| 메모리 누수 | 중간 | 중간 | 적절한 cleanup, dispose() 호출 |
| 보안 취약점 | 높음 | 낮음 | Allowlist, 입력 검증, 보안 테스트 |
| 성능 문제 | 낮음 | 낮음 | 최적화, 페이지네이션 |

## 성공 지표

1. **기능적 지표**
   - 모든 기본 명령어 정상 작동
   - 파일시스템 CRUD 작업 성공
   - 에러 없이 100개 명령어 연속 실행

2. **성능 지표**
   - 터미널 로딩 시간 < 2초
   - 명령어 응답 시간 < 100ms
   - 메모리 사용량 < 50MB

3. **사용자 경험**
   - 모바일 반응형 지원
   - 직관적인 UI/UX
   - 명확한 에러 메시지

## 질문 사항 (승인 전 확인 필요)

1. **MVP 범위**
   - Phase 1-3 명령어들로 충분한가요?
   - 추가하고 싶은 명령어가 있나요?

2. **UI/UX**
   - 개발자센터 내 별도 탭으로 추가할까요? (Sandbox, Terminal)
   - 풀스크린 모드가 필요한가요?

3. **파일 지속성**
   - 세션 간 파일 유지가 필요한가요? (InMemory vs IndexedDB)
   - 사용자별 격리가 필요한가요?

4. **모바일 지원**
   - 모바일에서도 동작해야 하나요?
   - 아니면 데스크톱만 지원하나요?

5. **보안/규정**
   - 특별한 보안 요구사항이 있나요?
   - 사용자 입력 로깅이 필요한가요?

## 다음 단계

승인 후:
1. ✅ 패키지 설치
2. ✅ LinuxTerminal 컴포넌트 생성
3. ✅ FileSystemManager 구현
4. ✅ 기본 명령어 구현
5. ✅ 개발자센터 통합
6. ✅ 테스트 및 배포

---

**작성일**: 2025-12-14
**작성자**: Claude Code
**검토 필요**: 사용자 승인 대기
