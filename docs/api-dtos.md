# API · DTO 정리

프론트가 부르는 서버 API와 요청·응답 DTO입니다. 코드: `server/src/main/java/com/hackathon/server/{도메인}/dto/` (`industry` · `ingredient` · `supply` · `location` · `analysis` · `area` · `publicdata` · `supportprogram`). `PriceStage`는 `ingredient/entity/`에 있습니다.
아래 DTO 표는 코드의 `@Schema` 설명에서 만들었습니다. DTO를 고치면 이 문서도 함께 고칩니다. 기준일: 2026-11-21.

API 구현(컨트롤러·서비스)은 본선에서 합니다. Python ai-server와 주고받는 DTO는 [`ai-server/docs/API_CONTRACT_DRAFT.md`](../ai-server/docs/API_CONTRACT_DRAFT.md) 계약이 확정되면 따로 만듭니다.

## API 목록

| 화면 | 메서드 · 경로 | 요청 | 응답 | 메모 |
| --- | --- | --- | --- | --- |
| 가게 정보 선택 | `GET /api/industries` | — | `List<IndustryResponse>` | 전체 업종 28개 |
| 가게 정보 선택 | `GET /api/industries/{industryId}/recommended-items` | — | `RecommendedItemsResponse` | 업종 기본 원자재 최대 3 · 부자재 최대 2 ("한 번에 담기") |
| 가게 정보 선택 | `GET /api/ingredients?query=` | — | `List<IngredientResponse>` | 원자재 검색, 최대 20개 |
| 가게 정보 선택 | `GET /api/supplies?query=` | — | `List<SupplyResponse>` | 부자재 검색, 최대 20개 |
| 가게 정보 선택 | `POST /api/locations/validate` | `LocationValidateRequest` | `LocationValidateResponse` | 주소 검색은 프론트(카카오 지도). 쓸 수 없는 주소도 200 + `valid=false`, 요청 형식이 틀리면 400 |
| 분석 중 | `POST /api/analysis` | `AnalysisRequest` | `AnalysisCreatedResponse` (202) | 약 1분 걸려 번호만 바로 돌려줌. 서울 밖 422, 품목 개수 초과 400 |
| 분석 중 · 대시보드 | `GET /api/analysis/{id}` | — | `AnalysisResponse` | 1~2초마다 다시 물음. `SUCCEEDED`면 `briefing`, `FAILED`면 `failure` |
| 대시보드 | `POST /api/analysis/{id}/feedback` | `SuggestionFeedbackRequest` | 204 | 카드 1장에 1표, 다시 보내면 표를 바꿈 |
| 대시보드 | `GET /api/areas` | — | `List<AreaResponse>` | "지금 이 지역은" 장소 목록 |
| 대시보드 | `GET /api/seoul/city-data/{area}` | — | `CityDataResponse` | 지금은 원본 CITYDATA를 그대로 줌. 이 DTO로 바꿀 예정 |
| 대시보드 | `GET /api/support-programs` | — | `SupportProgramListResponse` | 마감 임박 지원사업. 오늘 이후 마감, 임박 순 최대 5개. 원본이 하루 1번 갱신되고 느려서 서버가 매일 6시에 모아 두고 내줌. 한 번도 모으지 못했으면 503 |

- 오류는 모두 Spring `ProblemDetail`(RFC 9457) 형식입니다.
- 할 일(오늘 확인할 일)은 로그인이 없어 브라우저 localStorage `todos:{analysisId}`에 둡니다. 서버 API가 없습니다. 강추 카드의 `subTasks`를 처음 열 때 담습니다(항목 `{ id, text, cardKey, done }`, 직접 추가한 할 일은 `cardKey`가 `null`).
- 강추 할 일도 정리할 수 있습니다. 목록에서 `cardKey`로 묶어 `title`·`reason`·`dueDate`(`null`이면 "이번 주 발주 전")를 보여 주고, 세부 할 일은 "이미 했어요"(`done`), 카드는 "해당 없어요"(피드백 `NOT_RELEVANT` + `excludedKeys`에 키 추가 + 그 카드의 할 일을 뺌, 되돌리기 가능)로 정리합니다. 삭제 버튼은 직접 추가한 할 일에만 둡니다. 같은 분석 안에서 강추를 다시 고르지 않습니다.
- "해당 없어요"를 누른 카드 키는 localStorage에 모아 다음 `POST /api/analysis`의 `excludedKeys`로 보냅니다.
- 카드 키 형식: `{category}:{taskType}:{itemId 또는 -}` (예: `EVENT:STAFFING:-`, `INGREDIENT:PRICE_CHECK:12`). `SuggestionResponse.cardKey`, `SuggestionFeedbackRequest.cardKey`, `AnalysisRequest.excludedKeys`, DB `suggestion_feedback.card_key`가 같은 형식입니다.

### 개발용 원본 중계 API

공공데이터 원본을 그대로 돌려주는 확인용 API입니다. 화면에서는 위 API를 씁니다.
`GET /api/kakao/admin-dong` (`AdminDong`) · `/api/kweather/*` · `/api/seoul/*` · `GET /api/agro/price-trends` · `GET /api/bizinfo/support-programs` (모두 `ListResponse` 또는 원본 JSON)

## 가게 정보 선택

### IndustryResponse

industry 테이블 한 줄.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `id` | `long` | 필수 | 업종 id (예: 9) |
| `category` | `String` | 필수 | 업종 분류 (음식점 · 소매점 · 단체·개인서비스업) (예: 음식점) |
| `name` | `String` | 필수 | 업종 이름 (예: 한식·국밥) |

### RecommendedItemsResponse

GET /api/industries/{industryId}/recommended-items. "많이 고르는 품목 · 한 번에 담기"에 쓴다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `industryId` | `long` | 필수 | 업종 id |
| `ingredients` | `List<IngredientResponse>` | 필수 | 추천 원자재 (최대 3개, 원자재 선택 상한과 같음) |
| `supplies` | `List<SupplyResponse>` | 필수 | 추천 부자재 (최대 2개, 부자재 선택 상한과 같음) |

### IngredientResponse

GET /api/ingredients 한 건 (ingredient 테이블).

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `id` | `long` | 필수 | 원자재 번호 (ingredient.id) (예: 9) |
| `name` | `String` | 필수 | 품목 이름 (ingredient.item_name) (예: 배추) |

### SupplyResponse

supply 테이블 한 줄.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `id` | `long` | 필수 | 부자재 id (예: 3) |
| `name` | `String` | 필수 | 부자재 이름 (예: 포장 용기) |

### LocationValidateRequest

POST /api/locations/validate 요청. 주소 검색은 프론트(카카오 지도 JS)가 하고, 고른 주소만 서버가 확인한다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `roadAddress` | `String` | 필수 | 도로명 주소 (예: 서울 성동구 서울숲2길 32) · 검증: `NotBlank` |
| `latitude` | `Double` | 필수 | 위도 (예: 37.5446) · 검증: `NotNull` |
| `longitude` | `Double` | 필수 | 경도 (예: 127.0446) · 검증: `NotNull` |

### LocationValidateResponse

POST /api/locations/validate 응답 (항상 200). 서울 밖 등 "쓸 수 없는 주소"는 요청 오류가 아니라 확인 결과라서 valid=false로 돌려준다. 빈 값처럼 요청 자체가 잘못되면 400 ProblemDetail.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `valid` | `boolean` | 필수 | 분석할 수 있는 주소인지 |
| `message` | `String` | null 가능 | valid=false일 때 화면에 보여줄 이유. true면 null (예: 서울 밖 주소는 아직 지원하지 않아요.) |

## 분석 요청·상태

### AnalysisRequest

POST /api/analysis 요청. 가게 정보 선택 화면의 입력 전부. 좌표는 /api/locations/validate 결과를 그대로 보내지만, 서버는 서울 여부를 다시 확인한다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `storeName` | `String` | 필수 | 가게 이름 (필수) (예: 성수동 한식당) · 검증: `NotBlank`, `Size(max = 30)` |
| `roadAddress` | `String` | 필수 | 도로명 주소 (필수) (예: 서울 성동구 서울숲2길 32) · 검증: `NotBlank` |
| `latitude` | `Double` | 필수 | 위도 · 검증: `NotNull` |
| `longitude` | `Double` | 필수 | 경도 · 검증: `NotNull` |
| `industryId` | `Long` | 필수 | 업종 id (필수) · 검증: `NotNull` |
| `operationType` | `OperationType` | null 가능 | 운영 형태 (선택). null = 선택 안 함 |
| `ingredientIds` | `List<Long>` | null 가능 | 원자재 id (선택, 최대 3개, 중복 불가) · 검증: `Size(max = 3)` · 항목 검증: `NotNull` |
| `supplyIds` | `List<Long>` | null 가능 | 부자재 id (선택, 최대 2개, 중복 불가) · 검증: `Size(max = 2)` · 항목 검증: `NotNull` |
| `excludedKeys` | `List<String>` | null 가능 | 제외 목록: 사장님이 '해당 없어요'로 표시한 카드 키 {category}:{taskType}:{itemId 또는 -}. 서버가 카드 칸을 고르기 전에 뺀다 (순서도 11). 로그인이 없어 브라우저가 보관해 보낸다 (예: ["EVENT:STAFFING:-"]) · 검증: `Size(max = 50)` · 항목 검증: `NotNull`, `Pattern` (카드 키 형식) |

### AnalysisCreatedResponse

POST /api/analysis 응답 (202 Accepted). 이 id로 GET /api/analysis/{id}를 폴링한다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `analysisId` | `UUID` | 필수 | 분석 id |
| `status` | `AnalysisStatus` | 필수 | 처음엔 PENDING |

### AnalysisResponse

GET /api/analysis/{id}. 분석 중에는 steps만, 끝나면 briefing 또는 failure가 채워진다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `analysisId` | `UUID` | 필수 | 분석 id |
| `status` | `AnalysisStatus` | 필수 | 전체 상태 |
| `baseTime` | `OffsetDateTime` | 필수 | 브리핑 기준 시각 = 리포트 만들기를 누른 시각. 행사·날씨의 향후 7일은 이 시각부터 센다 |
| `store` | `StoreSummary` | 필수 | 입력한 가게 정보. 칩·대시보드 머리말·'입력한 정보는 그대로예요'에 쓴다 |
| `steps` | `List<AnalysisStep>` | 필수 | 4단계 상태 (StepCode 순서로 항상 4개) |
| `eventSearch` | `EventSearch` | null 가능 | 행사 조회 조건과 건수. EVENT_SEARCH가 DONE이면 채움 |
| `failure` | `AnalysisFailure` | null 가능 | 실패 정보. FAILED일 때만 |
| `briefing` | `BriefingResponse` | null 가능 | 브리핑. SUCCEEDED일 때만 |

### AnalysisStep

분석 단계 상태

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `code` | `StepCode` | 필수 | 단계 |
| `status` | `StepStatus` | 필수 | 상태 |

### AnalysisFailure

분석 실패 정보

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `step` | `StepCode` | 필수 | 멈춘 단계 |
| `code` | `String` | 필수 | 실패 코드 (예: PRICE_SOURCE_FAILED) |
| `message` | `String` | 필수 | 단계 아래 문구 (예: 가격 자료를 불러오지 못했어요) |

### EventSearch

행사 트랙 조회 조건과 결과 분기. "향후 7일 · 2건 확인".

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `days` | `int` | 필수 | 조회 기간 (일) (예: 7) |
| `baseDate` | `LocalDate` | 필수 | 기준일 (리포트 만들기 클릭 날짜) |
| `count` | `int` | 필수 | 찾은 주변 행사 수 (예: 2) |
| `source` | `EventSource` | 필수 | 행사 후보를 만든 분기: 주변 행사 → 시즌 행사 → 날씨 → 없음 |

### StoreSummary

분석에 쓴 가게 정보

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `name` | `String` | 필수 | 가게 이름 (예: 성수동 한식당) |
| `roadAddress` | `String` | 필수 | 도로명 주소 |
| `gu` | `String` | 필수 | 자치구 (예: 성동구) |
| `areaLabel` | `String` | 필수 | 생활권 표시 이름 (예: 성수동 · 서울숲 인근) |
| `industry` | `IndustryResponse` | 필수 | 업종 |
| `operationType` | `OperationType` | null 가능 | 운영 형태. null = 선택 안 함 |
| `ingredients` | `List<IngredientResponse>` | 필수 | 고른 원자재 |
| `supplies` | `List<SupplyResponse>` | 필수 | 고른 부자재 |

## 브리핑

### BriefingResponse

통합 대시보드의 브리핑 영역 전부. 지역 현황은 /api/seoul/city-data/{area}로 따로 받는다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `events` | `List<EventResponse>` | 필수 | 주변 행사 (순서도 3·4). 0건이면 시즌 행사·날씨로 넘어감 |
| `seasonEvents` | `List<SeasonEventResponse>` | 필수 | 시즌 행사 (순서도 5). 주변 행사가 없을 때, 또는 부자재 카드의 근거일 때 채움 |
| `weather` | `WeatherResponse` | null 가능 | 날씨 (순서도 3-1). 주변 행사·시즌 행사가 모두 없을 때, 또는 부자재 카드의 근거일 때 채움 |
| `ingredients` | `List<IngredientPriceResponse>` | 필수 | 고른 원자재 전부의 가격 (가격 자료 없는 품목 포함, 순서도 7) |
| `supplies` | `List<SupplyTimingResponse>` | 필수 | 고른 부자재 전부의 준비 시점 (부자재 정보 없음 포함, 순서도 9) |
| `suggestions` | `List<SuggestionResponse>` | 필수 | 추천 카드 (순서도 17~19). 강추가 하나도 없으면 빈 상태 문구 |
| `sources` | `List<SourceResponse>` | 필수 | 근거 (하단 출처·확인 일시) |

### EventResponse

서울시 문화행사(aiViewCulture)는 날짜 단위라 시각은 문구로만 둔다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `evidenceId` | `String` | 필수 | 근거 id (추천 할 일과 연결) (예: E1) |
| `title` | `String` | 필수 | 행사 이름 (예: 서울숲 음악회) |
| `category` | `String` | 필수 | 분류 (필터 칩) (예: 공연) |
| `trigger` | `EventTrigger` | 필수 | 분류가 켜는 트리거 (부자재 준비표 연결) |
| `place` | `String` | 필수 | 장소 (예: 서울숲 야외무대) |
| `startDate` | `LocalDate` | 필수 | 시작일 |
| `endDate` | `LocalDate` | 필수 | 종료일 |
| `timeText` | `String` | null 가능 | 시간 문구. 원본에 없으면 null (예: 16:00–19:00) |
| `status` | `EventStatus` | 필수 | 예정 · 진행 중 |
| `distanceM` | `int` | 필수 | 가게까지 직선거리 (m) (예: 650) |
| `walkMinutes` | `int` | 필수 | 도보 시간 추정 (분) (예: 10) |
| `isFree` | `Boolean` | null 가능 | 무료 여부. 모르면 null |
| `latitude` | `double` | 필수 | 위도 (지도 표시) |
| `longitude` | `double` | 필수 | 경도 (지도 표시) |
| `sourceUrl` | `String` | null 가능 | 원문 링크 |

### SeasonEventResponse

순서도 5: 주변 행사가 없을 때 쓰는 시즌 행사. supply_trigger + supply_trigger_date.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `evidenceId` | `String` | 필수 | 근거 id (예: H1) |
| `code` | `String` | 필수 | 트리거 코드 (예: CHRISTMAS) |
| `label` | `String` | 필수 | 이름 (예: 크리스마스) |
| `type` | `TriggerType` | 필수 | 종류 (HOLIDAY · SCHOOL · SEASON) |
| `startDate` | `LocalDate` | 필수 | 시작일 |
| `endDate` | `LocalDate` | 필수 | 종료일 (하루짜리면 시작일과 같음) |

### WeatherResponse

순서도 3-1: 매장 주소(행정동) 기준 향후 7일 날씨. 케이웨더 기상특보 + 단기예보 + 주간예보.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `evidenceId` | `String` | 필수 | 근거 id (예: W1) |
| `regionCode` | `String` | 필수 | 행정동 코드 (10자리) (예: 1120011400) |
| `impacts` | `List<WeatherImpact>` | 필수 | 영업에 영향 주는 날씨. 비어 있으면 날씨 후보 없음 |
| `warnings` | `List<WeatherWarning>` | 필수 | 발효 중인 기상특보 |
| `forecasts` | `List<WeatherForecast>` | 필수 | 향후 7일 예보, 시각 순 (0~2일 후는 단기예보 kw-fct-dl, 3일 후부터는 주간예보 kw-fct-wl) |
| `checkedAt` | `OffsetDateTime` | 필수 | 케이웨더 확인 시각 |

### WeatherWarning

케이웨더 kw-warning 한 건.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `kind` | `String` | 필수 | 특보 종류 (예: 한파) |
| `level` | `String` | 필수 | 강도 (예: 주의보) |
| `startTime` | `OffsetDateTime` | 필수 | 발효 시각 |
| `endTime` | `OffsetDateTime` | null 가능 | 해제 시각. 해제 전이면 null |

### WeatherForecast

케이웨더 단기예보(kw-fct-dl)·주간예보(kw-fct-wl) 한 시점.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `date` | `LocalDate` | 필수 | 날짜 |
| `dayPart` | `DayPart` | 필수 | 오전 · 오후 · 하루 |
| `sky` | `String` | 필수 | 날씨 문구 (예: 흐리고 비) |
| `minTemperature` | `Double` | null 가능 | 최저 기온 (℃) (예: -3.0) |
| `maxTemperature` | `Double` | null 가능 | 최고 기온 (℃) (예: 5.0) |
| `precipitationChance` | `Integer` | null 가능 | 강수 확률 (%) (예: 60) |

### IngredientPriceResponse

관심 품목 가격 (원자재). 가격 자료가 없으면 price부터 priceStage까지 모두 null.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `evidenceId` | `String` | 필수 | 근거 id (예: R1) |
| `itemId` | `long` | 필수 | 원자재 id |
| `name` | `String` | 필수 | 품목 이름 (예: 배추) |
| `grade` | `String` | null 가능 | 등급 (예: 상품) |
| `unit` | `String` | null 가능 | 단위 (예: 1포기) |
| `baseDate` | `LocalDate` | null 가능 | 조사 기준일 |
| `price` | `Long` | null 가능 | 기준일 가격 (원) (예: 4380) |
| `weeklyPrices` | `List<Long>` | null 가능 | 4주 전 → 기준일 순서 5개. 빈 주는 null |
| `change1w` | `PriceChange` | null 가능 | 1주 전 대비. 가격 흐름(trend) 기준 |
| `change4w` | `PriceChange` | null 가능 | 4주 전 대비. 가격 단계 기준이고 화면 설명에도 씀 ('4주 전보다 430원 올랐어요') |
| `trend` | `Trend` | null 가능 | 가격 흐름 |
| `consecutiveWeeks` | `Integer` | null 가능 | 같은 방향으로 이어진 주 수 (4주 연속 상승). FLAT이면 0 |
| `priceStage` | `PriceStage` | null 가능 | 가격 단계: 4주 변동률을 부류별 분위수로 나눈 7단계 (수급조절매뉴얼 방식). 강추 점수의 원자재 가격 신호. 기준: docs/database-schema.md ingredient_price_band |

### PriceChange

"172원 (4.1%)". 하락이면 둘 다 음수.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `amount` | `long` | 필수 | 변동 금액 (원) (예: 172) |
| `percent` | `double` | 필수 | 변동률 (%), 소수 첫째 자리 (예: 4.1) |

### SupplyTimingResponse

부자재 준비 시점 (supply_rule 기준). 이번 주 걸린 규칙이 없으면(순서도 "부자재 정보 없음") itemId·name만 채운다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `evidenceId` | `String` | null 가능 | 근거 id. 해당 없으면 null (예: S1) |
| `itemId` | `long` | 필수 | 부자재 id |
| `name` | `String` | 필수 | 품목 이름 (예: 포장 용기) |
| `demand` | `Demand` | null 가능 | 수요 변화 (수요 높음 배지) |
| `prepareBy` | `LocalDate` | null 가능 | 이날까지 준비 (11.18(수)) |
| `leadDays` | `Integer` | null 가능 | 며칠 전 준비 (supply_rule.lead_days_max) (예: 3) |
| `triggerLabel` | `String` | null 가능 | 준비 이유 (예: 공연) |
| `triggerEvidenceId` | `String` | null 가능 | 연결된 행사·시즌·날씨 근거 id. 없으면 null (예: E1) |

### SuggestionResponse

추천 카드 (순서도 18~19). Java가 칸·사유·준비 기한·강추를 정하고, Python(LLM)이 제목·기대 효과·세부 할 일을 쓴다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `id` | `long` | 필수 | 추천 id |
| `cardKey` | `String` | 필수 | 카드 키 {category}:{taskType}:{itemId 또는 -}. 피드백·해당 없어요 제외에 씀 (예: INGREDIENT:PRICE_CHECK:12) |
| `category` | `SuggestionCategory` | 필수 | 유형 라벨 |
| `taskType` | `TaskType` | 필수 | 할 일 유형. 세 묶음 모두 필수이고 묶음마다 쓸 수 있는 유형이 정해져 있다 |
| `itemId` | `Long` | null 가능 | 원자재·부자재 id. EVENT는 null |
| `title` | `String` | 필수 | 할 일 제목 (LLM, 24자 이내) (예: 배추 거래처 단가 확인) |
| `reason` | `String` | 필수 | 한 줄 사유: 왜 추천했는지 정보형으로. 서버가 대표 근거(evidenceIds 첫 번째)의 숫자로 템플릿을 채운다 (순서도 19) (예: 배추가 4주 전보다 10.9% 올랐어요.) |
| `dueDate` | `LocalDate` | null 가능 | 준비 기한: 이날까지 하면 좋은 날. 강추 '급한 정도'의 행동 마감과 같다. EVENT는 대표 근거의 시작일(행사·시즌 시작일, 날씨 영향 첫날, 진행 중이면 기준일), SUPPLY는 prepareBy, INGREDIENT는 null('이번 주 발주 전'으로 표시) |
| `expectedEffect` | `String` | null 가능 | 기대 효과 (LLM, 60자 이내, '~수 있어요.'). reason과 같은 대표 근거에서 나온 효과만, 숫자·매출·손님 수 없이 쓴다. 근거로 말할 효과가 없으면 null이고 화면은 그 줄을 숨긴다 (예: 배추값이 오른 만큼 메뉴 원가 부담을 미리 살펴볼 수 있어요.) |
| `subTasks` | `List<String>` | 필수 | 세부 할 일 (LLM, 2~3개). 강추면 내 준비할 일에 자동 추가하고, 이미 했어요(완료)·해당 없어요(할 일에서 뺌)로 정리한다 (순서도 20) |
| `recommended` | `boolean` | 필수 | 강추 여부. 모든 추천 중 근거 점수 상위 3개 (같은 유형 최대 2개). 강추도 해당 없어요로 뺄 수 있다. 기준: ai-server/docs/API_CONTRACT_DRAFT.md 강추 고르기 |
| `evidenceIds` | `List<String>` | 필수 | 근거 id (E·H·W·R·S + 번호). 첫 번째가 대표 근거로, 카드를 그 행사·품목과 묶어 보여 준다 |

### SourceResponse

근거 자료

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `name` | `String` | 필수 | 자료 이름 (예: 서울시 문화행사 정보) |
| `provider` | `String` | 필수 | 제공 기관 (예: 서울 열린데이터광장) |
| `updateCycle` | `String` | 필수 | 갱신 주기 (예: 매일 갱신) |
| `baseDate` | `LocalDate` | null 가능 | 자료 기준일 (가격처럼 기준일이 있는 자료만) |
| `checkedAt` | `OffsetDateTime` | 필수 | 확인 시각 |
| `url` | `String` | null 가능 | 원문 링크 |

## 피드백

### SuggestionFeedbackRequest

POST /api/analysis/{id}/feedback 요청 → 204. 분석 1건의 카드 1장에 1표, 다시 보내면 표를 바꾼다. 감사 토스트는 프론트가 띄운다. 해당 없어요는 강추에도 쓰며, 프론트가 그 카드 키를 excludedKeys에 더하고 할 일에서 뺀다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `cardKey` | `String` | 필수 | 카드 키 {category}:{taskType}:{itemId 또는 -} (예: INGREDIENT:PRICE_CHECK:12) · 검증: `NotNull`, `Pattern` (카드 키 형식) |
| `vote` | `FeedbackVote` | 필수 | 도움돼요 · 해당 없어요 · 검증: `NotNull` |

## 지역 현황

### AreaResponse

GET /api/areas 한 건. city_data_area 테이블 (서울시 주요 장소, 데이터셋 OA-21285).

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `code` | `String` | 필수 | 장소 코드 (예: POI009) |
| `name` | `String` | 필수 | 장소 이름. /api/seoul/city-data/{area}에 그대로 넘긴다 (예: 광화문·덕수궁) |
| `category` | `String` | 필수 | 분류 (예: 고궁·문화유산) |
| `latitude` | `double` | 필수 | 영역 중심 위도 (boundary의 ST_Centroid) |
| `longitude` | `double` | 필수 | 영역 중심 경도 (boundary의 ST_Centroid) |

### CityDataResponse

GET /api/seoul/city-data/{area}를 화면용으로 줄인 응답 ("지금 이 지역은 어떤가요?"). 원본 CITYDATA는 응답이 커서 그대로 보내지 않는다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `areaCode` | `String` | 필수 | 장소 코드 (AREA_CD) (예: POI072) |
| `areaName` | `String` | 필수 | 장소 이름 (AREA_NM) (예: 서울숲공원) |
| `observedAt` | `OffsetDateTime` | 필수 | 인구 집계 시각 (PPLTN_TIME). '11.17 07:00 기준' |
| `congestion` | `Congestion` | 필수 | 현재 혼잡도 (AREA_CONGEST_LVL) |
| `populationMin` | `int` | 필수 | 현재 인구 최소 (AREA_PPLTN_MIN) (예: 28000) |
| `populationMax` | `int` | 필수 | 현재 인구 최대 (AREA_PPLTN_MAX) (예: 30000) |
| `forecasts` | `List<CityPopulationForecast>` | 필수 | 이후 예상 혼잡도 (FCST_PPLTN, 12시간 뒤까지 시각 순) |
| `ageRatios` | `List<CityAgeRatio>` | 필수 | 연령대별 인구 비율 (PPLTN_RATE_0~70) |
| `weather` | `CityWeather` | null 가능 | 날씨 (WEATHER_STTS). 자료가 없으면 null |

### CityPopulationForecast

예상 혼잡도

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `time` | `OffsetDateTime` | 필수 | 예측 시각 (FCST_TIME) |
| `congestion` | `Congestion` | 필수 | 예상 혼잡도 (FCST_CONGEST_LVL) |
| `populationMin` | `int` | 필수 | 예상 인구 최소 (FCST_PPLTN_MIN) (예: 32000) |
| `populationMax` | `int` | 필수 | 예상 인구 최대 (FCST_PPLTN_MAX) (예: 34000) |

### CityAgeRatio

연령대별 인구 비율

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `ageGroup` | `int` | 필수 | 연령대 (0 = 10세 미만, 10 = 10대 … 70 = 70대 이상) (예: 20) |
| `percent` | `double` | 필수 | 비율 (%) (예: 31.0) |

### CityWeather

"맑음 · 8°C · 강수 확률 10% · 미세먼지 보통".

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `sky` | `String` | null 가능 | 하늘 상태 (SKY_STTS) (예: 맑음) |
| `temperature` | `Double` | null 가능 | 기온 (TEMP, ℃) (예: 8.0) |
| `precipitationChance` | `Integer` | null 가능 | 강수 확률 (가장 가까운 시각의 RAIN_CHANCE, %) (예: 10) |
| `pm10Index` | `String` | null 가능 | 미세먼지 등급 (PM10_INDEX). FINE_DUST 판정에 씀 (예: 보통) |
| `pm25Index` | `String` | null 가능 | 초미세먼지 등급 (PM25_INDEX) (예: 좋음) |

## 지원사업

### SupportProgramListResponse

GET /api/support-programs. 대시보드 "마감 임박 지원사업" 카드. 기업마당 지원사업 공고(공공데이터포털 15157820) 중 서울·전국 소상공인 대상이고 오늘 이후에 마감되는 공고. 원본은 하루 한 번 갱신되고 한 번 받는 데 몇 초 걸려서 서버가 모아 두고 내준다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `items` | `List<SupportProgramResponse>` | 필수 | 공고 (마감일 빠른 순, 최대 5개). '예산 소진시까지'처럼 마감일이 없는 공고는 뺀다 |
| `checkedAt` | `OffsetDateTime` | 필수 | 원본을 확인한 시각 (출처 표시) |

### SupportProgramResponse

기업마당 공고 한 건. 출처표시·변경금지(공공누리 제3유형)라 제목과 기관명은 원문 그대로 둔다.

| 필드 | 타입 | 값 | 설명 |
| --- | --- | --- | --- |
| `id` | `String` | 필수 | 공고 id (pblancId) (예: PBLN_000000000128062) |
| `title` | `String` | 필수 | 공고명 (pblancNm) (예: 2026년 소상공인 법률ㆍ노무 현장 상담 지원사업 모집 공고) |
| `url` | `String` | 필수 | 기업마당 원문 링크 (pblancUrl) |
| `agency` | `String` | 필수 | 소관기관 (jrsdInsttNm) (예: 중소벤처기업부) |
| `field` | `String` | null 가능 | 지원 분야 (pldirSportRealmLclasCodeNm). 모르면 null (예: 경영) |
| `deadline` | `LocalDate` | 필수 | 마감일 (오늘이거나 그 뒤) (예: 2026-11-25) |
| `applicationPeriod` | `String` | 필수 | 신청 기간 원문 (reqstBeginEndDe) (예: 2026-11-19 ~ 2026-11-25) |

## 값 목록 (enum)

| 이름 | 뜻 | 값 |
| --- | --- | --- |
| `AnalysisStatus` | 분석 전체 상태. SUCCEEDED·FAILED가 되면 폴링을 멈춘다. | `PENDING` · `RUNNING` · `SUCCEEDED` · `FAILED` |
| `StepCode` | 분석 중 화면 4단계: 가게 정보 확인 · 주변 행사 조회 · 품목 가격 조회 · 준비할 일 정리. EVENT_SEARCH는 순서도의 행사 트랙 전체(3 행사 → 5 시즌 행사 → 3-1 날씨)를 맡고, 어디서 멈췄는지는 EventSearch.source로 알린다. | `STORE_CHECK` · `EVENT_SEARCH` · `PRICE_LOOKUP` · `TASK_GENERATION` |
| `StepStatus` | 단계 배지: 대기 · 확인 중 · 완료 · 실패. | `WAITING` · `RUNNING` · `DONE` · `FAILED` |
| `EventSource` | 행사 트랙이 어느 분기에서 후보를 만들었는지 (순서도 3 → 5 → 3-1). | `CULTURAL_EVENT` 3·4: 주변 문화행사가 있음 · `SEASON` 5: 행사가 없어서 시즌 행사(설·추석·크리스마스 등)로 대신함 · `WEATHER` 3-1: 시즌 행사도 없어서 영업에 영향 주는 날씨로 대신함 · `NONE` 셋 다 없음: "조건에 맞는 예정 행사가 없어요" |
| `EventStatus` | 행사 배지: 예정 · 진행 중. | `UPCOMING` · `ONGOING` |
| `EventTrigger` | 행사 분류(cultural_event.category)가 켜는 부자재 준비표 트리거: 축제 · 공연. 분류 매핑: ai-server/docs/API_CONTRACT_DRAFT.md "행사 후보와 할 일 유형" | `FESTIVAL` · `PERFORMANCE` |
| `OperationType` | 운영 형태: 매장만 · 배달만 · 매장·배달 모두. | `STORE` · `DELIVERY` · `BOTH` |
| `SuggestionCategory` | 추천 할 일 묶음: 행사 · 원자재 · 부자재. | `EVENT` · `INGREDIENT` · `SUPPLY` |
| `TaskType` | 추천 할 일 유형. 묶음마다 쓸 수 있는 유형이 정해져 있다 (행사 7개 · 원자재 3개 · 부자재 3개). 카드 키 {category}:{taskType}:{itemId 또는 -}의 가운데 값. 기준: docs/api-dtos.md | `STAFFING` 행사 · 근무 배치 · `STOCK` 행사 · 재고 · `HOURS` 행사 · 영업시간 · `PICKUP` 행사 · 주문·픽업 안내 · `RESERVATION` 행사 · 예약 안내 (서비스업) · `FACILITY` 행사 · 매장 정비 · `PACKING` 행사 · 포장 동선 · `PRICE_CHECK` 원자재 · 단가 확인 · `BUY_AHEAD` 원자재 · 미리 주문 · `ORDER_PLAN` 원자재 · 발주량 점검 · `RESTOCK` 부자재 · 재고 채우기 · `DISPLAY` 부자재 · 판매 상품 진열 · `ORDER_LESS` 부자재 · 주문 줄이기 |
| `FeedbackVote` | 추천 카드 피드백: 도움돼요 · 해당 없어요. | `HELPFUL` · `NOT_RELEVANT` |
| `Trend` | 가격 흐름: 상승 · 하락 · 변동 없음 (change1w 기준, ±1% 미만이면 FLAT. 강추 점수가 같으면 change1w.percent 절대값이 큰 품목 먼저). | `UP` · `DOWN` · `FLAT` |
| `PriceStage` | 가격 단계. 정부 농산물 수급조절매뉴얼의 7단계(상승 심각·경계·주의 / 안정 / 하락 주의·경계·심각)를 따른다. 경계값은 ingredient_price_band의 부류별 4주 변동률 분위수. 기준: docs/database-schema.md ingredient_price_band | `SEVERE_HIGH` 상승 심각: 상위 5%, 규칙 PRICE_RISE_FOUR_WEEKS_SEVERE · `ALERT_HIGH` 상승 경계: 상위 12.5%, 규칙 PRICE_RISE_FOUR_WEEKS_ALERT · `WATCH_HIGH` 상승 주의: 상위 20%, 규칙 PRICE_RISE_FOUR_WEEKS_WATCH · `STABLE` 안정: 가격 규칙 없음 · `WATCH_LOW` 하락 주의: 하위 20%, 규칙 PRICE_DROP_FOUR_WEEKS_WATCH · `ALERT_LOW` 하락 경계: 하위 12.5%, 규칙 PRICE_DROP_FOUR_WEEKS_ALERT · `SEVERE_LOW` 하락 심각: 하위 5%, 규칙 PRICE_DROP_FOUR_WEEKS_SEVERE |
| `WeatherImpact` | 영업에 영향 주는 날씨 (순서도 3-1 판정 기준). HEAVY_SNOW는 대설 특보, FINE_DUST는 도시데이터 미세먼지 "나쁨" 이상. | `RAIN_SNOW` · `HEAVY_SNOW` · `COLD_WAVE` · `HEATWAVE` · `STRONG_WIND` · `FINE_DUST` · `WARNING` |
| `DayPart` | 예보 시점: 단기예보와 3~7일 후 주간예보는 오전·오후, 8~10일 후는 하루 단위. | `AM` · `PM` · `ALL_DAY` |
| `Demand` | supply_rule.demand. 값은 DB check 제약과 같아야 한다. | `UP` · `STRONG_UP` · `DOWN` |
| `TriggerType` | supply_trigger.type. 값은 DB check 제약과 같아야 한다. | `WEATHER` · `EVENT` · `HOLIDAY` · `SCHOOL` · `SEASON` |
| `Congestion` | 서울시 혼잡도 (AREA_CONGEST_LVL, FCST_CONGEST_LVL): 여유 · 보통 · 약간 붐빔 · 붐빔. | `RELAXED` · `NORMAL` · `SLIGHTLY_CROWDED` · `CROWDED` |
