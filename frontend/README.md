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

`npm run dev`는 API 코드 생성 → Prettier 정리(`format`) → ESLint → 타입 검사 → 빌드를
순서대로 통과해야 개발 서버를 띄웁니다. 하나라도 실패하면 멈춥니다.

`npm run typecheck`로 타입 검사, `npm run lint`로 ESLint 검사, `npm run format`으로 Prettier 정리,
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

## API 코드 생성 (hey-api)

서버 OpenAPI 문서(`{VITE_API_BASE_URL}/v3/api-docs`)로 DTO 타입과 API 함수를
`src/lib/api/generated/`에 자동 생성합니다. (git에는 올리지 않음)

- `npm run dev`, `npm run build` 시작 시 자동 생성 → **서버가 켜져 있어야 합니다.**
- 서버 API가 바뀌면 `npm run api`로 다시 생성
- 생성된 함수는 `src/lib/api/client.ts`의 `apiClient`(axios)로 요청합니다. (`heyApiConfig.ts`에서 연결)

```ts
import { culturalEvents } from '../lib/api/generated'

const { data } = await culturalEvents({ query: { category: '콘서트', size: 20 } })
// data: { totalCount: number; items: unknown[] }
```

## 배포 (Cloudflare Pages, 무료)

서버(Render)를 먼저 배포한 뒤 진행합니다. 빌드 중 `VITE_API_BASE_URL`의 `/v3/api-docs`로
API 코드를 생성하므로 **배포된 서버가 켜져 있어야 빌드가 성공합니다.**

Cloudflare 대시보드 → Workers & Pages → Create → **Pages** → Connect to Git → 이 레포

- Production branch: `main`
- Framework preset: None (Vite)
- Build command: `npm run build`
- Build output directory: `dist`
- Root directory: `frontend`
- Environment variables: `VITE_API_BASE_URL=https://<서비스>.onrender.com`

Node.js 버전은 `.nvmrc`로 지정됩니다. `dist`에 `404.html`이 없으면 Cloudflare Pages가
SPA로 보고 하위 경로를 `index.html`로 연결합니다.
