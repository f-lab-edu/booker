# Enhance Dreamy UI with Shader Effects

## 목표
BOOKER 랜딩 페이지의 배경을 커서를 따라 밝아지는 몽환적인 shader 효과로 업그레이드

## 현재 상황 분석

### 기존 구조
- `HeroBanner.tsx`: framer-motion을 사용한 3개의 blur된 orb 애니메이션
- `ShaderCanvas.tsx`: WebGL canvas 컴포넌트 (현재 미사용)
- `shaderPrograms.ts`: spectral wave shader (무지개색 파동 패턴)

### 문제점
- 현재 orb 효과는 CSS blur만 사용하여 제한적
- 커서 반응이 있지만 "밝아지는" 느낌이 부족
- 참고 디자인의 부드러운 blob 효과와 차이가 있음

## 구현 계획

### Phase 1: 새로운 Dreamy Blob Shader 작성
**파일:** `shaderPrograms.ts`

새로운 shader 추가: `dreamyBlobShader`

**특징:**
1. **Metaball/Blob 효과**
   - 여러 개의 부드러운 blob이 천천히 움직임
   - Smooth distance field를 사용한 유기적인 형태

2. **커서 반응형 밝기**
   - 커서 주변에서 색상이 밝아지는 glow 효과
   - Smooth falloff로 자연스러운 전환
   - 커서 위치에 따라 bloom 효과 추가

3. **몽환적 색상 팔레트**
   - 베이스: 딥 퍼플/네이비 (#1a1a2e, #16213e)
   - 중간: 라벤더/퍼플 (#8b7dd1, #a991d4)
   - 하이라이트: 밝은 핑크/화이트 (#ff6ec7, #f7e6ff)
   - 커서 근처: 밝은 화이트/골드 glow

4. **유려한 애니메이션**
   - 느린 시간 기반 움직임
   - 여러 frequency의 noise 조합
   - Smooth step functions으로 부드러운 전환

**기술적 접근:**
```glsl
// Metaball distance function
// Multiple blob centers with smooth min
// Cursor influence on brightness
// Layered gradients (base -> glow -> highlight)
```

### Phase 2: HeroBanner에 Shader 통합
**파일:** `HeroBanner.tsx`

**변경사항:**
1. ShaderCanvas를 배경으로 추가
2. 기존 orb div들은 제거 또는 감소
3. 콘텐츠는 shader 위에 z-index로 레이어링
4. 선택적: 약간의 glass morphism 효과 추가

**구조:**
```tsx
<section>
  {/* WebGL Shader Background */}
  <ShaderCanvas />

  {/* Content Layer */}
  <div className="relative z-10">
    {/* 기존 콘텐츠 */}
  </div>
</section>
```

### Phase 3: 성능 최적화
1. **저사양 기기 대응**
   - WebGL 지원 확인
   - Fallback으로 현재 CSS 버전 유지

2. **렌더링 최적화**
   - Lower resolution rendering + upscaling
   - RequestAnimationFrame 최적화

### Phase 4: 인터랙션 향상 (선택사항)
1. 커서 움직임에 따른 trail 효과
2. 클릭 시 ripple 효과
3. 스크롤에 따른 intensity 조절

## Shader 구현 상세

### Fragment Shader 핵심 로직

```glsl
// 1. Multiple blob centers
vec2 blob1 = vec2(sin(iTime * 0.3), cos(iTime * 0.2)) * 0.5;
vec2 blob2 = vec2(cos(iTime * 0.4), sin(iTime * 0.3)) * 0.6;
vec2 blob3 = vec2(sin(iTime * 0.2 + 3.0), cos(iTime * 0.25)) * 0.4;

// 2. Distance fields
float d1 = length(p - blob1);
float d2 = length(p - blob2);
float d3 = length(p - blob3);

// 3. Smooth minimum (metaball blend)
float k = 0.5; // blend factor
float smin = smoothmin(smoothmin(d1, d2, k), d3, k);

// 4. Cursor glow
vec2 mouseP = (iMouse / iResolution) * 2.0 - 1.0;
float mouseDist = length(p - mouseP);
float glow = exp(-mouseDist * 2.0) * 0.8;

// 5. Color mixing
vec3 baseColor = mix(deepPurple, lavender, somePattern);
vec3 glowColor = mix(brightPink, white, glow);
vec3 finalColor = mix(baseColor, glowColor, glow);

// 6. Brightness boost near cursor
finalColor += glow * brightWhite * 0.5;
```

### 색상 설정
```glsl
const vec3 deepPurple = vec3(0.1, 0.1, 0.18);   // #1a1a2e
const vec3 navy = vec3(0.09, 0.13, 0.24);       // #16213e
const vec3 lavender = vec3(0.54, 0.49, 0.82);   // #8b7dd1
const vec3 lightPurple = vec3(0.66, 0.57, 0.83);// #a991d4
const vec3 brightPink = vec3(1.0, 0.43, 0.78);  // #ff6ec7
const vec3 lightPink = vec3(0.97, 0.9, 1.0);    // #f7e6ff
const vec3 brightWhite = vec3(1.0, 1.0, 1.0);
```

## 성공 기준
1. ✅ 커서를 따라 자연스럽게 밝아지는 효과
2. ✅ 부드럽고 유기적인 blob 형태
3. ✅ 몽환적인 purple/pink 색상 팔레트
4. ✅ 60fps 부드러운 애니메이션
5. ✅ 저사양 기기에서도 작동 (fallback)

## 참고 자료
- Metaball 알고리즘: Smooth minimum functions
- Shader glow 효과: Exponential falloff
- 참고 디자인: Beautiful Shader Experiences (제공된 스크린샷)

## 구현 순서
1. [✅] 새로운 `dreamyBlobShader` 작성 및 테스트
2. [✅] ShaderCanvas가 새 shader 사용하도록 수정
3. [✅] HeroBanner에 ShaderCanvas 통합
4. [✅] 색상 및 애니메이션 파라미터 조정
5. [ ] 성능 테스트 및 최적화 (필요시)
6. [ ] Fallback 구현 (필요시)

## 예상 작업 시간
- Shader 작성: 30-45분
- 통합 및 스타일링: 15-20분
- 미세 조정: 20-30분
- **총 예상: 1-1.5시간**

## 추가 고려사항
- 모바일에서는 터치 위치를 커서로 사용
- 애니메이션 속도를 사용자가 조절할 수 있는 옵션 (선택)
- 색상 테마를 여러 개 만들어 전환 가능하도록 (선택)

---

## 구현 완료 내역 (2025-12-13)

### 1. Dreamy Blob Shader 작성
**파일:** `shaderPrograms.ts`

- Metaball 기반 blob 효과 구현
- 4개의 blob이 유기적으로 움직임 (범위: 0.85-1.0, 넓게 확장)
- 커서 glow 효과:
  - Exponential falloff: `exp(-mouseDist * 1.2) * 0.5`
  - Influence range: `smoothstep(2.5, 0.0, mouseDist)` - 넓은 범위
- 색상 팔레트: Deep purple → Lavender → Bright pink/white
- 전체 밝기: 0.75배로 감소

### 2. ShaderCanvas 개선
**파일:** `ShaderCanvas.tsx`

- Framer Motion spring 추가:
  - `damping: 25, stiffness: 150`
  - 기존 HeroBanner와 동일한 부드러운 커서 추적
- `dreamyBlobShader` 사용

### 3. HeroBanner 통합
**파일:** `HeroBanner.tsx`

- 기존 CSS orb 제거
- ShaderCanvas를 배경으로 배치
- 콘텐츠는 z-index로 레이어링

### 최종 파라미터 값
```glsl
// Blob 움직임 범위
blob1-4: 0.85 ~ 1.0 (넓은 범위)

// 커서 glow
cursorGlow: exp(-mouseDist * 1.2) * 0.5
cursorInfluence: smoothstep(2.5, 0.0, mouseDist)

// 밝기
color *= 0.75

// Spring animation
{ damping: 25, stiffness: 150 }
```

### 성공 기준 달성
- ✅ 커서를 따라 자연스럽게 밝아지는 효과 (넓은 범위)
- ✅ 부드럽고 유기적인 blob 형태 (파도처럼 넓게 움직임)
- ✅ 몽환적인 purple/pink 색상 팔레트
- ✅ 부드러운 spring 애니메이션
- ✅ 전체적으로 어두운 톤 유지
