# Java ↔ Python API 계약 초안 (설계 전용)

**아직 구현하지 않은 API의 제안 명세입니다.** UI/UX 확정안(`[261118]사모이로_UI-UX`)의 대시보드를 그릴 수 있게 맞췄고, Python 담당자와 합의한 뒤 확정합니다.
프롬프트 초안은 [`PROMPT_DRAFT.md`](PROMPT_DRAFT.md)에 있습니다.

## 역할 나누기

분석 중 화면의 4단계 가운데 앞의 셋은 Java, 마지막 하나만 Python이 맡습니다.

| 단계 | 화면 문구 | 담당 |
| --- | --- | --- |
| 1 | 가게 정보 확인 | Java (주소·업종·품목 검증) |
| 2 | 주변 행사 조회 | Java (서울시 문화행사, 1km → 없으면 1.5km, 향후 7일) |
| 3 | 품목 가격 조회 | Java (aT 가격 추이 4주, 부자재 준비표) |
| 4 | 준비할 일 정리 | **Python** (`POST /v1/analyze`, LLM) |

- 숫자(거리, 가격, 변동률, 준비 시점)는 **Java가 계산해서 넘기고**, Python은 그 숫자로 추천 문장만 만듭니다. 화면의 행사 목록, 가격, 추이 그래프도 Java가 계산한 값을 그대로 씁니다.
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
  "event_search": { "radius_m": 1000, "days": 7 },
  "events": [
    {
      "evidence_id": "E1",
      "title": "서울숲 음악회",
      "category": "공연",
      "place": "서울숲 야외무대",
      "start": "2026-11-21T16:00:00+09:00",
      "end": "2026-11-21T19:00:00+09:00",
      "distance_m": 650,
      "walk_minutes": 10,
      "is_free": true,
      "trigger": "PERFORMANCE",
      "time_band": "AFTERNOON",
      "peak_time": "15:00~20:00",
      "task_types": ["STAFFING", "PICKUP", "PACKING", "STOCK"]
    }
  ],
  "raw_materials": [
    {
      "evidence_id": "R1",
      "item_id": 12,
      "name": "배추",
      "unit": "1포기",
      "grade": "상품",
      "base_date": "2026-11-16",
      "price": 4380,
      "weekly_prices": [3950, 4050, 4120, 4208, 4380],
      "change_1w_pct": 4.1,
      "change_4w_pct": 10.9,
      "trend": "UP"
    },
    {
      "evidence_id": "R3",
      "item_id": 7,
      "name": "쌀",
      "unit": "20kg",
      "grade": "상품",
      "base_date": "2026-11-16",
      "price": 54200,
      "weekly_prices": [54200, 54200, 54200, 54200, 54200],
      "change_1w_pct": 0.0,
      "change_4w_pct": 0.0,
      "trend": "FLAT"
    }
  ],
  "supplies": [
    {
      "evidence_id": "S1",
      "item_id": 3,
      "name": "포장 용기",
      "trigger": "공연",
      "trigger_evidence_id": "E1",
      "prepare_by": "2026-11-18",
      "lead_days_min": 3,
      "lead_days_max": 3,
      "demand": "UP"
    }
  ],
  "excluded_titles": ["돼지고기 발주량 점검"]
}
```

| 필드 | 설명 |
| --- | --- |
| `request_id` | Java의 분석 id. 로그 추적에만 씁니다. |
| `store` | 가게 이름과 상세 주소는 **보내지 않습니다**(LLM에 개인정보를 보내지 않기 위해). 자치구와 생활권 이름까지만 보냅니다. |
| `store.operation_type` | `STORE`(매장만) · `DELIVERY`(배달만) · `BOTH` · `null`(선택 안 함) |
| `event_search.radius_m` | 1000. 1km 안에 행사가 없어서 넓혀 찾았으면 1500 |
| `events` | 비어 있을 수 있습니다. 진행 중 행사는 `start`가 기준 시각보다 앞섭니다. 후보는 아래 "행사 후보와 할 일 유형"으로 Java가 고릅니다. |
| `events[].trigger` | `FESTIVAL`(축제) · `PERFORMANCE`(공연). 부자재 준비표 규칙도 이 값으로 켭니다. |
| `events[].time_band` · `peak_time` | 시간 문구(`PRO_TIME`)에서 읽은 시간대(`MORNING`·`LUNCH`·`AFTERNOON`·`EVENING`)와 붐비는 시간(시작 1시간 전 ~ 끝 1시간 후). 못 읽으면 `UNKNOWN`·`null` |
| `events[].task_types` | 이 행사로 만들 수 있는 할 일 유형. Java가 업종·운영 방식·시간으로 고릅니다. |
| `raw_materials[].price` | 가격 자료가 없는 품목이면 `price`, `weekly_prices`, `change_*`, `trend`가 모두 `null`입니다. |
| `raw_materials[].weekly_prices` | 4주 전 → 기준일 순서로 5개. 비어 있는 주는 `null` |
| `raw_materials[].trend` | `UP` · `DOWN` · `FLAT`. Java가 1주 전 대비 변동률로 정합니다(±1% 미만이면 `FLAT`, 기준은 합의 필요). |
| `supplies` | 부자재 준비표(`supply_rule`)에서 이번 주에 걸린 규칙만 보냅니다. `trigger_evidence_id`는 행사·날씨 근거 id이고, 없으면 `null`입니다. |
| `supplies[].demand` | `UP` · `STRONG_UP` · `DOWN` |
| `excluded_titles` | 이 가게가 이전 브리핑에서 "해당 없어요"로 표시한 추천 제목 |

`evidence_id`는 Java가 요청마다 붙이는 짧은 id입니다(행사 `E`, 원자재 `R`, 부자재 `S` + 번호).

### 응답 (200)

```json
{
  "suggestions": [
    {
      "category": "EVENT",
      "task_type": "STAFFING",
      "item_id": null,
      "title": "공연 시간대 근무자 역할 나누기",
      "expected_effect": "주문·수령 안내 혼선을 줄이는 데 도움이 될 수 있어요.",
      "evidence_ids": ["E1"]
    },
    {
      "category": "RAW_MATERIAL",
      "item_id": 12,
      "title": "배추 거래처 단가 확인",
      "expected_effect": "다음 발주 전에 단가 변동을 미리 확인할 수 있어요.",
      "evidence_ids": ["R1"]
    },
    {
      "category": "SUPPLY",
      "item_id": 3,
      "title": "포장 용기 재고 확인",
      "expected_effect": "행사 전 포장 주문 증가에 대비할 수 있어요.",
      "evidence_ids": ["S1", "E1"]
    }
  ],
  "model": "provider/model-name",
  "generated_at": "2026-11-17T07:00:12+09:00"
}
```

| 규칙 | 내용 |
| --- | --- |
| `category` | `EVENT`(행사 종합) · `RAW_MATERIAL`(원자재 품목별) · `SUPPLY`(부자재 품목별) |
| `task_type` | `EVENT`만 필수이고 근거 행사의 `task_types` 중 하나. 나머지는 `null` |
| 개수 | 묶음마다 최대 3개. 입력이 비어 있는 묶음은 0개 |
| `item_id` | `EVENT`는 `null`, 나머지는 입력에 있는 `item_id` 중 하나 |
| 순서 | 묶음 안에서는 먼저 해야 할 일부터 둡니다. 강추는 Python이 정하지 않고 Java가 근거 점수로 고릅니다(아래 "강추 고르기"). |
| `title` | 24자 이내, 명사형으로 끝나는 할 일("~확인", "~점검", "~나누기") |
| `expected_effect` | 60자 이내, "~수 있어요"처럼 단정하지 않는 문장. 숫자는 입력에 있는 것만 씁니다. |
| `evidence_ids` | 1개 이상, 모두 입력에 있는 id |
| 제외 | `excluded_titles`와 같은 제목은 만들지 않습니다. |

### Java 쪽 검증

Python 응답을 그대로 믿지 않고, 저장하기 전에 한 번 더 거릅니다.

- 입력에 없는 `evidence_id`나 `item_id`가 들어간 추천, 근거 행사의 `task_types`에 없는 `task_type`을 단 `EVENT` 추천은 버립니다.
- 남은 추천으로 강추를 고릅니다(아래 "강추 고르기").
- Python이 준 추천이 걸러져서 하나도 남지 않으면 4단계 실패로 처리합니다. 입력(행사·품목)이 모두 비어서 처음부터 추천이 없는 경우는 실패가 아닙니다.

### 행사 후보와 할 일 유형 (Java)

행사는 이 순서로 걸러 `events`에 넣습니다.

1. 활성 오프라인 행사(`cultural_event.is_active`) 중 분류가 아래 13개인 것
2. 진행 중이거나 7일 안에 시작. 15일 이상 이어지는 행사는 시작 후 첫 7일만
3. 가게에서 1km 안, 없으면 1.5km 안
4. 같은 제목·장소는 회차를 묶어 1건
5. 강추 점수 순으로 최대 5건

| 행사 분류(`CODENAME`) | 트리거 |
| --- | --- |
| 축제-문화/예술 · 축제-기타 · 축제-전통/역사 · 축제-자연/경관 · 축제-시민화합 · 축제-관광/체육 | `FESTIVAL` |
| 콘서트 · 클래식 · 국악 · 독주/독창회 · 뮤지컬/오페라 · 연극 · 무용 | `PERFORMANCE` |
| 교육/체험 · 전시/미술 · 영화 · 기타 | 후보 아님 (사전 신청 소규모, 몇 주씩 이어져 몰리는 날이 없음, 강당 상영, 포럼·강연 섞임) |

할 일 유형은 행사·업종·운영 방식이 맞을 때만 `task_types`에 넣습니다. LLM은 이 안에서만 `EVENT` 할 일을 씁니다.

| 유형 | 할 일 예 | 넣는 조건 |
| --- | --- | --- |
| `STAFFING` 근무 배치 | 붐비는 시간 근무자 역할 나누기 | 음식점·소매점 |
| `HOURS` 영업시간 | 행사 끝나는 21시까지 영업 연장 검토 | 행사가 21시 이후 끝나거나 11시 전에 시작 (시간 미상이면 뺌) |
| `PICKUP` 주문·픽업 안내 | 행사 전 미리 주문·픽업 안내문 붙이기 | 음식점이고 운영 방식이 `STORE`·`BOTH`·모름 |
| `PACKING` 포장 동선 | 포장 대기 줄·픽업 자리 정하기 | 음식점이고 축제이거나 행사가 17시 이후에 끝남 (저녁 손님과 겹침) |
| `STOCK` 재고 | 많이 나갈 메뉴·상품 재고 확인 | 음식점·소매점. 부자재 묶음에 같은 물품이 있으면 그쪽에서 다룹니다. |
| `RESERVATION` 예약 안내 | 행사 시간대 예약 손님에게 주차·교통 안내 | 단체·개인서비스업 |

### 강추 고르기 (Java)

강추는 유형별 1개가 아니라, **모든 추천 중 점수가 가장 높은 3개**입니다. 점수는 LLM이 아니라 Java가 근거(`E`·`H`·`W`·`R`·`S`)마다 매기고, 추천의 점수는 그 추천이 단 `evidence_ids` 중 가장 높은 근거 점수입니다.

근거 점수 = 급한 정도 + 영향 크기 + 확실성 (각 0~3점, 최대 9점)

| 축 | 3점 | 2점 | 1점 | 0점 |
| --- | --- | --- | --- | --- |
| 급한 정도: 행동 마감까지 남은 날 | 0~1일 | 2~3일 | 4~7일 | 8일 이상 |
| 영향 크기 | 행사 500m 이내 · 기상특보 · 원자재 신호 크기 3 이상 · 부자재 `STRONG_UP` | 행사 1km 이내 · 영업에 영향 주는 예보 · 원자재 신호 크기 2 · 부자재 `UP` | 행사 1.5km 이내 · 시즌 행사 · 원자재 신호 크기 1 · 부자재 `DOWN` | 원자재 신호 0 (후보 아님) |
| 확실성 | 날짜가 정해진 행사·공휴일·학사일정, 조사된 가격 | 날씨 예보·특보 | 그 밖의 추정 | |

- 행동 마감: 행사는 시작일까지(진행 중이면 0일), 부자재는 `prepare_by`까지, 날씨는 영향이 시작되는 날까지입니다. 원자재는 "이번 주 발주 전"으로 보고 4~7일(1점)로 둡니다. 0일로 두면 작은 가격 변동도 늘 1등이 되기 때문입니다.
- 원자재 신호는 걸린 규칙의 `demand`를 더한 값입니다: 제철 +2, 제철 한 달 전 +1, 1주 전보다 하락 +1, 10% 이상 하락 +2, 상승 −1. 오른 품목(단가 확인)도 할 일이 되므로 크기(절댓값)로 영향을 봅니다.
- 실시간 혼잡도는 강추 계산에 쓰지 않습니다. 지역 현황은 대시보드에 따로 보여주고 LLM에도 보내지 않습니다(위 "역할 나누기"). 혼잡도로만 켜지는 부자재 `CROWD_SURGE` 규칙도 후보가 되지 않습니다.
- 고르는 순서: 점수 높은 순 → 같으면 급한 정도 높은 순 → 같으면 Python이 준 순서.
- 부자재 근거의 확실성은 그 부자재를 걸리게 한 행사·날씨 근거(`trigger_evidence_id`)를 따릅니다.
- 같은 유형은 강추 최대 2개, 같은 근거로 만든 추천은 강추 1개(Python이 준 순서상 첫 번째)까지입니다.
- 점수 구간은 실제 데이터로 돌려 보기 전 초안입니다.

### 오류

본문은 `{"code": "...", "detail": "사람이 읽을 설명"}` 형식입니다. Java는 어떤 오류든 **4단계 실패**로 기록하고, 화면에는 "브리핑을 만들다가 멈췄어요"와 [다시 시도]를 보여줍니다.

| 상태 | `code` | 언제 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | `X-Internal-Token`이 없거나 틀림 |
| 422 | `INVALID_REQUEST` | 요청 스키마 오류 |
| 502 | `INVALID_LLM_OUTPUT` | 재시도 후에도 LLM 출력이 규칙에 맞지 않음 |
| 503 | `LLM_UNAVAILABLE` | LLM 공급자 오류, API 키 미설정 |
| 504 | `LLM_TIMEOUT` | LLM 호출 40초 초과 |

`detail`과 로그에 API 키나 요청 원문을 넣지 않습니다.

## 남은 결정

- LLM 모델 (`LLM_MODEL`). 공급자는 OpenAI로 정함
- `trend`를 `FLAT`으로 보는 변동률 기준
- 날씨 근거(`W` id)를 부자재 트리거로 함께 보낼지 (준비표에 폭염·한파·비 트리거가 있음)
