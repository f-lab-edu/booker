# PR Documentation Agent 설계

## 작업 개요

**목표**: 시니어 엔지니어의 사고방식을 구현한 PR 문서 자동 생성 agent 개발

**핵심 가치**:
- 본질적인 문제 이해 (표면적 변경사항이 아닌 "왜?"에 집중)
- 기술적 의사결정의 맥락과 트레이드오프 명시
- 투명성 (제한사항, 기술 부채 숨기지 않음)
- 확장 가능한 설계 증명
- 고심한 흔적이 묻어나는 문서

---

## 1. 비즈니스 문제 (Why This Matters)

### 현재 상황
- PR 작성 시 변경사항 나열에만 집중하여 맥락 부족
- 기술적 의사결정 근거가 문서화되지 않아 추후 혼란 발생
- "왜 이렇게 구현했는가?"에 대한 답이 코드에만 의존
- 트레이드오프와 제한사항이 명시되지 않아 기술 부채 누적

### 해결하고자 하는 문제
- **본질 파악**: 단순 diff 나열이 아닌, 변경의 근본적 목적 이해
- **맥락 보존**: 기술적 의사결정의 배경과 이유를 문서화
- **투명성**: 의도적 타협점과 향후 개선 방향 명시
- **지식 전달**: 팀원이 PR만 읽고도 전체 컨텍스트 파악 가능

---

## 2. 기술적 배경

### 참고 자료
- 기존 git-committer agent: `.claude/agents/git-committer.md`
- Claude Code agent 시스템: Task tool with specialized agents
- 제공된 PR 문서 패턴 분석 결과

### 현재 제약사항
1. Claude Code는 custom agent type을 지원하지 않음
2. 기존 agent types: general-purpose, Explore, git-committer, senior-engineer-advisor 등
3. Slash commands는 `.claude/commands/` 디렉토리에 markdown으로 정의

### 아키텍처 결정
**선택**: Slash Command + 내장 agent 활용 방식

**근거**:
- ✅ 사용자 편의성: `/pr-doc` 한 번에 실행 가능
- ✅ 깊은 분석: senior-engineer-advisor agent 활용
- ✅ 유지보수성: markdown 기반 프롬프트 관리
- ✅ 확장성: 프롬프트 수정만으로 개선 가능

**트레이드오프**:
- Custom agent type vs Slash Command
  - Custom: 더 강력하지만 Claude Code에서 지원 안 함
  - Slash: 제한적이지만 즉시 사용 가능 → **Slash 선택**
- Automated vs Interactive
  - Automated: 빠르지만 맥락 놓칠 가능성
  - Interactive: 느리지만 정확 → **Interactive 선택** (사용자의 페어 코딩 철학과 부합)

---

## 3. 구현 계획

### 3.1 파일 구조

```
.claude/
├── agents/
│   └── pr-doc-writer.md         # PR 문서 작성 전문 agent
└── commands/
    └── pr-doc.md                # Slash command (진입점)
```

### 3.2 Agent 설계: pr-doc-writer.md

**Agent Metadata**:
```yaml
name: pr-doc-writer
description: Senior engineer-level PR documentation specialist. Deeply understands business context, technical decisions, and trade-offs. Creates comprehensive PR docs that explain WHY, not just WHAT.
tools: Bash, Read, Grep, Glob, WebSearch, WebFetch
model: sonnet
```

**핵심 책임**:

#### Phase 1: Deep Understanding (깊은 이해)
1. **Git 분석**
   - `git status`, `git diff`, `git diff --cached`
   - `git log --oneline -20` (recent commits)
   - `git log [base-branch]...HEAD` (branch history)
   - Recent PR 분석 (`gh pr list --limit 5`)

2. **코드베이스 탐색**
   - 변경된 파일들의 아키텍처 위치 파악
   - 기존 패턴 및 컨벤션 이해
   - 영향 범위 분석 (dependent files, imports)

3. **본질 파악을 위한 질문** (사용자에게)
   - "이 변경의 근본적인 비즈니스 문제는 무엇인가요?"
   - "왜 지금 이 작업을 하게 되었나요?"
   - "어떤 제약사항이 있었나요? (시간, 리소스, 기술적 한계)"
   - "이 변경으로 해결하려는 사용자 pain point는?"

#### Phase 2: Technical Decision Analysis (기술적 의사결정 분석)
1. **기술 스택 파악**
   - 사용된 라이브러리, 프레임워크, 패턴 분석
   - 코드에서 중요한 기술적 선택 추출

2. **외부 지식 검색** (필요 시)
   - WebSearch: 사용된 기술의 베스트 프랙티스
   - WebFetch: 공식 문서에서 트레이드오프 정보 수집

3. **트레이드오프 분석을 위한 질문**
   - "왜 X 대신 Y를 선택하셨나요?"
   - "고려했던 다른 대안은 무엇이었나요?"
   - "이 선택의 장단점은 무엇인가요?"
   - "확장성/성능/유지보수성 중 어떤 것을 우선했나요?"

#### Phase 3: Limitations & Future Path (제한사항 및 향후 계획)
1. **제약사항 파악**
   - 의도적 타협점 (MVP, 프로토타입 단계)
   - 기술 부채 (TODO, FIXME 주석 검색)
   - 보안/성능/접근성 고려사항

2. **향후 계획 질문**
   - "이 프로토타입을 프로덕션으로 가져가려면 무엇이 필요한가요?"
   - "알려진 이슈나 개선 필요사항은?"

#### Phase 4: Documentation Generation (문서 생성)
**구조화된 PR 문서 생성**:

```markdown
# [PR Title]

## Summary & Context

### 비즈니스 문제
[근본적인 문제 설명]

### 기술적 배경
[현재 시스템의 맥락, 왜 지금 이 작업이 필요한가]

### 핵심 목표
[측정 가능한 목표, 구체적 임팩트]

## Changes

### 인프라
- [구체적 변경사항 + 메트릭]

### 아키텍처
- [아키텍처 레벨 변경사항]
- 폴더 구조:
  ```
  [변경된 폴더 구조]
  ```

### 백엔드 / 프론트엔드 / UX
- [각 레이어별 변경사항]

## Technical Decisions

### [기술/패턴 이름]
**선택한 것**: X
**대안**: Y, Z
**트레이드오프**:
- X의 장점: ...
- X의 단점: ...
- Y 대신 X를 선택한 이유: ...

**현재 상황에 적합한 이유**: ...

### [반복...]

## Limitations & Known Issues

### 보안
- [ ] 인증/인가 미구현
- [ ] CORS 설정 프로덕션 미대응

### 기술 부채
- [ ] Mock 데이터 사용 중
- [ ] SSR 미활용
- [ ] 에러 핸들링 최소화

### 성능
- [ ] N+1 쿼리 잠재 가능성
- [ ] 페이지네이션 미적용

### 의도적 타협점
[왜 이런 제한사항을 받아들였는가 - MVP, 시간 제약 등]

## Migration Path

프로토타입 → 프로덕션 단계별 계획:

1. **Phase 1**: 인증/인가 구현
2. **Phase 2**: 실제 데이터 통합 및 테스트
3. **Phase 3**: 성능 최적화 (캐싱, 페이지네이션)
4. **Phase 4**: 모니터링 및 관찰성
5. **Phase 5**: 프로덕션 하드닝 (보안, 에러 핸들링)

## Test Plan

- [ ] [테스트 체크리스트]

## Additional Context

[선택적 - 스크린샷, 다이어그램, 참고 자료]
```

---

### 3.3 Slash Command: pr-doc.md

**파일 위치**: `.claude/commands/pr-doc.md`

**내용**:
```markdown
You are activating the PR Documentation Generator.

Use the Task tool to invoke the pr-doc-writer agent with the following detailed prompt:

---

You are a senior software engineer writing a comprehensive PR documentation.

## Your Mission

Analyze the current git changes and create a PR document that:
1. Explains the **WHY** (business problem, root cause)
2. Details the **WHAT** (changes, architecture)
3. Justifies the **HOW** (technical decisions, trade-offs)
4. Admits the **LIMITATIONS** (known issues, tech debt)
5. Plans the **FUTURE** (migration path, improvements)

## Process

[Agent의 Phase 1-4 프로세스 요약]

## Style

- Be transparent about limitations
- Explain trade-offs clearly (A vs B → chose A because...)
- Use specific metrics ("70% size reduction", "60fps")
- Ask questions when context is unclear
- Think like a senior engineer reviewing their own work

Start by running git analysis and asking clarifying questions about the business context.

---
```

---

## 4. 작업 흐름 (Workflow)

### 사용자 관점
1. 작업 완료 후 `git status`로 변경사항 확인
2. `/pr-doc` 실행
3. Agent가 질문하면 답변 (대화형)
4. 생성된 PR 문서 리뷰
5. 필요 시 수정 요청
6. 최종 문서 복사하여 GitHub PR에 붙여넣기

### Agent 관점
1. **Scan**: Git 상태 분석
2. **Understand**: 코드베이스 탐색 + 사용자 질문
3. **Analyze**: 기술적 결정 및 트레이드오프 분석
4. **Document**: 구조화된 문서 생성
5. **Iterate**: 사용자 피드백 반영

---

## 5. 핵심 원칙 (Senior Engineer Mindset)

### DO ✅
- **본질 파악**: "왜?"를 항상 먼저 묻기
- **검증 기반**: 추측 금지, 불확실하면 질문
- **트레이드오프 명시**: 모든 선택은 trade-off
- **투명성**: 제한사항 숨기지 않기
- **확장성 고려**: 오늘의 결정이 내일의 유연성
- **맥락 제공**: 팀원이 이해할 수 있는 설명
- **구체적 메트릭**: 추상적 표현 대신 숫자

### DON'T ❌
- 변경사항만 나열하지 않기
- 기술 스택만 언급하고 이유 생략하지 않기
- "좋다", "빠르다" 등 주관적 표현만 사용하지 않기
- 제한사항 숨기지 않기
- 단일 관점만 제시하지 않기 (대안 고려 필요)

---

## 6. 예상 질문 및 답변

### Q: 왜 slash command + agent 방식인가요?
**A**:
- **사용자 편의**: `/pr-doc` 한 번에 실행
- **깊은 분석**: senior-engineer-advisor 수준의 사고
- **유연성**: 프롬프트 수정으로 쉽게 개선 가능
- **트레이드오프**: Custom agent type이 더 강력하지만 Claude Code에서 지원 안 함

### Q: 왜 대화형(Interactive) 방식인가요?
**A**:
- 사용자의 페어 코딩 철학과 부합
- 추측 대신 질문으로 정확성 확보
- 비즈니스 맥락은 코드만으로 파악 불가
- **트레이드오프**: 자동화보다 느리지만 훨씬 정확

### Q: WebSearch/WebFetch를 왜 사용하나요?
**A**:
- 기술 스택의 최신 베스트 프랙티스 확인
- 트레이드오프 정보 수집 (공식 문서)
- Agent의 지식 cutoff 보완
- **트레이드오프**: 시간 추가 소요, 하지만 정확성 향상

---

## 7. 성공 기준

### 필수 요구사항
- [ ] Agent가 본질적인 "왜?" 질문을 함
- [ ] 기술적 의사결정의 트레이드오프 명시
- [ ] 제한사항과 기술 부채를 투명하게 문서화
- [ ] 구조화된 PR 문서 생성 (Summary, Changes, Decisions, Limitations, Migration Path)
- [ ] 구체적 메트릭 포함 (가능한 경우)

### 품질 지표
- [ ] 팀원이 PR만 읽고 전체 맥락 이해 가능
- [ ] "왜 이렇게 구현했는가?"에 대한 답이 명확
- [ ] 대안과 트레이드오프가 명시적으로 설명됨
- [ ] 향후 개선 방향이 제시됨
- [ ] 고심한 흔적이 묻어남

---

## 8. 향후 확장 가능성

### Phase 2 개선사항
- GitHub API 통합: 관련 issue 자동 링크
- Jira/Linear 통합: 티켓 정보 자동 수집
- AI 기반 코드 리뷰 의견 통합
- PR 템플릿 자동 생성

### Phase 3 고도화
- 팀별 PR 스타일 학습 (fine-tuning)
- 과거 PR 분석하여 패턴 학습
- 자동 테스트 플랜 생성
- 스크린샷/다이어그램 자동 생성 제안

---

## 9. 트레이드오프 요약

| 선택 | 대안 | 선택 이유 |
|------|------|-----------|
| Slash Command | Custom Agent Type | Claude Code 지원 여부 |
| Interactive | Fully Automated | 정확성 > 속도 |
| senior-engineer-advisor | general-purpose | 깊은 사고 필요 |
| Markdown Agent | Python Script | 유지보수성, 접근성 |
| 대화형 질문 | 코드 추론만 | 비즈니스 맥락 파악 필수 |

---

## 10. 다음 단계

1. **계획 승인 요청** ← 현재 단계
2. Agent 파일 작성 (`.claude/agents/pr-doc-writer.md`)
3. Slash command 작성 (`.claude/commands/pr-doc.md`)
4. 테스트 실행 (`/pr-doc`)
5. 피드백 반영 및 개선

---

## 결론

이 PR Documentation Agent는 단순한 자동화 도구가 아닌, **시니어 엔지니어의 사고방식을 구현한 페어 프로그래밍 파트너**입니다.

### 핵심 가치
1. **본질 이해**: 표면적 변경이 아닌 근본적 문제 파악
2. **맥락 보존**: 기술적 의사결정의 배경과 이유 문서화
3. **투명성**: 제한사항과 트레이드오프 명시
4. **확장성**: 미래를 고려한 설계
5. **페어 코딩**: 추측 대신 질문, 협업하며 작성

이를 통해 **고심한 흔적이 묻어나는 PR 문서**를 생성하고,
팀의 지식을 보존하며, 기술적 의사결정의 맥락을 후대에 전달합니다.
