# 컴포넌트 규칙

지금 있는 컴포넌트와 앞으로 만들 컴포넌트가 모두 따르는 규칙입니다. 값은 `design.md`, 화면 배치는 `screen.md`를 봅니다. 저장소 공통 규칙은 `AGENTS.md`가 우선합니다.

## 1. 만들기 전에

아래 순서에서 처음 맞는 것을 씁니다.

1. **이미 있는 컴포넌트**: 아래 목록에서 먼저 찾습니다.
2. **antd 컴포넌트**: Button, Card, Modal, Select, Checkbox, Radio, Tag, Popconfirm, Popover, Alert, Progress 등이 있으면 직접 만들지 않습니다.
3. **네이티브 요소**: `<button>`, `<a href>`, `<input type="radio">` 등을 씁니다. `div onClick`은 쓰지 않습니다.
4. **설치된 라이브러리**: react-icons, recharts, TanStack Query, react-router를 씁니다. 몇 줄이면 되는 일에 새 의존성을 넣지 않습니다.
5. **위로 안 되면**: 새 컴포넌트를 만듭니다.

## 2. 파일과 코드 형식

- **위치**: `src/components/{영역}/{이름}.tsx`입니다. 폴더에 `index.ts`를 만들지 않습니다.

  | 영역                               | 쓰는 곳                  |
  | ---------------------------------- | ------------------------ |
  | `common`                           | 두 화면 이상에서 쓰는 것 |
  | `layout`                           | 뼈대                     |
  | `dashboard`, `onboarding`, `items` | 화면이나 기능에 묶인 것  |

- **내보내기**: `export default function 이름()`입니다. 컴포넌트가 아닌 모듈(훅, 컨텍스트, 상수)은 named export입니다.
- **import**: 항상 `@/` 별칭을 씁니다. 상대 경로를 쓰지 않습니다.
- **타입**: props 타입은 함수 인자에 바로 적습니다. 여러 파일이 쓰는 타입은 `src/types/{도메인}.ts`에 둡니다. `any`와 `as`는 쓰지 않습니다.
- **핸들러**: 함수 하나만 부르는 핸들러는 감싸지 않고 바로 넘깁니다(`onChange={setDraft}`).
- **주석**: 코드로 안 보이는 이유만 한 줄로 적습니다. 알고 남긴 한계는 `// shortcut: 한계, 언제 바꿀지` 형식으로 적습니다.
- **목 데이터**: 가짜 데이터는 `src/mocks/`에 둡니다. API를 연결할 때 fetcher만 바꾸면 되게 합니다.

## 3. 스타일

- **역할 나누기**: antd는 컴포넌트와 테마(`theme.ts`)를, Tailwind는 레이아웃과 간격을 맡습니다.
- **antd 안쪽 모양**: 클래스로 덮지 않고 `theme.ts`의 component 토큰을 바꿉니다.
- **색**: 토큰 클래스만 씁니다(`bg-surface`, `text-muted`, `border-line-strong`). hex 값을 직접 쓰지 않습니다. JS에서 색이 필요하면 `var(--color-…)`을 씁니다.
- **글자**: 7단계 클래스(`text-display`부터 `text-tag`까지)만 씁니다. `text-[15px]` 같은 임의 크기는 쓰지 않습니다.
- **간격**: 4의 배수 Tailwind 단계(`gap-2`, `p-4`, `mt-6`)만 씁니다.
- **어두운 화면**: 토큰이 바꿔 주므로 `dark:` 분기를 쓰지 않습니다. antd 쪽 값이 다르면 `theme.ts`에서 `dark ? … : …`로 처리합니다.
- **글자 크기 설정**: 높이를 px로 고정하지 않습니다. rem 기반 클래스나 antd 토큰(`controlHeight`)을 씁니다.

## 4. 접근성 (모든 컴포넌트 필수)

- **이름**:
  - 아이콘만 있는 버튼에는 `aria-label`을 붙입니다. 꾸밈 아이콘과 그림에는 `aria-hidden`이나 `alt=""`를 씁니다.
  - 보이는 글자가 있으면 그 글자로 이름을 시작합니다. 예: `${todo.title} 지우기`
- **섹션**: 카드 내용은 `<section aria-labelledby="xxx-heading">`과 `<h2 id="xxx-heading">`으로 묶습니다.
- **선택**:
  - 하나만 고르는 선택은 네이티브 라디오(antd `Radio.Group`)를 씁니다. 원형 표시만 `[&_.ant-radio]:sr-only`로 숨기고, 화살표 키 이동은 그대로 둡니다.
  - 선택 표시는 색, 체크 아이콘, 굵은 글자 세 가지를 함께 씁니다.
- **켜고 끄기**: `<Button aria-pressed={on}>`을 씁니다.
- **알림**: 바뀌는 글자는 미리 그려 둔 `aria-live="polite"` 영역의 내용만 바꿉니다. `role="alert"`는 급한 오류에만 씁니다.
- **팝업**:
  - antd Modal과 Popover를 씁니다. Esc와 바깥 클릭으로 닫히게 합니다.
  - 닫힌 뒤에는 연 버튼으로 포커스를 돌려줍니다.
  - 지운 뒤처럼 포커스할 요소가 사라지면, 다음으로 자연스러운 요소로 포커스를 옮깁니다.
- **삭제**: 되돌릴 수 없는 동작은 `Popconfirm`으로 확인합니다. 제목은 "정말 삭제할까요?", 확인 버튼은 `danger` "예, 삭제해요"입니다. 되돌릴 수 있는 숨기기는 '되돌리기'를 제공합니다.
- **움직임**:
  - 자동으로 움직이는 것은 `usePrefersReducedMotion()`이 참이면 멈춥니다.
  - 5초 넘게 움직이면 멈춤 버튼을 둡니다.
  - 마우스를 올리거나 포커스가 들어오면 잠시 멈춥니다.
- **누르는 영역**: 최소 24×24입니다. 모바일은 44×44입니다.
- **입력 검증**: 제출 버튼을 미리 비활성화하지 않고, 제출할 때 검증합니다. 예외는 서울 밖 주소처럼 확인 자체가 불가능한 경우입니다.

## 5. 데이터와 상태

- **서버 데이터**: `useQuery`로 읽고 `useMutation`으로 바꿉니다. 바꾼 뒤에는 관련 `queryKey`를 `invalidateQueries`로 무효화합니다. 서버 데이터를 `useState`에 복사하지 않습니다.
- **검색 입력**: `SearchSelect`를 씁니다. `useDebouncedValue`(300ms)로 기다린 뒤 조회하고, `keepPreviousData`로 깜빡임을 막습니다. 옵션을 화면에서 다시 거르지 않습니다(`filterOption={false}`).
- **파생 값은 계산합니다**: 개수, 완료 수, 담김 여부, 증감 문구 등은 state로 따로 두지 않고 원본에서 계산합니다.
- **여러 컴포넌트가 나누는 상태**: 가장 가까운 Provider와 `lib/{영역}`의 Context + 훅으로 나눕니다. 예: `TodoProvider` + `useTodos()`
- **팝업 안 수정**: 초안(draft) state를 따로 둡니다. '수정'을 눌러야 반영하고, '취소'나 닫기는 버립니다. 열릴 때 초안을 다시 맞춥니다(`afterOpenChange`).
- **브라우저 저장소**: 이 기기 전용 편의 기능(화면 설정, 안내 본 여부)에만 씁니다. 읽고 쓸 때 항상 try/catch로 감싸고, 저장된 값은 검증한 뒤 씁니다.

## 6. 지금 있는 컴포넌트

### 공통 `components/common`

| 컴포넌트        | 하는 일                                                                  | 주요 props                                                         |
| --------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------ |
| `SearchSelect`  | 300ms 디바운스 서버 검색 Select                                          | `queryKey`, `fetcher(keyword)`, antd Select props                  |
| `ChoiceChips`   | 하나만 고르는 칩(필터). 네이티브 라디오이고 색, 체크, 굵기로 선택을 표시 | `label`, `options`, `value`, `onChange`, `block`(가득 찬 세그먼트) |
| `RemovableChip` | 고른 항목 칩. 빼기는 진짜 버튼(`{label} 빼기`)                           | `label`, `onRemove`                                                |
| `EmptyState`    | 빈 상태: 아이콘, 무엇이 없는지, 다음 동작                                | `icon`, `title`, `description`, `actions`                          |
| `WaveProgress`  | antd Progress + 차오름 + 물결. 동작 줄이기면 멈춤                        | antd Progress props                                                |
| `OfflineNotice` | 오프라인 동안 상단 안내                                                  | 없음                                                               |

### 뼈대 `components/layout`

| 컴포넌트                 | 하는 일                                                                                                                          |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| `AppLayout`              | 본문 바로가기, 헤더, main, 푸터. 문서 제목과 이동 뒤 포커스 처리. `title`, `headerContext`, `wide`, `width`, `showSettingsCoach` |
| `AppHeader`              | 헤더 v3 세 칸                                                                                                                    |
| `StepIndicator`          | 입력 → 분석 → 브리핑 단계. 지금 단계는 `aria-current="step"`이고, 지난 단계는 '완료' 글자로 표시                                 |
| `DisplaySettingsPopover` | 글자 크기와 화면 모드. 고르면 바로 적용되고, 첫 방문 안내 말풍선이 있음                                                          |

### 기능 컴포넌트

| 컴포넌트                                     | 하는 일                                                                                                                |
| -------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `items/InterestItemsEditor`                  | 원자재(최대 3)와 부자재(최대 2) 고르기. 입력 화면과 품목 수정 팝업이 함께 씀. 가득 차도 입력은 열어 두고 이유를 알려줌 |
| `items/ItemEditModal`                        | BI-PC-04 품목 수정 (초안, 취소, 수정)                                                                                  |
| `onboarding/LocationSearchModal`             | 주소 검색 팝업. 서울 밖 주소는 확인을 막음                                                                             |
| `dashboard/DashboardHero`                    | 상단 강조 블록과 신호 카드 3장. `empty`                                                                                |
| `dashboard/EventsSection`                    | 주변 예정 행사 + 유형 필터 + 추천 할 일                                                                                |
| `dashboard/RecommendationList`               | 추천 할 일. 담기·담김 토글, 피드백, 숨기기와 되돌리기, 전체 추가                                                       |
| `dashboard/PriceSection` / `PriceTrendChart` | 관심 품목 가격 타일과 4주 추이 차트(recharts). 증감색이 선, 점, 그라디언트를 따름                                      |
| `dashboard/AreaStatus` / `AreaMap`           | 지역 자동완성 선택, 혼잡도, 연령대                                                                                     |
| `dashboard/TodayChecklist` + `TodoProvider`  | 오늘 확인할 일. 체크하면 취소선, 모든 항목 삭제 확인, 최대 12개                                                        |
| `dashboard/SupportPrograms`                  | 마감 임박 지원사업 캐러셀. 3초 자동 넘김, 멈춤, 이전·다음, 점                                                          |
| `dashboard/EvidenceSection`                  | 근거 데이터 출처와 갱신 주기                                                                                           |

### 훅과 모듈

| 위치                            | 하는 일                                                |
| ------------------------------- | ------------------------------------------------------ |
| `hooks/useDebouncedValue`       | 값이 멈춘 뒤 지연 반영(기본 300ms)                     |
| `hooks/usePrefersReducedMotion` | 동작 줄이기 설정 구독                                  |
| `lib/display/displaySettings`   | 화면 설정 Context, 저장소 읽기·쓰기(`samoiro:display`) |
| `lib/dashboard/todos`           | 할 일 Context, `useTodos()`, `TODO_LIMIT`              |

## 7. 새 컴포넌트 체크리스트

- [ ] 위 목록이나 antd에 이미 있지 않은가
- [ ] `export default function`, `@/` import, props 타입을 인자에 적었는가
- [ ] 색, 글자, 간격을 토큰 클래스만으로 썼는가 (hex 값과 임의 px가 없는가)
- [ ] 어두운 화면과 글자 '아주 크게'에서 깨지지 않는가
- [ ] 모든 버튼과 링크에 이름이 있고 키보드로 닿는가
- [ ] 선택과 상태를 색만으로 표시하지 않았는가
- [ ] 빈 상태와 실패 상태를 처리했는가
- [ ] 움직임이 동작 줄이기 설정을 따르는가
- [ ] `npm run typecheck && npm run lint`를 통과하는가
