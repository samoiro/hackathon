# 사장님의 1분 경영회의

사모이로 해커톤 프론트엔드. React + TypeScript 기반 CSR SPA입니다.

## 실행 환경

- Node.js **22.22.0 이상** (React Router v8 최소 요구사항)
- npm
- 프론트엔드 작업 경로: `hackathon/frontend/`

```sh
npm ci
npm run dev
```

`npm run typecheck`로 타입 검사, `npm run lint`로 ESLint 검사,
`npm run build`로 타입 검사와 프로덕션 빌드를 수행합니다.
`npm run preview`로 빌드 결과를 로컬에서 확인합니다.

## 구성

- `src/app/providers/`: TanStack Query, Ant Design, BrowserRouter 공급자와 테마
- `src/app/router/`: `/`, `*`(404) 경로
- `src/lib/query/queryClient.ts`: 공통 QueryClient (라이브러리 기본 정책)
- `src/lib/api/client.ts`: 공통 Axios 인스턴스
- `src/pages/`: 최소 시작 화면과 404 화면
- `src/styles/index.css`: Tailwind v4 진입점과 전역 스타일

Tailwind Vite 플러그인을 사용합니다. Ant Design의 StyleProvider에서 CSS layer를
활성화하고 `theme, base, antd, components, utilities` 순서로 선언해
Tailwind Preflight와 Ant Design의 스타일 우선순위를 명시합니다.

## 환경변수

`.env.example`을 참고해 로컬 `.env.local`을 만드세요.

- `VITE_API_BASE_URL`: 백엔드 기본 URL
- 예제의 `http://localhost:8080`은 자리표시자이며 확정된 서버 주소가 아닙니다.
- 실제 요청, API 경로, 인증 및 인터셉터는 아직 구현하지 않았습니다.
- `VITE_*` 값은 브라우저에 공개됩니다. 비밀 키를 넣지 마세요.

## Vercel 배포 준비

- Root Directory: `frontend`
- Framework Preset: Vite
- Build Command: `npm run build`
- Output Directory: `dist`
- Node.js 버전은 `package.json`의 최소 요구사항 이상으로 맞추세요.
- 배포 환경의 `VITE_API_BASE_URL`은 빌드 전에 설정해야 합니다.
- `vercel.json`은 SPA 하위 경로를 `index.html`로 연결합니다.
  Vercel은 실제로 존재하는 정적 파일을 우선하며 `/assets/`는 rewrite에서 제외합니다.

Vercel 연결 및 실제 배포는 수행하지 않습니다. `server/`는 별도 프로젝트입니다.
