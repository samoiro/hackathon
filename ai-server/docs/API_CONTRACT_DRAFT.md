# Java ↔ Python API 계약 초안 (설계 전용)

UI/UX 확정안(`[261118]사모이로_UI-UX`)의 대시보드를 그릴 수 있게 맞춘 명세입니다. Python 쪽 `POST /v1/analyze`는 `ai-server/app/`에 구현돼 있고(본선 2026-11-21), Java 쪽은 아직입니다.
프롬프트 초안은 [`PROMPT_DRAFT.md`](PROMPT_DRAFT.md), Java가 카드 칸을 만드는 규칙은 [`SUGGESTION_TODOS_DRAFT.md`](SUGGESTION_TODOS_DRAFT.md)에 있습니다.

## 역할 나누기

분석 중 화면의 4단계 가운데 앞의 셋은 Java, 마지막 하나만 Python이 맡습니다.

| 단계 | 화면 문구 | 담당 |
| --- | --- | --- |
| 1 | 가게 정보 확인 | Java (주소·업종·품목 검증) |
| 2 | 주변 행사 조회 | Java (서울시 문화행사, 1.5km 안, 향후 7일 → 없으면 시즌 행사 → 날씨) |
| 3 | 품목 가격 조회 | Java (aT 가격 추이 4주, 부자재 준비표) |
| 4 | 준비할 일 정리 | Java가 카드 칸을 정하고 **Python**이 문장을 씀 (`POST /v1/analyze`, LLM) |

- **Java가 카드 칸(묶음·할 일 유형·품목·근거·순서)을 정하고, LLM은 칸마다 `title`·`expected_effect`·`sub_tasks` 문장만 씁니다.** 사유(`reason`), 준비 기한(`dueDate`), 강추(`recommended`), 카드 키(`cardKey`)는 Java가 붙입니다.
- 숫자(거리, 가격, 변동률, 준비 시점)는 **Java가 계산해서 넘기고**, Python은 그 숫자로 문장만 만듭니다. 날짜·시간은 `date_label`("11.21(토)")·`peak_label`("15~20시") 같은 화면용 문구로 넘깁니다.
- 지역 현황(혼잡도·연령 비율)은 기획서상 AI 브리핑과 분리돼 있어 LLM에 보내지 않습니다.
- 카드 소제목("조회된 행사 2건을 종합했어요" 등)은 프론트 고정 문구이므로 LLM이 만들지 않습니다.

## 연결 방식

| 항목 | 값 |
| --- | --- |
| 주소 | Java 환경변수 `AI_SERVER_URL` (로컬 `http://localhost:8000`) |
| 인증 | 헤더 `X-Internal-Token: {AI_SERVER_TOKEN}`. Render에서 ai-server가 공개 URL이라 필요합니다. 두 서버가 같은 값을 씁니다. |
| 형식 | `application/json`, 필드명 snake_case |
| 타임아웃 | Python의 LLM 호출 40초, Java의 읽기 대기 60초 |
| 재시도 | LLM 출력이 형식에 맞지 않으면 Python이 1번만 다시 요청합니다. Java는 재시도하지 않고 화면의 [다시 시도]에 맡깁니다. |

세 묶음 모두 칸이 0개면 Java는 Python을 부르지 않고 4단계를 완료로 두며 `suggestions`는 빈 배열입니다.

## `POST /v1/analyze`

### 요청

```json
{
  "request_id": "6f1c2a4e-0d0b-4e7b-9a51-2b1f0c3d9e10",
  "base_time": "2026-11-17T07:00:00+09:00",
  "store": {
    "gu": "성동구",
    "area_label": "성수동 · 서울숲 인근",
    "industry_category": "음식점",
    "industry_name": "한식",
    "operation_type": "BOTH"
  },
  "event_search": { "radius_m": 1500, "days": 7 },
  "events": [
    {
      "evidence_id": "E1",
      "title": "서울숲 음악회",
      "category": "공연",
      "place": "서울숲 야외무대",
      "date_label": "11.21(토)",
      "status": "UPCOMING",
      "distance_m": 650,
      "walk_minutes": 10,
      "is_free": true,
      "trigger": "PERFORMANCE",
      "time_band": "AFTERNOON",
      "peak_label": "15~20시"
    }
  ],
  "season_events": [],
  "weather": null,
  "raw_materials": [
    {
      "evidence_id": "R1",
      "item_id": 12,
      "name": "배추",
      "unit": "1포기",
      "grade": "상품",
      "base_date": "2026-11-16",
      "price": 4380,
      "change_1w_pct": 4.1,
      "change_4w_pct": 10.9,
      "trend": "UP",
      "consecutive_weeks": 4,
      "price_stage": "ALERT_HIGH",
      "signals": [{ "text": "4주 전보다 많이 오름 (상승 경계: 부류 상위 12.5%)", "demand": "STRONG_DOWN" }]
    },
    {
      "evidence_id": "R2",
      "item_id": 15,
      "name": "돼지고기",
      "unit": "100g",
      "grade": "상품",
      "base_date": "2026-11-16",
      "price": 2650,
      "change_1w_pct": -1.2,
      "change_4w_pct": -6.3,
      "trend": "DOWN",
      "consecutive_weeks": 2,
      "price_stage": "WATCH_LOW",
      "signals": [{ "text": "4주 전보다 내림 (하락 주의: 부류 하위 20%)", "demand": "UP" }]
    },
    {
      "evidence_id": "R3",
      "item_id": 7,
      "name": "쌀",
      "unit": "20kg",
      "grade": "상품",
      "base_date": "2026-11-16",
      "price": 54200,
      "change_1w_pct": 0.0,
      "change_4w_pct": 0.0,
      "trend": "FLAT",
      "consecutive_weeks": 0,
      "price_stage": "STABLE",
      "signals": []
    }
  ],
  "supplies": [
    { "evidence_id": "S1", "item_id": 3, "name": "포장 용기", "kind": "SUPPLY", "demand": "UP",
      "trigger_label": "공연", "trigger_evidence_id": "E1", "prepare_by_label": "11.18(화)" },
    { "evidence_id": "S2", "item_id": 5, "name": "일회용 수저", "kind": "SUPPLY", "demand": "UP",
      "trigger_label": "공연", "trigger_evidence_id": "E1", "prepare_by_label": "11.19(수)" }
  ],
  "cards": [
    { "card_id": "C1", "category": "EVENT", "task_type": "STAFFING", "item_id": null, "evidence_ids": ["E1"] },
    { "card_id": "C2", "category": "EVENT", "task_type": "STOCK", "item_id": null, "evidence_ids": ["E1"] },
    { "card_id": "C3", "category": "EVENT", "task_type": "PICKUP", "item_id": null, "evidence_ids": ["E1"] },
    { "card_id": "C4", "category": "INGREDIENT", "task_type": "PRICE_CHECK", "item_id": 12, "evidence_ids": ["R1"] },
    { "card_id": "C5", "category": "INGREDIENT", "task_type": "ORDER_PLAN", "item_id": 15, "evidence_ids": ["R2"] },
    { "card_id": "C6", "category": "SUPPLY", "task_type": "RESTOCK", "item_id": 3, "evidence_ids": ["S1", "E1"] },
    { "card_id": "C7", "category": "SUPPLY", "task_type": "RESTOCK", "item_id": 5, "evidence_ids": ["S2", "E1"] }
  ]
}
```

`price_stage`·`signals` 값은 설명용 예시입니다(실제 단계는 부류별 분위수로 정해짐).

| 필드 | 설명 |
| --- | --- |
| `request_id` | Java의 분석 id. 로그 추적에만 씁니다. |
| `store` | 가게 이름과 상세 주소는 **보내지 않습니다**(LLM에 개인정보를 보내지 않기 위해). 자치구와 생활권 이름까지만 보냅니다. |
| `store.operation_type` | `STORE`(매장만) · `DELIVERY`(배달만) · `BOTH` · `null`(선택 안 함) |
| `event_search.radius_m` | 1500 (고정) |
| `events` · `season_events` · `weather` | `EVENT` 칸의 근거입니다. 주변 행사(E)가 있으면 `events`, 없으면 시즌 행사(H) `season_events`, 그것도 없으면 날씨(W) `weather`를 채웁니다. 부자재 트리거가 H·W이면 그 근거도 넣습니다. 안 쓰는 것은 빈 배열·`null` |
| `events[].date_label` · `peak_label` | 화면용 날짜 문구와 붐비는 시간(시작 1시간 전 ~ 끝 1시간 후). LLM이 그대로 옮깁니다. 시간을 못 읽으면 `peak_label`은 `null` |
| `events[].status` | `UPCOMING` · `ONGOING` |
| `events[].trigger` | `FESTIVAL`(축제) · `PERFORMANCE`(공연). 부자재 준비표 규칙도 이 값으로 켭니다. |
| `events[].time_band` | 시간 문구(`PRO_TIME`)에서 읽은 시간대 `MORNING`·`LUNCH`·`AFTERNOON`·`EVENING`, 못 읽으면 `UNKNOWN` |
| `season_events[]` | `evidence_id`, `code`, `label`, `type`(`HOLIDAY`·`SCHOOL`·`SEASON`), `date_label`, `days_left` |
| `weather` | `evidence_id`, `impacts`(`WeatherImpact`), `warnings`(`kind`·`level`), `first_date_label`(영향이 시작되는 날) |
| `raw_materials[].price` | 가격 자료가 없는 품목이면 `price`, `change_*`, `trend`, `consecutive_weeks`, `price_stage`가 모두 `null`입니다. |
| `raw_materials[].trend` | `UP` · `DOWN` · `FLAT`. 1주 전 대비 변동률로 정한 화면용 흐름(±1% 미만이면 `FLAT`, 기준은 합의 필요). 점수에는 쓰지 않습니다. |
| `raw_materials[].price_stage` | 가격 단계 7개(`PriceStage`): `SEVERE_HIGH`·`ALERT_HIGH`·`WATCH_HIGH`·`STABLE`·`WATCH_LOW`·`ALERT_LOW`·`SEVERE_LOW`. **4주 변동률**을 부류별 분위수로 나눈 값(수급조절매뉴얼 방식)이고, 원자재 가격 신호는 이 값만 씁니다. |
| `raw_materials[].signals` | 이번 주 걸린 `ingredient_rule`의 `trigger_text`(`text`)와 `demand`. 가격 단계 규칙, 제철, 김장·설·추석 전 규칙이 들어갑니다. |
| `supplies` | 부자재 준비표(`supply_rule`)에서 이번 주에 걸린 규칙만, 물품당 1개 보냅니다. `trigger_evidence_id`는 행사·시즌·날씨 근거 id이고, 없으면 `null`입니다. |
| `supplies[].kind` | `SUPPLY`(부자재) · `STOCK`(손님에게 파는 상품) |
| `supplies[].demand` | `UP` · `STRONG_UP` · `DOWN` |
| `supplies[].prepare_by_label` | 이날까지 준비(화면용 문구) |
| `cards` | **Java가 정한 카드 칸**, 화면에 보일 순서대로. 묶음당 최대 `EVENT` 3 · `INGREDIENT` 3 · `SUPPLY` 2. 사장님이 "해당 없어요"로 뺀 칸(`excludedKeys`)은 Java가 이미 뺐습니다. |
| `cards[].evidence_ids` | 첫 번째가 **대표 근거**입니다. Java의 `reason`과 LLM의 `title`·`expected_effect`가 이 근거로 쓰입니다. |

`evidence_id`는 Java가 요청마다 붙이는 짧은 id입니다(행사 `E`, 시즌 `H`, 날씨 `W`, 원자재 `R`, 부자재 `S` + 번호).
"해당 없어요" 제외 목록은 LLM에 보내지 않습니다. 예전 `excluded_titles`는 없앴습니다(같은 일을 다른 말로 다시 쓸 수 있어서 Java가 카드 키로 거릅니다).

#### 묶음과 할 일 유형

`category`는 `EVENT`(행사 종합) · `INGREDIENT`(원자재 품목별) · `SUPPLY`(부자재 품목별)입니다. `task_type`은 세 묶음 모두 필수이고 Java `TaskType` 13개 값 중 묶음에 맞는 것입니다.

| 묶음 | `task_type` |
| --- | --- |
| `EVENT` | `STAFFING` 근무 배치 · `STOCK` 재고 · `HOURS` 영업시간 · `PICKUP` 주문·픽업 안내 · `RESERVATION` 예약 안내 · `FACILITY` 매장 정비 · `PACKING` 포장 동선 |
| `INGREDIENT` | `PRICE_CHECK` 단가 확인 · `BUY_AHEAD` 미리 주문 · `ORDER_PLAN` 발주량 점검 |
| `SUPPLY` | `RESTOCK` 재고 채우기 · `DISPLAY` 판매 상품 진열 · `ORDER_LESS` 주문 줄이기 |

어떤 근거·업종·운영 방식에서 어떤 유형을 여는지는 [`SUGGESTION_TODOS_DRAFT.md`](SUGGESTION_TODOS_DRAFT.md) "카드 칸 만들기"를 따릅니다.

### 응답 (200)

```json
{
  "suggestions": [
    {
      "card_id": "C1",
      "title": "토요일 공연 시간대 역할 나누기",
      "expected_effect": "공연 전후로 붐비는 시간에 주문·수령 혼선을 줄일 수 있어요.",
      "sub_tasks": ["토요일 15~20시 근무자 정하기", "주문·수령 담당 나누기", "근무표 미리 공유하기"]
    },
    {
      "card_id": "C4",
      "title": "배추 거래처 단가 확인",
      "expected_effect": "배추값이 오른 만큼 메뉴 원가 부담을 미리 살펴볼 수 있어요.",
      "sub_tasks": ["배추 거래처 단가 묻기", "배추 들어간 메뉴 원가 보기", "배추 대체 재료 알아보기"]
    },
    {
      "card_id": "C6",
      "title": "포장 용기 재고 확인",
      "expected_effect": "공연 준비 마감 전에 포장 용기가 모자라지 않게 할 수 있어요.",
      "sub_tasks": ["포장 용기 남은 수량 세기", "11.18(화)까지 포장 용기 주문"]
    }
  ],
  "model": "provider/model-name",
  "generated_at": "2026-11-17T07:00:12+09:00"
}
```

(예시는 C2·C3·C5·C7을 줄였습니다. 실제 응답에는 요청의 모든 칸이 있어야 합니다.)

| 규칙 | 내용 |
| --- | --- |
| `card_id` | 요청의 `cards`에 있는 id를 **빠짐없이 한 번씩**. 순서는 상관없습니다(Java가 요청 순서로 둡니다). |
| `title` | 24자 이내. 대상 + 행동 명사형("~확인", "~점검", "~정하기", "~나누기"). `INGREDIENT`·`SUPPLY`는 품목 이름을 넣습니다. 한 응답 안에서 겹치지 않습니다. |
| `expected_effect` | 60자 이내 해요체 한 문장, "~수 있어요."로 끝냅니다. 아래 "기대 효과의 근거"를 지키지 못하면 `null`. |
| `sub_tasks` | 2~3개, 각 24자 이내, 하는 순서대로. 할 일 목록에 따로 담기므로 대상(품목·요일·시간)을 넣어 혼자 읽어도 통하게 씁니다. 제목을 그대로 되풀이하지 않습니다. |
| 숫자·날짜 | `title`·`sub_tasks`의 숫자는 입력에 있는 것만, 날짜·시간은 `date_label`·`peak_label`·`prepare_by_label`을 그대로 옮깁니다. |
| 뺀 필드 | `category`, `task_type`, `item_id`, `evidence_ids`는 Java가 이미 알아서 받지 않습니다. |

#### 기대 효과의 근거

`expected_effect`는 **그 칸의 대표 근거(`evidence_ids[0]`) 하나에 기댑니다.** `reason`과 같은 근거라서 화면은 사유 바로 아래에 기대 효과를 두어 "이 근거라서 이런 효과"로 읽히게 합니다. 따로 근거 id 필드를 두지 않습니다.

- 대표 근거가 말하는 상황(붐비는 시간, 행사 날, 가격 상승·하락, 준비 마감, 날씨)에 할 일이 어떻게 대비하는지만 씁니다.
- **숫자를 쓰지 않습니다.** 숫자는 `reason`에 이미 있습니다.
- 매출·수익, 손님·방문객 수, 주문 수의 증감, 절약 금액처럼 근거가 말하지 않는 결과를 쓰지 않습니다. "~할 거예요", "반드시"처럼 단정하지 않습니다.
- 대표 근거로 말할 효과가 없으면 지어내지 말고 `null`을 줍니다. 화면은 그 줄을 숨깁니다.

### 검증과 실패

- Python: 길이·개수, `card_id` 누락·중복, `title`·`sub_tasks`의 입력에 없는 숫자, `expected_effect`의 숫자·금지어(매출, 수익, 방문객, 손님이 늘)·끝맺음("수 있어요.")을 검사하고, 어기면 오류를 붙여 1번 다시 요청합니다. 그래도 `expected_effect`만 어긴 칸은 `expected_effect`를 `null`로 바꿔 남기고, 다른 규칙을 어긴 칸은 뺍니다. 남은 칸이 0개면 502 `INVALID_LLM_OUTPUT`.
- Java: 요청에 없는 `card_id`는 버립니다. 응답이 빠진 칸은 화면에 내지 않습니다. 칸이 있었는데 남은 카드가 0개면 4단계 실패입니다. 칸이 처음부터 0개면 Python을 부르지 않고 실패도 아닙니다.
- Java가 `cardKey`·`reason`·`dueDate`를 붙이고 강추를 고른 뒤 `SuggestionResponse`로 저장합니다.

### 준비 기한 (Java)

카드마다 `SuggestionResponse.dueDate`(LocalDate)로 "언제까지 준비할지"를 보여 줍니다. 아래 "강추 고르기"의 **행동 마감과 같은 날짜**라서, 점수의 급한 정도와 화면의 기한이 어긋나지 않습니다. Python에는 보내지 않습니다.

| 묶음 · 대표 근거 | `dueDate` | 근거 |
| --- | --- | --- |
| `EVENT` · E 주변 행사 | 행사 시작일. 진행 중이면 기준일 | 행사 전에 끝내야 하는 준비 |
| `EVENT` · H 시즌 행사 | 시즌 행사 시작일. 기간 중이면 기준일 | 같음 |
| `EVENT` · W 날씨 | 영향이 시작되는 날(첫 영향 예보일). 특보 발효 중이면 기준일 | 날씨가 바뀌기 전에 하는 정비 |
| `SUPPLY` | 그 물품의 `prepareBy` | 준비표의 받아 둘 날짜를 그대로 씀 |
| `INGREDIENT` | `null`. 화면은 "이번 주 발주 전"으로 보여 줍니다 | 가격 신호에는 날짜가 없고, 점수도 "이번 주 발주 전"(4~7일)으로 고정해서 |

## 행사 후보 (Java)

행사는 이 순서로 걸러 `events`에 넣습니다.

1. 활성 오프라인 행사(`cultural_event.is_active`) 중 분류가 아래 13개인 것
2. 진행 중이거나 7일 안에 시작. 15일 이상 이어지는 행사는 시작 후 첫 7일만
3. 가게에서 1.5km 안
4. 같은 제목·장소는 회차를 묶어 1건
5. 강추 점수 순으로 최대 5건

| 행사 분류(`CODENAME`) | 트리거 |
| --- | --- |
| 축제-문화/예술 · 축제-기타 · 축제-전통/역사 · 축제-자연/경관 · 축제-시민화합 · 축제-관광/체육 | `FESTIVAL` |
| 콘서트 · 클래식 · 국악 · 독주/독창회 · 뮤지컬/오페라 · 연극 · 무용 | `PERFORMANCE` |
| 교육/체험 · 전시/미술 · 영화 · 기타 | 후보 아님 (사전 신청 소규모, 몇 주씩 이어져 몰리는 날이 없음, 강당 상영, 포럼·강연 섞임) |

## 강추 고르기 (Java)

강추는 유형별 1개가 아니라, **모든 카드 중 점수가 가장 높은 3개**입니다. 점수는 LLM이 아니라 Java가 근거(`E`·`H`·`W`·`R`·`S`)마다 매기고, 카드의 점수는 그 카드의 `evidence_ids` 중 가장 높은 근거 점수입니다.

근거 점수 = 급한 정도 + 영향 크기 + 확실성 (각 0~3점, 최대 9점)

| 축 | 3점 | 2점 | 1점 | 0점 |
| --- | --- | --- | --- | --- |
| 급한 정도: 행동 마감까지 남은 날 | 0~1일 | 2~3일 | 4~7일 | 8일 이상 |
| 영향 크기 | 행사 500m 이내 · 기상특보 · 원자재 신호 크기 3 이상 · 부자재 `STRONG_UP` | 행사 1km 이내 · 영업에 영향 주는 예보 · 원자재 신호 크기 2 · 부자재 `UP` | 행사 1.5km 이내 · 시즌 행사 · 원자재 신호 크기 1 · 부자재 `DOWN` | 원자재 신호 0 (후보 아님) |
| 확실성 | 날짜가 정해진 행사·공휴일·학사일정, 조사된 가격 | 날씨 예보·특보 | 그 밖의 추정 | |

- 행동 마감: 행사는 시작일까지(진행 중이면 0일), 부자재는 `prepare_by`까지, 날씨는 영향이 시작되는 날까지입니다. 이 날짜가 카드의 준비 기한(`dueDate`)입니다. 원자재는 "이번 주 발주 전"으로 보고 4~7일(1점)로 두고 `dueDate`는 `null`입니다. 0일로 두면 작은 가격 변동도 늘 1등이 되기 때문입니다.
- 원자재 신호는 걸린 규칙의 `demand`를 더한 값입니다: 제철 +2, 제철 한 달 전 +1, 김장·설·추석 전 +1, 4주 가격 단계(`price_stage`) 하락 주의 +1 · 하락 경계·심각 +2 · 상승 주의 −1 · 상승 경계·심각 −2. 1주 변동(`trend`)은 점수에 넣지 않습니다. 오른 품목(단가 확인)도 할 일이 되므로 크기(절댓값)로 영향을 봅니다.
- 실시간 혼잡도는 강추 계산에 쓰지 않습니다. 지역 현황은 대시보드에 따로 보여주고 LLM에도 보내지 않습니다(위 "역할 나누기"). 혼잡도로만 켜지는 부자재 `CROWD_SURGE` 규칙도 후보가 되지 않습니다.
- 고르는 순서: 점수 높은 순 → 같으면 급한 정도 높은 순 → 같으면 `cards` 순서(Java가 정한 묶음 안 순서).
- 부자재 근거의 확실성은 그 부자재를 걸리게 한 행사·날씨 근거(`trigger_evidence_id`)를 따릅니다.
- 같은 유형은 강추 최대 2개, 같은 대표 근거로 만든 카드는 강추 1개(`cards` 순서상 첫 번째)까지입니다.
- 강추도 사장님이 정리할 수 있습니다. 강추 카드와 할 일에도 `reason`·`dueDate`를 보여 주고, "이미 했어요"(완료)·"해당 없어요"(피드백 `NOT_RELEVANT` + 할 일에서 뺌 + 다음 분석의 `excludedKeys`)로 정리합니다. 같은 분석 안에서 강추를 다시 고르지는 않습니다.
- 점수 구간은 실제 데이터로 돌려 보기 전 초안입니다.

### 오류

본문은 `{"code": "...", "detail": "사람이 읽을 설명"}` 형식입니다. Java는 어떤 오류든 **4단계 실패**로 기록하고, 화면에는 "브리핑을 만들다가 멈췄어요"와 [다시 시도]를 보여줍니다.

| 상태 | `code` | 언제 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | `X-Internal-Token`이 없거나 틀림 |
| 422 | `INVALID_REQUEST` | 요청 스키마 오류 |
| 502 | `INVALID_LLM_OUTPUT` | 재시도 후에도 남은 칸이 0개 |
| 503 | `LLM_UNAVAILABLE` | LLM 공급자 오류, API 키 미설정 |
| 504 | `LLM_TIMEOUT` | LLM 호출 40초 초과 |

`detail`과 로그에 API 키나 요청 원문을 넣지 않습니다.

## 남은 결정

- LLM 모델: OpenAI GPT 6.2 Astra로 정함([`docs/plan-feedback.md`](../../docs/plan-feedback.md) 1절). `LLM_MODEL`에 넣을 API 모델 id만 확인 필요
- `trend`를 `FLAT`으로 보는 변동률 기준
- 카드 칸 규칙의 남은 결정: [`SUGGESTION_TODOS_DRAFT.md`](SUGGESTION_TODOS_DRAFT.md) "남은 결정"
