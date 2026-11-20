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
      "is_free": true
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
| `events` | 비어 있을 수 있습니다. 진행 중 행사는 `start`가 기준 시각보다 앞섭니다. |
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
      "item_id": null,
      "title": "공연 시간대 근무자 역할 나누기",
      "expected_effect": "주문·수령 안내 혼선을 줄이는 데 도움이 될 수 있어요.",
      "recommended": true,
      "evidence_ids": ["E1"]
    },
    {
      "category": "RAW_MATERIAL",
      "item_id": 12,
      "title": "배추 거래처 단가 확인",
      "expected_effect": "다음 발주 전에 단가 변동을 미리 확인할 수 있어요.",
      "recommended": true,
      "evidence_ids": ["R1"]
    },
    {
      "category": "SUPPLY",
      "item_id": 3,
      "title": "포장 용기 재고 확인",
      "expected_effect": "행사 전 포장 주문 증가에 대비할 수 있어요.",
      "recommended": true,
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
| 개수 | 묶음마다 최대 3개. 입력이 비어 있는 묶음은 0개 |
| `item_id` | `EVENT`는 `null`, 나머지는 입력에 있는 `item_id` 중 하나 |
| `recommended` | 추천이 있는 묶음마다 정확히 1개가 `true` → 오늘 확인할 일에 처음부터 담기는 "강추"(최대 3건) |
| `title` | 24자 이내, 명사형으로 끝나는 할 일("~확인", "~점검", "~나누기") |
| `expected_effect` | 60자 이내, "~수 있어요"처럼 단정하지 않는 문장. 숫자는 입력에 있는 것만 씁니다. |
| `evidence_ids` | 1개 이상, 모두 입력에 있는 id |
| 제외 | `excluded_titles`와 같은 제목은 만들지 않습니다. |

### Java 쪽 검증

Python 응답을 그대로 믿지 않고, 저장하기 전에 한 번 더 거릅니다.

- 입력에 없는 `evidence_id`나 `item_id`가 들어간 추천은 버립니다.
- 강추가 없는 묶음은 첫 번째 추천을 강추로 바꾸고, 2개 이상이면 첫 번째만 남깁니다.
- Python이 준 추천이 걸러져서 하나도 남지 않으면 4단계 실패로 처리합니다. 입력(행사·품목)이 모두 비어서 처음부터 추천이 없는 경우는 실패가 아닙니다.

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

- LLM 공급자와 모델 (`LLM_MODEL`)
- `trend`를 `FLAT`으로 보는 변동률 기준
- 날씨 근거(`W` id)를 부자재 트리거로 함께 보낼지 (준비표에 폭염·한파·비 트리거가 있음)
