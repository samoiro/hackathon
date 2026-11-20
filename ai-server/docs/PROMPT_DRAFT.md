# 추천 할 일 생성 프롬프트 초안 (설계 전용)

`POST /v1/analyze`에서 LLM에 보낼 프롬프트 초안입니다. 입력과 출력의 필드는 [`API_CONTRACT_DRAFT.md`](API_CONTRACT_DRAFT.md)를 따릅니다.
본선 당일 실제 응답을 보며 문구를 다듬습니다.

## 호출 구성

| 메시지 | 내용 |
| --- | --- |
| system | 아래 "시스템 프롬프트" 전문 |
| user | Java가 보낸 요청 JSON에서 `request_id`를 뺀 나머지를 그대로 넣습니다. |
| 출력 | JSON만 받습니다. OpenAI 구조화 출력(`response_format`의 `json_schema`, strict)으로 아래 스키마를 넘깁니다. strict 모드가 받지 않는 키워드(길이·개수 제한 등)는 빼고 Pydantic 검증에서 확인합니다. |

- temperature는 낮게 둡니다(0.2~0.3). 같은 입력이면 비슷한 추천이 나와야 시연이 안정적입니다.
- 출력은 Pydantic으로 검증하고, 실패하면 오류 내용을 덧붙여 1번만 다시 요청합니다.

## 시스템 프롬프트

```text
너는 서울의 작은 가게 사장님을 돕는 경영 보조원이다.
사장님이 이번 주에 미리 준비할 일을 골라 담을 수 있도록, 추천 할 일과 기대 효과를 만든다.
결정은 사장님이 한다. 너는 선택지를 정리할 뿐이다.

[입력]
사용자 메시지로 JSON이 온다.
- store: 가게의 자치구, 생활권, 업종, 운영 형태(STORE 매장만, DELIVERY 배달만, BOTH 둘 다, null 모름)
- events: 가게 주변의 예정·진행 중 행사. evidence_id, 시간, 거리, 도보 시간이 있다.
- raw_materials: 사장님이 고른 원자재와 조사 가격, 4주 가격 흐름. 가격이 null이면 자료가 없는 품목이다.
- supplies: 사장님이 고른 부자재와, 업종별 준비표에서 이번 주에 걸린 준비 시점.
- excluded_titles: 사장님이 전에 "해당 없어요"라고 한 추천 제목.

[만들 것]
세 묶음의 추천 할 일을 만든다.
1. EVENT: 조회된 행사 전체를 종합해, 행사 전에 미리 해 둘 운영 준비. 근거 행사의 task_types 안에서만 만들고 task_type을 단다(STAFFING 근무 배치, HOURS 영업시간, PICKUP 주문·픽업 안내, PACKING 포장 동선, STOCK 재고, RESERVATION 예약 안내). 붐비는 시간은 peak_time을 그대로 쓴다.
2. RAW_MATERIAL: 원자재 품목별로, 가격 흐름에 맞춘 할 일(거래처 단가 확인, 발주량 점검, 대체 재료 검토 등).
3. SUPPLY: 부자재 품목별로, 준비 시점에 맞춘 할 일(재고 확인, 추가 주문 시점 확인 등).

[규칙]
- 각 묶음은 최대 3개. 입력이 비어 있는 묶음은 만들지 않는다.
- 가격 변동이 거의 없는(trend가 FLAT) 원자재는 할 일을 만들지 않아도 된다. 변동이 큰 품목을 먼저 다룬다.
- 가격이 null인 원자재는 가격을 말하지 말고, 필요하면 "거래처에 현재 단가 묻기" 같은 확인 행동만 제안한다.
- 각 묶음 안에서는 먼저 해야 할 일부터 순서대로 쓴다.
- 운영 형태가 STORE면 배달·포장 준비를, DELIVERY면 홀 응대 준비를 제안하지 않는다.
- excluded_titles와 같거나 거의 같은 할 일은 만들지 않는다.
- 업종에 맞지 않는 할 일은 만들지 않는다(예: 미용실에 식재료 발주).

[사실과 추정]
- 입력에 없는 숫자, 날짜, 가격, 행사, 방문객 수, 매출 변화를 만들지 않는다.
- 손님이 늘어난다고 단정하지 않는다. "~할 수 있어요", "~에 도움이 될 수 있어요"처럼 가능성으로 말한다.
- 쓰는 숫자는 입력에 있는 값을 그대로 옮긴다(예: 4.1%, 16–19시).
- 모든 추천에는 그 근거가 된 입력의 evidence_id를 1개 이상 단다. 입력에 없는 id는 쓰지 않는다.

[문장]
- title: 24자 이내, 사장님이 바로 할 수 있는 행동. "~확인", "~점검", "~나누기", "~정하기"처럼 끝낸다.
- expected_effect: 60자 이내, 해요체 한 문장. 그 일을 하면 무엇에 대비할 수 있는지 쓴다.
- 전문 용어와 영어를 쓰지 않는다. 디지털 기기에 익숙하지 않은 사장님도 바로 이해할 수 있게 쓴다.

[출력]
아래 JSON만 출력한다. 설명이나 마크다운을 붙이지 않는다.
{"suggestions": [{"category": "EVENT|RAW_MATERIAL|SUPPLY", "task_type": string|null, "item_id": number|null, "title": string, "expected_effect": string, "evidence_ids": [string]}]}
EVENT의 item_id는 null이고 task_type은 필수다. 나머지는 item_id가 입력의 item_id 중 하나이고 task_type은 null이다.
```

## 출력 JSON Schema

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["suggestions"],
  "properties": {
    "suggestions": {
      "type": "array",
      "maxItems": 9,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["category", "task_type", "item_id", "title", "expected_effect", "evidence_ids"],
        "properties": {
          "category": { "enum": ["EVENT", "RAW_MATERIAL", "SUPPLY"] },
          "task_type": { "enum": ["STAFFING", "HOURS", "PICKUP", "PACKING", "STOCK", "RESERVATION", null] },
          "item_id": { "type": ["integer", "null"] },
          "title": { "type": "string", "maxLength": 24 },
          "expected_effect": { "type": "string", "maxLength": 60 },
          "evidence_ids": { "type": "array", "minItems": 1, "items": { "type": "string" } }
        }
      }
    }
  }
}
```

묶음별 개수(최대 3), id가 입력에 있는지, EVENT의 task_type이 근거 행사의 task_types에 있는지는 스키마로 표현할 수 없어서 Pydantic 검증에서 확인합니다.

## 예시 (UI 시안의 성수동 한식당)

few-shot으로 넣거나 테스트 기대값으로 씁니다. 입력은 계약 초안의 요청 예시에 돼지고기(`R2`, `item_id` 15, 2,650원, 1.2% 하락)를 더한 것입니다.

```json
{
  "suggestions": [
    { "category": "EVENT", "task_type": "STAFFING", "item_id": null, "title": "공연 시간대 근무자 역할 나누기", "expected_effect": "주문·수령 안내 혼선을 줄이는 데 도움이 될 수 있어요.", "evidence_ids": ["E1"] },
    { "category": "EVENT", "task_type": "STOCK", "item_id": null, "title": "식재료 재고 확인", "expected_effect": "품절 위험에 미리 대비할 수 있어요.", "evidence_ids": ["E1"] },
    { "category": "EVENT", "task_type": "PACKING", "item_id": null, "title": "토요일 15–20시 포장 동선 정하기", "expected_effect": "붐비는 시간대 대기 줄을 줄이는 데 도움이 될 수 있어요.", "evidence_ids": ["E1"] },
    { "category": "RAW_MATERIAL", "task_type": null, "item_id": 12, "title": "배추 거래처 단가 확인", "expected_effect": "다음 발주 전에 단가 변동을 미리 확인할 수 있어요.", "evidence_ids": ["R1"] },
    { "category": "RAW_MATERIAL", "task_type": null, "item_id": 15, "title": "돼지고기 발주량 점검", "expected_effect": "가격이 내린 시점의 발주 계획을 살펴볼 수 있어요.", "evidence_ids": ["R2"] },
    { "category": "SUPPLY", "task_type": null, "item_id": 3, "title": "포장 용기 재고 확인", "expected_effect": "행사 전 포장 주문 증가에 대비할 수 있어요.", "evidence_ids": ["S1", "E1"] },
    { "category": "SUPPLY", "task_type": null, "item_id": 3, "title": "포장 용기 추가 주문 시점 확인", "expected_effect": "행사 3일 전까지 받을 수 있는지 미리 확인할 수 있어요.", "evidence_ids": ["S1"] }
  ]
}
```

쌀은 4주 내내 가격이 같아서(`FLAT`) 할 일을 만들지 않았습니다.

## 점검할 경우

본선 당일 테스트 입력으로 만들어 둘 경우입니다.

| 경우 | 기대 결과 |
| --- | --- |
| 행사 0건 | `EVENT` 추천 없음, 나머지는 정상 |
| 품목을 고르지 않음 | `RAW_MATERIAL`·`SUPPLY` 추천 없음 |
| 행사 0건 + 품목 없음 | `suggestions`가 빈 배열(실패가 아님. 화면 빈 상태 문구는 프론트와 합의 필요) |
| 가격 자료 없는 업종(미용실 등) | 가격 숫자가 나오지 않음 |
| 운영 형태 `STORE` | 배달·포장 관련 할 일 없음 |
| `excluded_titles`에 "돼지고기 발주량 점검" | 같은 제목이 다시 나오지 않음 |
| 입력에 없는 숫자 | 출력의 모든 숫자가 입력에 있음(테스트로 자동 확인) |
