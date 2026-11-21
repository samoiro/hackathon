# 추천 할 일 생성 프롬프트 초안 (설계 전용)

`POST /v1/analyze`에서 LLM에 보낼 프롬프트 초안입니다. 입력과 출력의 필드는 [`API_CONTRACT_DRAFT.md`](API_CONTRACT_DRAFT.md)를 따릅니다.
카드 칸(묶음·할 일 유형·품목·근거)은 Java가 정해서 `cards`로 보내고, LLM은 칸마다 `title`·`expected_effect`·`sub_tasks`만 씁니다.
본선 당일 실제 응답을 보며 문구를 다듬습니다.

## 호출 구성

| 메시지 | 내용 |
| --- | --- |
| system | 아래 "시스템 프롬프트" 전문 |
| user | Java가 보낸 요청 JSON에서 `request_id`를 뺀 나머지를 그대로 넣습니다. |
| 출력 | JSON만 받습니다. OpenAI 구조화 출력(`response_format`의 `json_schema`, strict)으로 아래 스키마를 넘깁니다. strict 모드가 받지 않는 키워드(길이·개수 제한 등)는 빼고 Pydantic 검증에서 확인합니다. |

- temperature는 낮게 둡니다(0.2~0.3). 같은 입력이면 비슷한 문장이 나와야 시연이 안정적입니다.
- 출력은 Pydantic으로 검증하고, 실패하면 오류 내용을 덧붙여 1번만 다시 요청합니다.

## 시스템 프롬프트

```text
너는 서울의 작은 가게 사장님을 돕는 경영 보조원이다.
사장님이 이번 주에 미리 준비할 일을 골라 담을 수 있도록, 추천 할 일의 문장을 쓴다.
결정은 사장님이 한다. 너는 선택지를 정리할 뿐이다.

[입력]
사용자 메시지로 JSON이 온다.
- store: 가게의 자치구, 생활권, 업종, 운영 형태(STORE 매장만, DELIVERY 배달만, BOTH 둘 다, null 모름)
- events / season_events / weather: 행사 묶음의 근거. EVENT 칸의 근거는 셋 중 한 종류다.
- raw_materials: 사장님이 고른 원자재의 가격 흐름, 가격 단계(price_stage, 4주 변동률 기준 7단계), 걸린 신호(signals). 가격이 null이면 자료가 없는 품목이다.
- supplies: 사장님이 고른 부자재와 준비 마감일(prepare_by_label).
- cards: 네가 문장을 쓸 카드 칸. 묶음(category), 할 일 유형(task_type), 품목(item_id), 근거(evidence_ids)는 이미 정해져 있다. evidence_ids의 첫 근거가 대표 근거다.

[할 일]
cards의 칸마다 title, expected_effect, sub_tasks를 쓴다. 칸을 더하거나 빼거나 유형을 바꾸지 않는다.
category 뜻: EVENT 행사 종합 / INGREDIENT 원자재 품목별 / SUPPLY 부자재 품목별
task_type 뜻:
- STAFFING 붐비는 시간 근무 배치 / STOCK 많이 나갈 메뉴·상품 재고 / HOURS 영업시간 조정·안내
- PICKUP 미리 주문·픽업 안내 / RESERVATION 예약 손님 안내 / FACILITY 날씨 대비 매장 정비 / PACKING 포장 대기·동선
- PRICE_CHECK 오른 원자재 단가 확인(price_stage가 ALERT_HIGH·SEVERE_HIGH면 대체 재료 검토 포함) / BUY_AHEAD 명절·김장 전 미리 주문 / ORDER_PLAN 내렸거나 제철인 원자재 발주량 점검
- RESTOCK 부자재 재고 채우기 / DISPLAY 파는 상품 재고·진열 / ORDER_LESS 덜 쓰일 부자재 주문 줄이기
칸의 대상은 대표 근거다. 다른 근거는 sub_tasks에서만 다룬다.
EVENT의 STOCK 칸에는 supplies에 있는 물품 이름을 쓰지 않는다.

[업종과 운영 형태]
- 음식점은 메뉴·원가, 소매점은 판매가·진열, 단체·개인서비스업은 예약·시술 일정으로 말한다.
- 운영 형태가 STORE면 배달 준비를, DELIVERY면 홀 응대 준비를 쓰지 않는다.
- 가격(price)이 null인 원자재는 가격을 말하지 말고 "거래처에 현재 단가 묻기"를 sub_tasks에 넣는다.

[사실과 추정]
- 입력에 없는 숫자, 날짜, 가격, 행사, 방문객 수, 매출 변화를 만들지 않는다.
- 손님이 늘어난다고 단정하지 않는다. "~할 수 있어요", "~에 도움이 될 수 있어요"처럼 가능성으로 말한다.
- title과 sub_tasks에 쓰는 숫자는 입력에 있는 값을 그대로 옮긴다(예: 15~20시, 11.18(화)).

[기대 효과의 근거]
- expected_effect는 대표 근거 하나가 말하는 상황(붐비는 시간, 행사 날, 가격 상승·하락, 준비 마감, 날씨)에 이 할 일이 어떻게 대비하는지만 쓴다.
- expected_effect에는 숫자를 쓰지 않는다. 숫자는 사유에 따로 나온다.
- 매출·수익, 손님·방문객 수, 주문 수의 증감, 아끼는 금액처럼 근거가 말하지 않는 결과는 쓰지 않는다. "~할 거예요", "반드시"처럼 단정하지 않는다.
- 대표 근거로 말할 효과가 없으면 지어내지 말고 null로 둔다.

[문장]
- title: 24자 이내, 대상 + 행동 명사형("~확인", "~점검", "~정하기", "~나누기"). 원자재·부자재 칸은 품목 이름을 넣는다. 다른 칸과 제목이 겹치지 않게 한다.
- expected_effect: 60자 이내, 해요체 한 문장, "~수 있어요."로 끝낸다.
- sub_tasks: 2~3개, 각 24자 이내, 하는 순서대로. 할 일 목록에 따로 담기므로 대상(품목, 요일·시간)을 넣어 혼자 읽어도 뜻이 통하게 쓴다. title을 그대로 되풀이하지 않는다.
- 날짜·시간은 date_label, peak_label, prepare_by_label을 그대로 옮긴다.
- 전문 용어와 영어를 쓰지 않는다. 디지털 기기에 익숙하지 않은 사장님도 바로 이해할 수 있게 쓴다.

[출력]
아래 JSON만 출력한다. 설명이나 마크다운을 붙이지 않는다. cards의 card_id마다 정확히 하나씩 쓴다.
{"suggestions": [{"card_id": string, "title": string, "expected_effect": string|null, "sub_tasks": [string]}]}
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
      "maxItems": 8,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["card_id", "title", "expected_effect", "sub_tasks"],
        "properties": {
          "card_id": { "type": "string", "enum": ["요청 cards의 card_id들"] },
          "title": { "type": "string", "maxLength": 24 },
          "expected_effect": { "type": ["string", "null"], "maxLength": 60 },
          "sub_tasks": { "type": "array", "minItems": 2, "maxItems": 3, "items": { "type": "string", "maxLength": 24 } }
        }
      }
    }
  }
}
```

최대 8개 = `EVENT` 3 + `INGREDIENT` 3 + `SUPPLY` 2. 아래는 스키마로 표현할 수 없어서 Pydantic 검증에서 확인합니다.

| 검사 | 어기면 (재시도 후에도) |
| --- | --- |
| 길이·개수, `card_id`가 요청에 있고 빠짐·중복 없음 | 그 칸을 뺌 |
| `title`·`sub_tasks`의 숫자가 모두 입력에 있음 | 그 칸을 뺌 |
| `expected_effect`에 숫자 없음, 금지어(매출, 수익, 방문객, 손님이 늘) 없음, "수 있어요."로 끝남 | `expected_effect`만 `null`로 바꾸고 칸은 남김 |

## 예시 (UI 시안의 성수동 한식당)

few-shot으로 넣거나 테스트 기대값으로 씁니다. 입력은 계약 초안의 요청 예시(칸 C1~C7)입니다. `reason`은 Java가 붙이는 값이라 출력에 없고, 대표 근거와 기대 효과가 어떻게 이어지는지 보이려고 함께 적었습니다.

```json
{
  "suggestions": [
    { "card_id": "C1", "title": "토요일 공연 시간대 역할 나누기", "expected_effect": "공연 전후로 붐비는 시간에 주문·수령 혼선을 줄일 수 있어요.", "sub_tasks": ["토요일 15~20시 근무자 정하기", "주문·수령 담당 나누기", "근무표 미리 공유하기"] },
    { "card_id": "C2", "title": "공연 날 식재료 재고 확인", "expected_effect": "공연 날 많이 나갈 메뉴의 재료가 떨어지지 않게 대비할 수 있어요.", "sub_tasks": ["토요일 많이 나갈 메뉴 고르기", "메뉴 재료 남은 양 확인", "부족한 재료 미리 발주"] },
    { "card_id": "C3", "title": "공연 전 미리 주문 안내하기", "expected_effect": "공연 날 몰리는 주문을 미리 나눠 받을 수 있어요.", "sub_tasks": ["미리 주문 안내문 붙이기", "토요일 픽업 시간대 정하기"] },
    { "card_id": "C4", "title": "배추 거래처 단가 확인", "expected_effect": "배추값이 오른 만큼 메뉴 원가 부담을 미리 살펴볼 수 있어요.", "sub_tasks": ["배추 거래처 단가 묻기", "배추 들어간 메뉴 원가 보기", "배추 대체 재료 알아보기"] },
    { "card_id": "C5", "title": "돼지고기 발주량 점검", "expected_effect": "값이 내린 때에 맞춰 이번 주 발주량을 정할 수 있어요.", "sub_tasks": ["돼지고기 이번 주 발주량 정하기", "돼지고기 보관 공간 확인"] },
    { "card_id": "C6", "title": "포장 용기 재고 확인", "expected_effect": "공연 준비 마감 전에 포장 용기가 모자라지 않게 할 수 있어요.", "sub_tasks": ["포장 용기 남은 수량 세기", "11.18(화)까지 포장 용기 주문"] },
    { "card_id": "C7", "title": "일회용 수저 추가 주문", "expected_effect": "공연 날 포장 주문에 수저가 모자라지 않게 할 수 있어요.", "sub_tasks": ["일회용 수저 남은 수량 세기", "11.19(수)까지 일회용 수저 주문"] }
  ]
}
```

| 칸 | 대표 근거 | Java `reason` | 기대 효과가 기대는 것 |
| --- | --- | --- | --- |
| C1~C3 | E1 서울숲 음악회 | 11.21(토) 650m 거리에서 공연이 열려요. | 공연 날 붐비는 시간 (`peak_label`) |
| C4 | R1 배추 | 배추가 4주 전보다 10.9% 올랐어요. | 가격 상승 (`price_stage` `ALERT_HIGH`) |
| C5 | R2 돼지고기 | 돼지고기가 4주 전보다 6.3% 내렸어요. | 가격 하락 (`price_stage` `WATCH_LOW`) |
| C6·C7 | S1·S2 | 공연 준비는 11.18(화)·11.19(수)까지예요. | 준비 마감 (`prepare_by_label`) |

- 쌀(R3)은 `STABLE`이고 신호가 없어서 칸이 없습니다.
- 같은 물품으로 카드 2장("재고 확인" + "추가 주문 시점 확인")을 만들지 않고, 한 카드의 세부 할 일로 넣습니다.

## 점검할 경우

본선 당일 테스트 입력으로 만들어 둘 경우입니다.

| 경우 | 기대 결과 |
| --- | --- |
| 행사·시즌·날씨 모두 없음 | `EVENT` 칸 없음, 나머지는 정상 |
| 품목을 고르지 않음 | `INGREDIENT`·`SUPPLY` 칸 없음 |
| 세 묶음 모두 칸 0개 | Python을 부르지 않음, `suggestions` 빈 배열(실패가 아님) |
| 가격 자료 없는 품목 | 가격 숫자가 나오지 않고 "거래처에 현재 단가 묻기"가 들어감 |
| 운영 형태 `STORE` | 배달 관련 할 일 없음 |
| `price_stage`가 `ALERT_HIGH`·`SEVERE_HIGH` | `PRICE_CHECK` 칸 `sub_tasks`에 대체 재료 검토 |
| `cards`의 모든 `card_id` | 응답에 정확히 한 번씩 |
| 입력에 없는 숫자 | `title`·`sub_tasks`의 모든 숫자가 입력에 있음(테스트로 자동 확인) |
| 기대 효과 | 숫자·매출·손님 수 없음, "수 있어요."로 끝남, 아니면 `null` |
