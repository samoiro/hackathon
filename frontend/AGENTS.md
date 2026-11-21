# Frontend (사모이로 · 사장님의 1분 경영회의)

루트 `AGENTS.md` 규칙 위에 이 파일이 더해집니다. 충돌하면 이 파일이 우선합니다.

## Stack

- Vite 8 + React 19 + TypeScript (strict), SPA
- 라우팅: `react-router` v8 — `src/app/router/AppRouter.tsx`
- 서버 상태: TanStack Query v5 — `src/lib/query/queryClient.ts`
- HTTP: axios 인스턴스 `src/lib/api/client.ts` (`VITE_API_BASE_URL`)
- API 타입·함수: hey-api 생성 코드 `src/lib/api/generated` (`npm run api`), 화면용 훅은 `src/lib/api/{도메인}.ts`
- UI: antd v6 (컴포넌트·테마 `src/app/providers/theme.ts`) + Tailwind v4 (레이아웃·간격)
- 포맷: Prettier (`semi: false`, `singleQuote: true`), ESLint `--max-warnings 0`

## 구조

```
src/
├── app/        providers, router — 앱 조립
├── pages/      라우트 단위 화면
├── lib/        api, query 등 인프라
└── styles/     전역 CSS (CSS layer 순서: theme, base, antd, components, utilities)
```

## 규칙

- **import**: 내부 모듈은 항상 `@/` 별칭(`@/` = `src/`)으로 가져온다. `./`, `../` 상대경로를 쓰지 않는다. (생성 코드 `generated` 제외)
- **export**: 컴포넌트는 `export default function 이름()`으로 내보낸다. 컴포넌트가 아닌 모듈(`apiClient`, `queryClient`, `appTheme` 등)은 named export를 유지한다.
- **API**: `src/lib/api/generated`는 생성 코드라 직접 수정하지 않는다. 서버 스펙이 바뀌면 `npm run api`로 다시 만든다. axios를 화면에서 직접 호출하지 않고 생성된 SDK 함수를 `src/lib/api/{도메인}.ts`의 useQuery·useMutation 훅으로 감싸 쓴다.
- **서버 데이터**: 조회는 `useQuery`, 변경은 `useMutation`. 변경 성공 시 관련 `queryKey`를 `invalidateQueries`로 무효화한다. 서버 데이터를 `useState`에 복사해 두지 않는다.
- **UI**: antd에 있는 컴포넌트는 직접 만들지 않는다. 색·radius·폰트는 `theme.ts` 토큰을 바꾼다. Tailwind는 레이아웃·간격에만 쓰고 antd 컴포넌트 내부 스타일을 덮어쓰지 않는다.
- **환경변수**: `VITE_` 접두사만 클라이언트에 노출된다. 비밀 값은 넣지 않는다.
- **검증**: 작업을 끝내기 전 `npm run typecheck && npm run lint`가 통과해야 한다.

## 스킬

`.claude/skills/`에 디자인·React 스킬이 있다. UI 품질 점검은 `impeccable`·`unslop`·`better-*`, React 패턴은 `vercel-react-best-practices`·`vercel-composition-patterns`를 쓴다.

## 문서

화면·컴포넌트 작업 전에 `docs/`를 읽는다.

- `docs/design.md`: 색·글자·모양·움직임·문구 기준 (브랜드가이드 PDF, 디자인 리뷰 정리)
- `docs/screen.md`: 화면 뼈대, 너비·그리드, 여백, 카드 구성, 상태 화면
- `docs/components.md`: 컴포넌트 작성 규칙, 접근성, 지금 있는 컴포넌트 목록
