# 데이터베이스 테이블 구조

기준일: 2026-11-21. 연결된 PostgreSQL DB의 실제 컬럼·타입·제약조건을 조회해 반영했습니다.
V24(강추 순위용 규칙 정리, 분석 작업 테이블)는 Testcontainers로 V1부터 적용해 검증했고 로컬·Neon DB에는 아직 적용하지 않았습니다. V25(추천 피드백)까지 로컬 DB에 적용했고 업무용 테이블은 16개, 관리용은 4개입니다. 업종별 추천 연결 V21과 이름 컬럼 V23은 로컬에만 적용했으며 Neon에는 아직 적용하지 않았습니다. 아래 미생성 설계안은 실제 DB에는 아직 없습니다.

표에서 별도 표시가 없는 현재 컬럼은 NOT NULL입니다. PK는 기본 키, FK는 외래 키, UNIQUE는 중복 불가를 뜻합니다.
현재 업무용 컬럼명은 단어를 풀어 쓰며, 외부 API 원본 필드명과 확장·관리 도구의 필드명은 그대로 둡니다.

## 현재 생성된 업무 테이블

### industry — 업종

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| id | bigint PK, 자동 생성 | 업종 ID |
| category | text | 상위 분류: 음식점 / 소매점 / 단체·개인서비스업 |
| name | text UNIQUE | 업종 이름 |

### supply — 부자재·소모품 및 판매 상품

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| id | bigint PK, 자동 생성 | 물품 ID |
| name | text UNIQUE | 물품 이름 |

부자재와 판매 상품은 규칙 연결 시 supply_rule_item.kind로 구분합니다.

### supply_trigger — 준비를 유발하는 상황

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| code | text PK | 상황 코드 |
| label | text | 표시 이름 |
| type | text | WEATHER / EVENT / HOLIDAY / SCHOOL / SEASON |

V24에서 CROWD_SURGE · SPORTS · TRAFFIC_CONTROL을 지워 35개에서 32개가 됩니다. EVENT는 FESTIVAL · PERFORMANCE만 남고, 행사 분류에서 정합니다(ai-server/docs/API_CONTRACT_DRAFT.md "행사 후보와 할 일 유형").

### supply_rule — 부자재 준비 규칙

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| id | bigint PK, 자동 생성 | 규칙 ID |
| industry_id | bigint FK → industry.id | 적용 업종 |
| trigger_code | text FK → supply_trigger.code | 준비를 유발하는 상황 |
| trigger_text | text | 원본 상황 설명 |
| forecast | boolean | true: 날씨 예보 기준, false: 날짜 기준 |
| lead_days_min | integer, NULL 허용 | 최소 준비 일수. D-1~3이면 1 |
| lead_days_max | integer, NULL 허용 | 최대 준비 일수. D-1~3이면 3 |
| demand | text | UP / STRONG_UP / DOWN |

- (industry_id, trigger_code)는 UNIQUE입니다.
- 준비 일수는 둘 다 NULL이거나 0 ≤ lead_days_min ≤ lead_days_max여야 합니다. 기존 적재 기준으로 수요 감소 규칙은 둘 다 NULL입니다.
- 이전 설명의 lead_time, lead_days 컬럼은 현재 존재하지 않습니다.

### supply_rule_item — 규칙별 물품

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| rule_id | bigint FK → supply_rule.id, 복합 PK | 준비 규칙 |
| supply_id | bigint FK → supply.id, 복합 PK | 해당 물품 |
| kind | text | SUPPLY: 부자재·소모품, STOCK: 손님에게 판매하는 상품 |

기본 키는 (rule_id, supply_id)입니다. 규칙 삭제 시 연결 행도 삭제됩니다.

- V24에서 sort_order를 지웠습니다. 노션 표 순서일 뿐 읽는 곳이 없었고, 0부터 시작해 7개 행에서 번호가 겹쳤습니다. 순위는 서버가 근거 점수로 정합니다.
- V24에서 켤 데이터가 없는 규칙 7개를 지웠습니다. 혼잡도 급증(CROWD_SURGE) 2개는 혼잡도를 강추 계산에 쓰지 않기로 해서이고 같은 노션 행의 축제·공연 규칙에 물품이 똑같이 있습니다. 스포츠 중계(SPORTS) 3개와 교통통제·마라톤(TRAFFIC_CONTROL) 2개는 일정을 주는 API가 없고 날짜를 직접 관리하지 않기로 했습니다.
- V24에서 규칙이 2개뿐이던 업종에 3개를 더했습니다: 분식·김밥 개학, 해물·조개구이 연말 송년회, 수선집 졸업. 부자재 규칙은 130개에서 126개가 됩니다.

### ingredient — 품목별 대표 원자재

| 컬럼 | 타입·제약 | 설명 | 원본 API 필드 |
| --- | --- | --- | --- |
| id | bigint PK, 자동 생성 | 원자재 ID | — |
| item_code | text UNIQUE | 품목 코드 | item_cd |
| item_name | text UNIQUE | 품목 이름 | item_nm |
| category_code | text | 부류 코드 | ctgry_cd |
| category_name | text | 부류 이름 | ctgry_nm |
| trade_type_code | text | 대표 거래 구분 코드 | se_cd |
| trade_type_name | text | 대표 거래 구분 이름 | se_nm |
| variety_code | text | 대표 품종 코드 | vrty_cd |
| variety_name | text | 대표 품종 이름 | vrty_nm |
| grade_code | text | 대표 등급 코드 | grd_cd |
| grade_name | text | 대표 등급 이름 | grd_nm |
| unit | text | 대표 판매 단위: kg, g, 개 등 | unit |
| unit_size | numeric | 대표 판매 단위 크기. 양수·유한값 | unit_sz |

- 품목별 대표 규격 하나를 유지합니다. API 축약어는 적재 코드에서 위 컬럼명으로 매핑합니다.
- 모든 문자열 컬럼은 공백만 있는 값을 허용하지 않습니다.
- price_basis 설명 문자열은 제거했습니다. 규격 비교는 거래 구분·품종·등급·단위·단위 크기 값으로 수행합니다.
- 기존 설계안의 name은 item_name, market_type_code/market_type_name은 trade_type_code/trade_type_name으로 반영했습니다.
- price_basis_version, storage_type, active는 미구현 설계 항목이며 현재 컬럼이 아닙니다. unit_size의 실제 타입은 정밀도 제한을 별도로 지정하지 않은 numeric입니다.

### ingredient_price_history — 원자재 가격 이력

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| ingredient_id | bigint FK → ingredient.id, 복합 PK | 원자재 |
| observed_date | date, 복합 PK | 가격 조사일 |
| price | numeric(18,4), NULL 허용 | 대표 판매 규격의 조사일 평균 가격 |
| previous_day_price | numeric(18,4), NULL 허용 | 동일 원자재·규격의 직전 관측일 가격 |
| daily_change_rate | numeric, 자동 생성, NULL 허용 | (현재 − 직전 관측 가격) / 직전 관측 가격 × 100(%), 소수 4자리 |
| price_trend | text, 자동 생성, NULL 허용 | 직전 관측일 대비 UP / DOWN / FLAT |
| one_week_before_price | numeric(18,4), NULL 허용 | API ww1_bfr_avg_prc |
| two_weeks_before_price | numeric(18,4), NULL 허용 | API ww2_bfr_avg_prc |
| three_weeks_before_price | numeric(18,4), NULL 허용 | API ww3_bfr_avg_prc |
| four_weeks_before_price | numeric(18,4), NULL 허용 | API ww4_bfr_avg_prc |
| created_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | 최초 생성 시각. 재적재 시 유지 |

- V20__ingredient_price_changes.sql을 로컬 DB에 Flyway로 적용했습니다. Neon에는 이번 변경을 적용하지 않았으며 main push 후 별도로 반영합니다.
- 2026-11-20 실제 API 조회로 2026-10-19~11-19의 118개 원자재, 총 3,497개 이력을 로컬에 적재했습니다. 11-20 응답은 0건이므로 임의 행을 만들지 않았습니다. 적재 전 로컬 가격 이력은 비어 있었습니다.
- 모든 가격은 ingredient.unit_size × ingredient.unit 규격입니다. 거래 구분·품종·등급·단위·단위 크기가 같은 API 행만 연결하며 대표 규격을 변경하거나 다른 규격으로 대체하지 않습니다.
- V22에서 비교 기준을 같은 원자재의 직전 관측일로 변경했습니다. LAG(price) OVER (PARTITION BY ingredient_id ORDER BY observed_date)로 계산하며 날짜가 연속되지 않아도 직전 행을 비교합니다. 최초 관측·직전 관측 가격 미제공·0이면 변동률·흐름은 NULL이며 FLAT으로 채우지 않습니다. 미제공 가격을 건너뛰지는 않습니다. 비교 가능한 동일 가격만 FLAT입니다. 컬럼명 previous_day_price와 daily_change_rate는 호환성을 위해 유지하며, 날짜가 벌어진 경우 변동률은 그 관측 구간 전체의 변동률입니다.
- 로컬 V22 적용으로 관측 간 공백 17건을 채웠습니다. 직전 관측 가격이 있는 이력은 3,379건이며 최초 관측 118건만 NULL입니다. 원래 가격·1~4주 가격·생성 시각은 보존했습니다.
- 방향은 반올림 전 실제 가격을 비교합니다. 미세한 변동은 표시 변동률이 0.0000이어도 UP 또는 DOWN일 수 있습니다.
- 1~4주 가격은 해당 조사일 API가 제공하는 평균 가격이며 7·14·21·28일 전 이력으로 대체하지 않습니다. kg 환산 필드도 사용하지 않습니다. API 미제공 값은 NULL이며 음수·비숫자·무한대는 거부합니다.
- 기본 키는 (ingredient_id, observed_date)입니다. API 재적재는 같은 키를 갱신하고 최초 생성 시각을 보존합니다. 과거 가격 정정·추가 시 해당 원자재의 직전 관측 비교값을 다시 계산합니다.
- 원자재 삭제·ID 변경은 RESTRICT입니다. raw_payload는 저장하지 않으며 실제 응답·수집 보고서는 server/build/ingredient-import 아래에 보관합니다.
- 수집·적재 절차는 [server README](../server/README.md#재료와-가격-이력-적재)를 참고하세요. 주기 실행과 Neon 적재는 수행하지 않았습니다.

### city_data_area — 도시데이터 지원 지역

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| code | text PK | 공식 지역 코드 |
| name | text UNIQUE | 공식 지역 이름 |
| category | text | 지역 분류 |
| boundary | geography(MultiPolygon,4326) | WGS84 위도·경도 기준 전체 지역 경계 |
| coord | jsonb, 자동 생성 | boundary에서 생성하는 GeoJSON MultiPolygon 좌표 배열. 좌표 순서는 [경도, 위도] |
| created_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | 생성 시각, 초 단위 |

- PostGIS 확장이 필요합니다. boundary는 비어 있지 않고 유효한 도형이어야 합니다.
- boundary에는 GiST 공간 인덱스가 있습니다. 영역 포함 여부와 거리 계산에 사용합니다.
- coord는 저장형 생성 컬럼이며 직접 입력·수정하지 않습니다. DB 정의에 별도의 NOT NULL 제약은 없지만 필수 boundary에서 자동 생성됩니다.
- source_id, active는 미구현 설계 항목입니다. 기존 설명의 JSON 경계는 지도 표시용 coord와 계산용 boundary로 구분해 반영했습니다.

### ingredient_rule — 원자재 추천 규칙

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint PK, 자동 생성 | 규칙 ID |
| trigger_code | text UNIQUE, 공백 불가 | 제철·가격 조건 코드. 예: IN_SEASON_MONTH_07, PRICE_DROP_ONE_WEEK_UNDER_5_PERCENT |
| trigger_text | text, 공백 불가 | LLM에 전달할 조건 설명. 예: 7월 제철 |
| demand | text | 구매 권장 방향: UP / STRONG_UP / DOWN / STRONG_DOWN |
| lead_days_min / lead_days_max | integer, NULL 허용 | 명절·김장 규칙의 준비 일수(14~21일 전). 둘 다 NULL이거나 0 ≤ min ≤ max |

- 규칙에는 ingredient_id, industry_id를 두지 않습니다. 같은 규칙을 여러 원자재가 공유하고, 같은 원자재도 여러 규칙에 연결될 수 있습니다.
- 제철 월은 IN_SEASON_MONTH_01~IN_SEASON_MONTH_12, 제철 시작 한 달 전은 BEFORE_SEASON_START_MONTH_01~12로 구분합니다. 제철의 첫 달과 나머지 달은 구분하지 않습니다. 가격 조건은 서버가 trigger_code별로 판정하며 trigger_text를 파싱하거나 LLM에 조건 충족 여부를 맡기지 않습니다.
- 별도 제철 달력·트리거 마스터 테이블은 만들지 않습니다. 서버가 현재 한국 시간의 월에 대응하는 규칙과 원자재별 가격 조건을 조회합니다.
- demand는 판매 수요나 소비 속도 예측이 아닌 서비스의 구매 권장 방향입니다. UP은 구매 권장, STRONG_UP은 강력 구매 권장, DOWN은 구매 미권장, STRONG_DOWN은 강한 구매 미권장입니다. 같은 컬럼명을 쓰는 supply_rule의 수요 방향과 의미를 구분합니다.
- 제철 시작 한 달 전은 UP, 제철인 모든 달은 STRONG_UP입니다. 7~8월 제철이라면 6월은 UP, 7월·8월은 모두 STRONG_UP입니다. 가격 하락은 UP이며 1주 대비 10% 이상 하락은 STRONG_UP입니다. 가격 상승은 DOWN이고 10% 이상 상승은 STRONG_DOWN입니다.
- 제철과 가격을 묶은 복합 규칙은 만들지 않습니다. 서버가 개별 규칙의 구매 신호를 모아 최종 권장 강도를 계산하며 LLM에는 결과와 개별 근거를 함께 전달합니다.
- 가격 하락은 비용 절감에 따른 구매 권장, 가격 상승은 원가 부담에 따른 구매 미권장입니다. 미래 가격이나 판매 수요를 예측한 값이 아니며 미권장은 필수 구매 금지가 아닙니다.
- 제철·가격 규칙은 준비 일수가 없어 lead_days가 NULL입니다. 명절·김장 규칙(BEFORE_KIMJANG·BEFORE_SEOLLAL·BEFORE_CHUSEOK)만 준비 일수를 두고, 날짜는 supply_trigger_date의 KIMJANG·SEOLLAL·CHUSEOK 기간을 씁니다.
- V24에서 PRICE_BELOW_FOUR_WEEKS(4주 전보다 낮은 가격)를 지웠습니다. 1주 비교와 같은 가격 변화를 두 번 세기 때문이며, 4주 변화는 LLM이 change_4w_pct로 받습니다. 1주 비교 규칙 37~40은 같은 가격 변화에 하나만 걸려서 남깁니다.

V24 전에는 제철 24개(제철 월·시작 한 달 전 각 12개)와 가격 5개, 총 29개 규칙이었습니다. V24에서 1개를 지우고 4개(10% 이상 상승, 김장·설·추석 전)를 더해 32개가 됩니다. 별도로 나눴던 제철 진입 월 규칙은 삭제했습니다. 규칙 초기 데이터는 V13__ingredient_rule_seed.sql과 V14__simplify_ingredient_season_rules.sql로 재현합니다. ingredient_rule_item은 실제 생성·적재했으며 조건 판정·추천 로직은 아직 미구현입니다.

V26에서 고정 % 가격 규칙 5개(1주 하락 5% 미만·5~10%·10% 이상, 상승 10% 미만·10% 이상)를 지우고 평년 비율·1주 변동률 단계 규칙 10개를 넣었고, V27에서 이 10개를 4주 변동률 단계 규칙 6개로 바꿨습니다. 규칙은 33개가 됩니다. V26은 main에 올라가 배포 DB에 적용됐을 수 있어 고치지 않고 V27로 바꿨습니다.

- 단계: 정부 농산물 수급조절매뉴얼의 7단계(상승 심각·경계·주의 / 안정 / 하락 주의·경계·심각)입니다. 경계값은 아래 ingredient_price_band의 부류별 분위수(상·하위 5% · 12.5% · 20%)이고, 매뉴얼의 상승 구간 확률분포 할당(배추·무·건고추 심각 5%·경계 12.5%, 주의는 품목별 15~20%)을 따릅니다.
- 규칙: PRICE_RISE_FOUR_WEEKS_SEVERE·ALERT·WATCH, PRICE_DROP_FOUR_WEEKS_WATCH·ALERT·SEVERE. 한 품목에 가격 규칙은 1개만 걸립니다. 1주 변동은 화면의 가격 흐름(trend)으로만 쓰고 점수에 넣지 않습니다.
- demand: 상승 심각·경계 STRONG_DOWN, 주의 DOWN, 하락 주의 UP, 경계·심각 STRONG_UP.
- 매뉴얼은 평년 대비 가격 수준을 보지만, 5년 평년은 물가 상승이 섞이고(평년 비율 중앙값 1.09) 원자재 118개 중 67개만 5년 가격이 이어져서 모든 품목에 있는 4주 변동률을 씁니다. 하락 구간도 비용 자료가 없어 같은 분위수를 거꾸로 씁니다. 판정 코드: server의 PriceBand.stageOf, PriceStage.ruleCode.

### cultural_event — 오프라인 문화행사

| 컬럼 | 타입·제약 | 설명 / API 필드 |
| --- | --- | --- |
| id | bigint PK, 자동 생성 | 내부 행사 ID |
| source_key | text UNIQUE, 숫자 문자열 | HMPG_ADDR의 cultcode. 제목·일정 변경에도 유지 |
| title | text, 공백 불가 | TITLE |
| category | text, NULL 허용 | CODENAME |
| district_name | text, NULL 허용 | GUNAME |
| place | text, 공백 불가 | PLACE |
| start_date / end_date | date | STRTDATE / END_DATE. 종료일 ≥ 시작일 |
| time_text | text, NULL 허용 | PRO_TIME. 운영시간은 원문 유지 |
| coord | geography(Point,4326) | 필수 WGS84 좌표. Point(LOT 경도, LAT 위도) |
| target_audience | text, NULL 허용 | USE_TRGT |
| program / notes | text, NULL 허용 | PROGRAM / ETC_DESC |
| fee_text | text, NULL 허용 | USE_FEE |
| is_free | boolean, NULL 허용 | IS_FREE 무료=true, 유료=false, 미상=NULL |
| source_url | text | HMPG_ADDR. 문화포털 상세 페이지 |
| image_url | text, NULL 허용 | MAIN_IMG |
| is_active | boolean, 기본값 true | 최신 전체 수집에서도 유효한 오프라인 행사인지 여부 |
| raw_payload | jsonb 객체 | 원본 25개 필드 전체. RGSTDATE는 여기에 보존 |
| created_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | DB 최초 적재 시각. 갱신해도 유지 |
| updated_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | 내용·활성 상태 변경 시 DB 트리거로 갱신. 동일 응답이면 유지 |

- 2026-11-20(한국 시간) 실제 API 19,791건을 조회하여 18,838건 적재했습니다. 온라인·혼합 451건, 좌표 오류 452건, 일정 오류 50건은 제외했습니다. 과거 행사도 보관하므로 추천 시 기간 필터가 필요합니다.
- 좌표가 있어도 온라인일 수 있습니다. 제목·장소의 온라인/비대면/원격/유튜브 등 표기를 보수적으로 제외합니다. 공식 개최 방식 필드는 없어 혼합 행사도 제외될 수 있고 미표기 온라인 행사는 남을 수 있습니다.
- 좌표는 NULL·0·비숫자·위경도 범위 초과를 거부합니다. 공간 인덱스는 활성 행사의 coord에 있습니다. ST_DWithin 및 ST_Distance는 미터 단위를 사용합니다. 지도용 배열은 ST_AsGeoJSON(coord::geometry)의 coordinates로 얻습니다.
- source_key 충돌 시 기존 ID로 갱신합니다. 전체 페이지의 건수·식별자를 검증한 수집만 적용합니다. 누락·제외 행사는 삭제하지 않고 is_active=false로 전환하며, 다시 유효해지면 활성화합니다. 수집 실패·빈 응답은 기존 데이터를 건드리지 않습니다.
- API에 행사별 수정 시각이 없어 원본 내용 비교로 변경을 감지합니다. RGSTDATE는 원본 등록일이며 DB 생성·수정 시각으로 사용하지 않습니다.
- V15__cultural_event.sql을 연결된 DB에 수동 적용했습니다. 자동 마이그레이션·주기 실행은 구성하지 않았습니다. 원본 API 중계 응답과 프론트 호출은 변경하지 않았습니다.
- 수집·갱신 기준: [server README](../server/README.md#문화행사-수집과-갱신). 별도 수집·검증·조회용 SQL/MJS 파일은 제공하지 않습니다.

### ingredient_rule_item — 규칙별 원자재

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| rule_id | bigint FK → ingredient_rule.id, 복합 PK | 추천 규칙 |
| ingredient_id | bigint FK → ingredient.id, 복합 PK | 규칙이 적용되는 원자재 |

- 기본 키는 (rule_id, ingredient_id)입니다. 규칙 삭제 시 연결 행도 삭제하며 원자재는 유지합니다.
- 업종은 규칙에 저장하지 않습니다. 가게가 선택한 원자재 또는 industry_ingredient로 추천 대상을 제한합니다.
- 서버는 연결된 원자재 중 조건을 충족하는 품목만 고르고, 순위는 근거 점수로 정합니다. LLM은 순위를 바꾸지 않습니다. V24에서 품목 코드순이던 sort_order를 지웠습니다.
- 제철이 여러 달이면 해당 월의 규칙마다 같은 원자재를 연결합니다. 대표 품종·원산지·신선/냉동 상태에 적용되는 제철만 연결하고, 규칙이 없으면 제철 여부 미확인으로 전달합니다.

V24 전 연결은 총 740개였습니다. 제철 원자재 31개에 제철 월 119개·시작 전 31개를 연결하고, 전체 원자재 118개에 가격 규칙 5개씩 590개를 연결했습니다. V24에서 4주 규칙 연결 118개를 지우고 10% 이상 상승 규칙 118개, 김장 11개, 설·추석 각 6개를 더해 763개가 됩니다. 대표 원자재는 V10__ingredient_seed.sql, 제철 근거 URL과 연결은 V17__ingredient_rule_item_seed.sql에 보존했습니다. CSV·JSON 사본은 제거했습니다.

- 제철 없음·확인 필요·참고 범위·저장품 여부 확인 필요·정확한 월 경계 미확정인 87개는 제철 연결에서 제외했습니다. 가격 규칙 대상에는 포함됩니다.
- 원자재 삭제·ID 변경은 RESTRICT입니다. ingredient_id 역조회 인덱스가 있습니다.
- 제철·명절 연결은 적용 가능한 대상입니다. 4주 가격 규칙 연결은 가격 일배치가 매일 판정 결과로 다시 만듭니다(원자재당 최대 1개).
- V26·V27을 거쳐 고정 % 가격 규칙 5개의 연결 590개가 지워지고, 4주 단계 규칙 6개가 전체 원자재에 연결돼 708개가 더해졌습니다. 이 연결은 첫 가격 일배치 실행 때 판정 결과로 바뀝니다.

### ingredient_price_band — 원자재 가격 단계 경계 (V26 생성, V27에서 4주 기준으로 다시 만듦)

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| category_name | text PK | ingredient.category_name 부류 |
| sample_count | integer > 0 | 분위수를 계산한 표본 수 |
| p5 / p12_5 / p20 | numeric(8,4) | 하락 심각 / 경계 / 주의 경계값 (하위 5% · 12.5% · 20%) |
| p80 / p87_5 / p95 | numeric(8,4) | 상승 주의 / 경계 / 심각 경계값 (상위 20% · 12.5% · 5%) |
| sample_period | text | 자료 기간과 표본 방식 |

- 값은 4주 변동률 = 조사일 가격 ÷ 4주 전 평균 가격(ww4_bfr_avg_prc) − 1입니다. IngredientPriceResponse.change4w와 같은 값입니다.
- 여섯 경계값은 작은 값부터 순서대로여야 합니다(CHECK).
- 2026-11-21에 aT 가격 추이 API(B552845/priceSequel)의 2019-01~2026-11 조사일을 2주 간격으로 받아 계산했습니다. 원자재 118개 모두 4주 전 가격이 있고, 표본은 부류별 892~5,423개(합계 15,798개)입니다.
- 같은 4주 변동이라도 부류마다 단계가 다릅니다. 예: 10% 상승은 채소류(상위 20% 경계 +13.1%)에선 안정, 축산물(상위 5% 경계 +8.7%)에선 상승 심각입니다. 4주 ±10%가 채소류는 46%, 축산물은 6.5% 걸려서 고정 %를 쓰지 않습니다.
- 경계값은 매년 초 다시 계산해 이 표만 고칩니다. 수급조절매뉴얼도 매년 기준가격을 갱신합니다.

### 가격 일배치 (INGREDIENT_PRICE_DAILY)

server의 IngredientPriceBatch가 매일 10시(KST, batch.ingredient-price.cron)에 돕니다.

1. 마지막 조사일 다음 날부터 오늘까지(최대 7일) aT 가격 추이 API를 받아, 대표 규격(거래 구분·품종·등급·단위·단위 크기)이 같은 행만 ingredient_price_history에 upsert하고 직전 관측 가격을 V22와 같은 방식으로 다시 계산합니다.
2. 가장 최근 조사일에 가격과 4주 전 가격(ww4_bfr_avg_prc)이 모두 있는 원자재만 4주 변동률 = 가격 ÷ 4주 전 가격 − 1을 구하고, ingredient_price_band의 부류 경계로 단계를 정합니다(경계값과 같으면 그 단계, 예: p95 이상이면 상승 심각).
3. 4주 가격 규칙 6개(PRICE_%FOUR_WEEKS_%)의 ingredient_rule_item 연결을 지우고 판정된 규칙 하나씩만 다시 연결합니다. 안정, 4주 전 가격 없음, 그날 조사되지 않은 원자재는 연결하지 않습니다. 제철·명절 규칙 연결은 건드리지 않습니다.

실행마다 batch_job 1행, 시도마다 batch_job_history 1행을 남깁니다(read = 적재한 가격 행, write = 만든 규칙 연결, skip = 판정 못 한 원자재). 실패하면 batch.max-retry-times만큼 바로 다시 시도하고, 모두 실패하면 이전 연결을 그대로 둡니다.

### industry_ingredient — 업종별 추천 원자재

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| industry_id | bigint FK → industry.id, 복합 PK | 추천 대상 업종 ID |
| ingredient_id | bigint FK → ingredient.id, 복합 PK | 추천 원자재 ID |
| sort_order | smallint, 1~3 | 업종 내 고정 추천 순서 |
| industry_name | text | 업종명. industry_id와 일치하는 원본 이름 |
| ingredient_name | text | 원자재명. ingredient_id와 일치하는 원본 item_name |

- (industry_id, ingredient_id)는 기본 키이고 (industry_id, sort_order)는 UNIQUE입니다. NULL이 아닌 순번을 1~3으로 제한하므로 업종별 최대 3개를 DB에서 보장합니다.
- 컨펌한 14개 업종에 총 42개를 연결했습니다. 배달 전문과 비식품 업종은 연결하지 않았습니다.

### industry_supply — 업종별 추천 부자재

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| industry_id | bigint FK → industry.id, 복합 PK | 추천 대상 업종 ID |
| supply_id | bigint FK → supply.id, 복합 PK | 추천 부자재 ID |
| sort_order | smallint, 1~2 | 업종 내 고정 추천 순서 |
| industry_name | text | 업종명. industry_id와 일치하는 원본 이름 |
| supply_name | text | 부자재명. supply_id와 일치하는 원본 이름 |

- (industry_id, supply_id)는 기본 키이고 (industry_id, sort_order)는 UNIQUE입니다. NULL이 아닌 순번을 1~2로 제한하므로 업종별 최대 2개를 DB에서 보장합니다.
- 컨펌한 28개 업종에 총 56개를 연결했습니다.

두 테이블 모두 업종 삭제 시 연결만 CASCADE로 삭제하며 원자재·부자재 삭제와 참조 ID 변경은 RESTRICT입니다. 순서는 컨펌 표의 나열 순서를 유지하며 가격 조건이나 LLM 판단으로 바꾸지 않습니다. V21__industry_recommendations.sql에 구조와 초기 데이터를 함께 저장했습니다. 품목 이름으로 기존 ID를 찾아 연결하므로 다른 DB의 ID가 달라도 동일 품목을 연결합니다. 조건별 규칙의 대상 목록인 ingredient_rule_item·supply_rule_item과는 별개의 업종별 기본 추천입니다.

V23__industry_recommendation_names.sql에서 기존 연결의 이름을 채웠습니다. 이름은 NOT NULL이며 ID·이름 쌍의 복합 외래 키로 원본과의 일치를 보장합니다. 원본 업종명·원자재명·부자재명을 변경하면 ON UPDATE CASCADE로 연결 테이블의 이름도 갱신됩니다. 이를 위해 industry(id, name), ingredient(id, item_name), supply(id, name)에 복합 UNIQUE 제약을 추가했습니다. 새 연결을 입력할 때도 ID와 일치하는 이름을 함께 넣습니다.

### support_program — 지원사업 공고 (V30)

기업마당 지원사업 API(`1421000/bizinfo/pblancBsnsService`, 공공데이터포털 15157820)의 접수 중인 공고 중 서울 소상공인 대상 공고. `GET /api/support-programs`(마감 임박 지원사업 카드)가 읽습니다.

| 컬럼 | 타입·제약 | 설명 / API 필드 |
| --- | --- | --- |
| id | text PK, `PBLN_` + 숫자 | pblancId |
| title | text, 공백 불가 | pblancNm. 공공누리 제3유형(변경금지)이라 앞뒤 공백도 원문 그대로 |
| url | text, `https://www.bizinfo.go.kr/`로 시작 | pblancUrl |
| agency_name | text, 공백 불가 | jrsdInsttNm 소관기관 |
| field_name | text, NULL 허용 | pldirSportRealmLclasCodeNm 분야 (경영·금융·기술·인력·수출·내수·창업·기타) |
| target_name | text, 공백 불가 | trgetNm 지원 대상. 공고마다 하나 (중소기업·소상공인·창업벤처 등) |
| hashtags | text[] | hashtags를 쉼표로 나누고 앞뒤 공백·빈 태그를 뺀 것 |
| application_period | text, 공백 불가 | reqstBeginEndDe 원문 |
| deadline | date, NULL 허용 | 신청 기간이 `YYYY-MM-DD ~ YYYY-MM-DD`(점 구분 포함)일 때 뒤 날짜. 그 밖에는 NULL |
| registered_datetime | timestamptz(0) | creatPnttm (KST) |
| created_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | 수집 시각. 응답의 `checkedAt`은 가장 최근 값 |

2026-11-21 실제 API를 전부 받아(1,000건씩 2번, 약 8초) 정한 것입니다.

- API는 접수 중인 공고만 줍니다. 1,303건 중 마감이 지난 공고는 0건이었습니다. 그래서 이력을 두지 않고 일배치가 한 트랜잭션에서 지우고 다시 넣습니다. 받은 건수가 `totalCount`와 다르거나 0건이면 바꾸지 않습니다.
- 신청 기간은 날짜 범위가 381건, 날짜가 없는 문구가 922건(예산 소진시까지 645 · 상시 접수 137 · 선착순 접수 35 · 모집 완료시 31 · 세부사업별 상이 20 등 30여 종)입니다. `매월 10일 18:00까지`처럼 날짜 범위가 아닌 것도 NULL입니다. 시작일이 오늘 이후인 공고도 있습니다.
- API에는 지역 코드 파라미터가 없습니다. 걸리는 조건은 분야(`searchLclasId`)와 해시태그뿐이고, `jrsdInsttNm`·`areaCd` 등을 넣어도 1,303건 그대로입니다.
- 서울 소상공인 대상 공고만 저장합니다(1,303건 중 40건). 일배치가 아래 넷을 모두 만족하는 공고만 넣습니다(`SupportProgramService.isTarget`). 조회는 파생 쿼리만 쓰도록 배열 조건을 저장할 때 겁니다.
  - `서울` 태그가 있음: 전국 공고는 `전국` 대신 17개 시도 태그를 모두 답니다(`전국` 태그 2건). 서울특별시 소관 38건은 모두 `서울` 태그가 있습니다. 384건.
  - 소관기관이 서울 밖 지자체가 아님: 이름이 `도`·`시`로 끝나는 15곳이 모두 광역 지자체이고, 중앙부처 20곳은 `부`·`처`·`청`·`회`로 끝납니다. 이름을 나열하지 않아 `전남광주통합특별시`처럼 바뀐 이름도 걸립니다.
  - 제목 맨 앞 `[..]`이 없거나 거기에 서울이 있음: 31종 모두 지역 표시입니다(`[경북]`, `[충남ㆍ충북ㆍ대전ㆍ세종]`, `[비수도권]` 등). `[서울ㆍ인천ㆍ경기ㆍ강원]`은 남깁니다.
  - 소상공인 대상: `소상공인` 태그(194건)와 target_name(179건)이 서로 빠뜨리는 공고가 있어 둘 중 하나면 됩니다(224건). API의 `hashtags=소상공인` 검색(207건)은 `소상공인시장진흥공단` 같은 태그도 걸고 target_name만 소상공인인 26건을 빠뜨려 쓰지 않습니다.
- 서울 조건 셋으로 308건이 남고, `서울` 태그가 있는데 뒤의 두 조건으로 빠지는 공고가 76건입니다(경상북도 14, 전남광주통합특별시 11, 충청남도 7 등). 서울·소상공인으로는 43건 중 3건(경상북도 경북 특별관, 경기도 과천시 육성자금, `[광주]` SNS 홍보지원)입니다.
- 제목 중간의 지역명은 보지 않습니다. `환경기업`에 `경기`가 들어 있는 식으로 잘못 걸려서입니다. 그래서 중소벤처기업부의 `소담스퀘어 in 광주` 같은 지역 센터 공고는 남습니다.
- 사업 개요(bsnsSumryCn, HTML 최대 11KB)·첨부파일·문의처·조회수는 화면에서 쓰지 않아 저장하지 않습니다. 행이 적어 인덱스는 두지 않습니다.

카드 조회는 `SupportProgramRepository.findTop5ByDeadlineGreaterThanEqualOrderByDeadlineAsc(오늘)`입니다. 오늘 이후 마감인 공고를 임박한 순으로 5개 주고, 마감일이 없는 공고는 나오지 않습니다. 오늘은 KST로 넘깁니다. `checkedAt`은 `findFirstByOrderByCreatedDatetimeDesc()`의 수집 시각입니다.

2026-11-21에는 40건 중 마감일이 있는 공고가 11건이고, 가장 이른 마감은 11-30입니다. V30은 로컬 DB(Homebrew PostgreSQL 18)에 적용했고 Neon에는 아직 적용하지 않았습니다. V30 파일 첫 줄 주석의 "서울·소상공인 조건은 조회할 때 건다"는 이 변경 전 설계입니다(로컬에 이미 적용돼 파일을 고치지 않음).

### 지원사업 일배치 (SUPPORT_PROGRAM_DAILY)

server의 SupportProgramBatch가 매일 6시(KST, batch.support-program.cron)에 돕니다. 기업마당은 낮에 공고를 올려서 아침에 전날 공고까지 받습니다.

1. 필터 없이 500건씩 `totalCount`까지 받습니다. 1,000건씩 받으면 한 번에 6~11초 걸려 공공 API 응답 제한 15초를 넘길 때가 있습니다(500건은 4~5초). 공고 ID·제목·URL·소관기관·지원 대상·신청 기간·등록 일시 중 하나라도 위 제약에 맞지 않는 공고는 건너뜁니다.
2. 받은 건수가 `totalCount`와 다르거나, 같은 공고가 두 번 왔거나(받는 사이 새 공고가 올라와 페이지가 밀림), 0건이면 실패합니다. 테이블은 건드리지 않고 다시 시도합니다.
3. 서울 소상공인 대상 공고만 남기고, 한 트랜잭션에서 support_program을 모두 지우고 넣습니다.

실행마다 batch_job 1행, 시도마다 batch_job_history 1행을 남깁니다(read = API 전체 건수, write = 넣은 공고, skip = 필수 값이 없거나 서울 소상공인 대상이 아니어서 뺀 공고). 한 번도 성공하지 않아 테이블이 비어 있으면 `GET /api/support-programs`는 503(`SERVICE_UNAVAILABLE`)을 돌려줍니다.

## 현재 생성된 관리 테이블

### batch_job — 작업·재시도 관리

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| id | bigint PK, 자동 생성 | 작업 ID |
| job_name | text, 공백 불가 | 수집 작업명 |
| job_parameters | jsonb 객체, 기본값 {} | 실행 조건. 비밀값 저장 금지 |
| status | text, 기본값 PENDING | PENDING / RUNNING / SUCCEEDED / FAILED |
| retry_times | integer, 기본값 0, 0 이상 | 현재 재시도 횟수. 최초 실행 0 |
| created_datetime / updated_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | 생성 / 변경 시각 |

### batch_job_history — 실행별 이력

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| id | bigint PK, 자동 생성 | 실행 이력 ID |
| batch_job_id | bigint FK → batch_job.id | 작업과 1:N. 삭제·ID 변경 RESTRICT |
| retry_times | integer, 0 이상 | 해당 실행의 재시도 차수. 최초 실행 0 |
| status | text, 기본값 RUNNING | RUNNING / SUCCEEDED / FAILED |
| started_datetime | timestamptz(3), 기본값 CURRENT_TIMESTAMP(3) | 시작 시각. 소요시간의 밀리초 정밀도 유지 |
| finished_datetime | timestamptz(3), NULL 허용 | 종료 시각. 종료 상태에서는 필수 |
| running_time | bigint, 자동 생성, NULL 허용 | 종료 − 시작의 밀리초. 실행 중 NULL |
| error_code / error_message | text, NULL 허용 | 오류 분류 / 비밀값을 제거한 원인 |
| read_count / write_count / skip_count | bigint, 기본값 0, 0 이상 | 읽기 / 반영 완료 / 제외 건수 |
| created_datetime / updated_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | 생성 / 변경 시각 |

- (batch_job_id, retry_times)는 UNIQUE이며 같은 작업의 RUNNING 이력은 최대 1개입니다. 종료 시각은 시작 시각 이상이어야 합니다.
- 변경 시 DB 트리거가 updated_datetime을 갱신하고 created_datetime은 유지합니다.
- application.yml의 batch.max-retry-times=3은 재시도 최대 3회, 최초 포함 총 4회입니다. DB에는 최대 횟수를 저장하지 않습니다.
- 부모 횟수 증가·이력 생성의 트랜잭션 처리, 상태 전이·재시도 실행 및 중단된 작업 복구는 아직 애플리케이션에 연결하지 않았습니다. running_time은 생존 확인 용도가 아닙니다.
- V18__batch_job_history.sql을 연결된 DB에 적용했습니다. 별도 검증 SQL 파일은 보관하지 않습니다.

### flyway_schema_history — DB 구조 변경 적용 이력

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| installed_rank | integer PK | 적용 순서 |
| version | varchar(50), NULL 허용 | 마이그레이션 버전 |
| description | varchar(200) | 변경 설명 |
| type | varchar(20) | 마이그레이션 종류 |
| script | varchar(1000) | 실행 파일명 |
| checksum | integer, NULL 허용 | 파일 체크섬 |
| installed_by | varchar(100) | 실행 사용자 |
| installed_on | timestamp, 기본값 now() | 적용 시각, 시간대 없음 |
| execution_time | integer | 실행 시간, 밀리초 |
| success | boolean | 성공 여부 |

도구가 관리하는 테이블입니다. 테이블이 있다는 사실만으로 현재 서버가 마이그레이션을 자동 실행한다는 뜻은 아닙니다.

2026-11-20 Neon의 이력을 V18까지 정합화했습니다. 빈 로컬 검증 DB에 V1~V18을 적용하여 실제 구조·초기 기준 데이터의 일치를 확인하고, 전체 백업 후 이미 적용된 V8~V18은 Flyway의 skipExecutingMigrations로 SQL 재실행 없이 이력만 등록했습니다. 기존 V1~V7 이력과 업무 데이터·구조는 유지됐으며 Flyway validate 및 로컬/Neon 체크섬 대조를 통과했습니다. 다음 변경은 V19부터 추가합니다. 서버 시작·main 푸시 시 자동 마이그레이션은 아직 구성하지 않았습니다.

### spatial_ref_sys — PostGIS 좌표계 정의

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| srid | integer PK | 좌표계 식별자. 0 초과, 998999 이하 |
| auth_name | varchar(256), NULL 허용 | 좌표계 정의 기관 |
| auth_srid | integer, NULL 허용 | 해당 기관의 좌표계 코드 |
| srtext | varchar(2048), NULL 허용 | 좌표계 WKT 정의 |
| proj4text | varchar(2048), NULL 허용 | 좌표계 PROJ 정의 |

PostGIS가 생성·관리하는 테이블이며 업무 테이블이 아닙니다. city_data_area는 좌표계 4326을 사용합니다. 확장 표준 컬럼명은 변경하지 않습니다.

### briefing_analysis_job — 분석 작업과 결과

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| id | uuid PK | 분석 번호. 서버가 만들어 POST 응답으로 돌려줍니다 |
| status | text, 기본값 PENDING | PENDING / RUNNING / SUCCEEDED / FAILED |
| steps | jsonb 배열, 기본값 [] | 4단계 진행 상태 (AnalysisStep) |
| base_time | timestamptz(0) | 리포트 만들기를 누른 시각 |
| input_snapshot | jsonb 객체 | 요청 원문(AnalysisRequest)과 확인한 가게 정보(StoreSummary)·카카오 행정동 코드 (AnalysisInput) |
| event_search | jsonb 객체, NULL 허용 | 행사 조회 조건과 건수 (EventSearch). 2단계가 끝나면 채움 (V31) |
| evidence_snapshot | jsonb 배열, NULL 허용 | 근거(E·H·W·R·S)별 행동 마감(deadline)·급한 정도·영향 크기·확실성·합계 점수. 4단계 실패여도 남김 |
| final_result | jsonb 객체, NULL 허용 | 브리핑 결과 (BriefingResponse). SUCCEEDED면 필수 |
| failure | jsonb 객체, NULL 허용 | 실패 정보 (AnalysisFailure). FAILED면 필수 |
| score_version | text, NULL 허용, 공백 불가 | 강추 점수표 버전. 점수표는 서버 코드 상수 |
| created_datetime / updated_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | 생성 / 변경 시각. 변경 시각은 batch_job과 같은 트리거로 갱신 |

- V24에서 만들었습니다. 분석이 1분쯤 걸려 POST로 시작하고 GET으로 상태를 다시 묻기 때문에, Render 무료 인스턴스가 재시작돼도 남도록 DB에 둡니다.
- 로그인이 없어 조회 토큰(access_token_hash)과 만료 시각은 두지 않습니다. 분석 번호가 UUID라 추측하기 어렵습니다.
- 점수 기준은 테이블(task_priority_policy)이 아니라 코드 상수이고 score_version으로 구분합니다. 기준: ai-server/docs/API_CONTRACT_DRAFT.md "강추 고르기 (Java)".
- 할 일(todo)은 서버에 저장하지 않고 브라우저 localStorage(`todos:{analysisId}`)에 둡니다.

### suggestion_feedback — 추천 카드 피드백

| 컬럼 | 타입·제약 | 설명 |
| --- | --- | --- |
| id | bigint PK, 자동 생성 | 피드백 ID |
| analysis_id | uuid FK → briefing_analysis_job.id | 분석 번호. 분석이 지워지면 함께 삭제 |
| industry_id | bigint FK → industry.id | 가게 업종. 집계용으로 분석 입력에서 복사 |
| card_key | text, 형식 검사 | 카드 키 `{category}:{task_type}:{item_id 또는 -}`. "해당 없어요" 제외 키와 같음 |
| category | text | EVENT / INGREDIENT / SUPPLY |
| task_type | text, 영문 대문자·밑줄 | 할 일 유형 |
| item_id | bigint, NULL 허용 | 원자재·부자재 ID. 행사 카드는 NULL |
| vote | text | HELPFUL 도움돼요 / NOT_RELEVANT 해당 없어요 |
| created_datetime / updated_datetime | timestamptz(0) | 생성 / 변경 시각 (batch_job과 같은 트리거) |

- V25에서 만들었습니다. (analysis_id, card_key)는 UNIQUE라 분석 1건의 카드 1장에 1표이고, 다시 누르면 표를 바꿉니다.
- card_key는 category·task_type·item_id를 이어 붙인 값과 같아야 합니다(CHECK).
- (industry_id, category, task_type) 인덱스로 업종 × 유형별 도움 비율을 집계합니다. 표가 충분히 모인 조합만 강추 점수를 ±1 보정하는 계획이며 집계 로직은 아직 없습니다. 기준: ai-server/docs/SUGGESTION_TODOS_DRAFT.md "피드백".

## 아직 생성하지 않은 테이블 설계안

다음은 전달받은 설계안입니다. 타입·관계·상태값은 향후 구현 시 검토해야 하며 현재 DB 구조로 해석하지 않습니다.
원자재는 ingredient_rule에 제철·가격 조건을 저장하고, ingredient_rule_item에서 원자재를 다대다로 연결합니다. 순위는 서버가 근거 점수로 정하며, 적용 규칙과 가격 데이터를 LLM에 전달해 추천 이유와 설명을 작성합니다.

### 원자재 규칙 예시 데이터

아래는 V24 전 데이터입니다. ingredient_rule은 실제 적재한 29개 규칙이며, ingredient_rule_item은 실제 DB에서 추출한 딸기(설향) 연결 일부입니다(sort_order 열은 V24에서 삭제). V24 변경은 아래 조건 표에 반영했습니다.

ingredient_rule（실제 적재 데이터）:

```csv
id,trigger_code,trigger_text,demand
1,IN_SEASON_MONTH_01,1월 제철,STRONG_UP
2,BEFORE_SEASON_START_MONTH_01,1월 제철 시작 한 달 전,UP
4,IN_SEASON_MONTH_02,2월 제철,STRONG_UP
5,BEFORE_SEASON_START_MONTH_02,2월 제철 시작 한 달 전,UP
7,IN_SEASON_MONTH_03,3월 제철,STRONG_UP
8,BEFORE_SEASON_START_MONTH_03,3월 제철 시작 한 달 전,UP
10,IN_SEASON_MONTH_04,4월 제철,STRONG_UP
11,BEFORE_SEASON_START_MONTH_04,4월 제철 시작 한 달 전,UP
13,IN_SEASON_MONTH_05,5월 제철,STRONG_UP
14,BEFORE_SEASON_START_MONTH_05,5월 제철 시작 한 달 전,UP
16,IN_SEASON_MONTH_06,6월 제철,STRONG_UP
17,BEFORE_SEASON_START_MONTH_06,6월 제철 시작 한 달 전,UP
19,IN_SEASON_MONTH_07,7월 제철,STRONG_UP
20,BEFORE_SEASON_START_MONTH_07,7월 제철 시작 한 달 전,UP
22,IN_SEASON_MONTH_08,8월 제철,STRONG_UP
23,BEFORE_SEASON_START_MONTH_08,8월 제철 시작 한 달 전,UP
25,IN_SEASON_MONTH_09,9월 제철,STRONG_UP
26,BEFORE_SEASON_START_MONTH_09,9월 제철 시작 한 달 전,UP
28,IN_SEASON_MONTH_10,10월 제철,STRONG_UP
29,BEFORE_SEASON_START_MONTH_10,10월 제철 시작 한 달 전,UP
31,IN_SEASON_MONTH_11,11월 제철,STRONG_UP
32,BEFORE_SEASON_START_MONTH_11,11월 제철 시작 한 달 전,UP
34,IN_SEASON_MONTH_12,12월 제철,STRONG_UP
35,BEFORE_SEASON_START_MONTH_12,12월 제철 시작 한 달 전,UP
37,PRICE_DROP_ONE_WEEK_UNDER_5_PERCENT,1주 전 대비 0% 초과 5% 미만 가격 하락,UP
38,PRICE_DROP_ONE_WEEK_5_TO_10_PERCENT,1주 전 대비 5% 이상 10% 미만 가격 하락,UP
39,PRICE_DROP_ONE_WEEK_AT_LEAST_10_PERCENT,1주 전 대비 10% 이상 가격 하락,STRONG_UP
40,PRICE_RISE_ONE_WEEK,1주 전 대비 가격 상승,DOWN
41,PRICE_BELOW_FOUR_WEEKS,4주 전 대비 낮은 가격,UP
```

ingredient_rule_item（실제 적재 데이터 일부）:

```csv
rule_id,trigger_code,ingredient_id,item_name,sort_order
1,IN_SEASON_MONTH_01,247,딸기,4
4,IN_SEASON_MONTH_02,247,딸기,4
7,IN_SEASON_MONTH_03,247,딸기,3
10,IN_SEASON_MONTH_04,247,딸기,1
13,IN_SEASON_MONTH_05,247,딸기,2
31,IN_SEASON_MONTH_11,247,딸기,5
32,BEFORE_SEASON_START_MONTH_11,247,딸기,3
34,IN_SEASON_MONTH_12,247,딸기,5
37,PRICE_DROP_ONE_WEEK_UNDER_5_PERCENT,247,딸기,29
38,PRICE_DROP_ONE_WEEK_5_TO_10_PERCENT,247,딸기,29
39,PRICE_DROP_ONE_WEEK_AT_LEAST_10_PERCENT,247,딸기,29
40,PRICE_RISE_ONE_WEEK,247,딸기,29
41,PRICE_BELOW_FOUR_WEEKS,247,딸기,29
```

| 조건 코드 | 서버 적용 조건 | 설명 방향 |
| --- | --- | --- |
| IN_SEASON_MONTH_07 | 현재 한국 시간 기준 7월 | STRONG_UP: 제철 품목 강력 구매 권장 |
| IN_SEASON_MONTH_08 | 현재 한국 시간 기준 8월 | STRONG_UP: 제철 품목 강력 구매 권장 |
| PRICE_DROP_ONE_WEEK_UNDER_5_PERCENT | 0% < 1주 대비 하락률 < 5% | UP: 구매 권장 |
| PRICE_RISE_ONE_WEEK | 0% < 1주 대비 상승률 < 10% (V24에서 좁힘) | DOWN: 구매 미권장 |
| PRICE_RISE_ONE_WEEK_AT_LEAST_10_PERCENT | 1주 대비 상승률 ≥ 10% (V24 추가) | STRONG_DOWN: 강한 구매 미권장 |
| BEFORE_KIMJANG | 김장철 시작 21~14일 전 (V24 추가, 설·추석도 같은 방식) | UP: 값 오르기 전 구매 |
| BEFORE_SEASON_START_MONTH_07 | 현재 한국 시간 기준 6월 | UP: 제철 시작 한 달 전 |
| PRICE_DROP_ONE_WEEK_5_TO_10_PERCENT | 5% ≤ 1주 대비 하락률 < 10% | UP: 구매 권장 |
| PRICE_DROP_ONE_WEEK_AT_LEAST_10_PERCENT | 1주 대비 하락률 ≥ 10% | STRONG_UP: 강력 구매 권장 |

가격 조건은 각 원자재의 동일 대표 규격끼리 비교하며, 비교 가격은 0보다 크고 두 가격 모두 유효해야 합니다. 가격이 같으면 상승·하락 규칙은 적용하지 않습니다.
하락률은 (1주 전 평균 가격 − 조사일 가격) ÷ 1주 전 평균 가격 × 100입니다. 구간의 5%·10%는 초기 운영 기준으로 규칙 데이터에 적재했습니다. 같은 비교 기간의 하락 구간은 정확히 하나만 적용해야 하며 실제 조건 판정 코드는 아직 미구현입니다.
예를 들어 규칙 37에서 일부 원자재만 해당 하락 구간에 속한다면 서버는 그 원자재만 선택하고 근거 점수로 순위를 정합니다. 상승·하락 규칙에 같은 원자재가 연결되어 있어도 같은 비교 가격에서는 동시에 충족되지 않습니다.

### reference_source — 근거 자료 (미생성)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint PK | 출처 ID |
| code | varchar | 출처 코드 |
| version | varchar | 자료 버전 |
| kind | varchar | 유형: PUBLIC_DATA / TEAM_GUIDE / OPERATING_PRINCIPLE |
| title / publisher | varchar | 자료명·발행 기관 또는 팀 |
| url | varchar, NULL | 자료 URL |
| content | text, NULL | 자료 원문·참고 원칙 |
| review_status | varchar | 검토 상태: DRAFT / REVIEWED |

## 원자재 우선순위 설계 (미구현)

가게가 선택한 원자재 또는 industry_ingredient로 연결된 원자재를 대상으로, 제철·가격 조건을 충족한 ingredient_rule과 가격 데이터를 LLM에 함께 전달합니다.
예를 들어 7월이면 IN_SEASON_MONTH_07 규칙을 조회하고 ingredient_rule_item의 ingredient_id로 대표 품목 및 가격을 연결합니다. 가격 규칙은 연결된 원자재마다 서버가 가격을 비교해 충족 여부를 판정합니다.
입력에는 분석 기준일, 원자재 ID·이름·대표 규격, 조사일, 조사일 가격, 1주·4주 전 가격과 변화율, 규칙의 코드·설명·구매 권장 방향(demand)과 서버가 정한 우선순위를 포함합니다.
서버는 날짜·대상 품목 조회, 변화율 계산, 개별 구매 신호 합산과 근거 점수 계산을 맡습니다. LLM은 서버가 확정한 순위·개별 신호·최종 권장 강도를 바꾸지 않고 가격·제철 근거를 설명합니다.
최종 응답의 품목 목록과 순서는 서버가 유지합니다. LLM 응답은 원자재·규칙 ID로 설명을 연결하고, LLM이 반환한 배열 순서나 점수로 재정렬하지 않습니다.
제철 여부를 LLM이 기억으로 생성하게 하지 않고 저장된 규칙을 근거로 제공합니다. 서버가 정한 순서·입력 근거와 생성된 설명은 briefing_analysis_job의 결과·근거 스냅샷 설계에 담습니다.

### 평가 근거

| 평가 항목 | 기준 | 추천 방향 |
| --- | --- | --- |
| 단기 가격 변화 | (조사일 가격 − 1주 전 가격) ÷ 1주 전 가격 × 100 | 하락: UP, 10% 이상 하락: STRONG_UP, 상승: DOWN |
| 최근 가격 수준 | (조사일 가격 − 4주 전 가격) ÷ 4주 전 가격 × 100 | 비교 기간에 맞는 규칙의 demand를 설명 |
| 제철 | 현재 월의 제철 트리거 규칙이 있는지 | 강력 구매 권장(STRONG_UP) |

- 서로 다른 원자재의 가격 금액을 직접 비교하지 않고, 동일 대표 규격의 변화율을 비교합니다.
- 최종 결과에는 규칙별 구매 권장 방향과 서버가 합산한 최종 권장 강도 및 계산 근거를 함께 남깁니다. 제철과 가격 조건의 신호는 서로 더하거나 상쇄할 수 있으며 LLM이 합산 기준을 바꾸지 않습니다.
- 가격 변화와 제철 규칙은 추천 이유를 설명하는 근거와 강추 점수의 원자재 신호로 사용합니다. 초기에는 제철 전용 점수표·가중치 테이블을 추가하지 않습니다.
- 기준 가격이 NULL 또는 0이면 해당 변화율을 계산하지 않습니다. 결측을 0% 변화로 처리하지 않습니다.
- 수집일이 아니라 observed_date를 기준으로 최신성을 확인하고 오래된 자료는 현재 구매 신호로 사용하지 않습니다. 최신성 허용 범위는 구현 시 정해야 합니다.
- 일별 가격 이력을 수집하고 있지만 품목·조사일별 결측이 있습니다. API의 1주·4주 전 평균 가격을 기준으로 판정하려면 아래 비교 가격 필드 적재가 필요합니다.
- 제철·가격 규칙에 따른 구매 권장 방향까지만 제공합니다. 재고·소비 속도·보관 가능 기간이 없으므로 구체적인 구매 수량·비축량은 계산하지 않습니다.

### 구매 신호 합산 초기안 (미구현·미확정)

규칙 데이터를 저장하는 구조는 유지하고 서버가 demand에 따라 신호 점수를 계산하는 초기안입니다. 하락률 구간과 demand는 실제 적재했으며 아래 합산 점수·결과 매핑은 아직 미확정·미구현입니다.

| 적용 신호 | demand | 합산 점수 초기안 |
| --- | --- | --- |
| 제철 시작 한 달 전 | UP | +1 |
| 제철인 모든 달 | STRONG_UP | +2 |
| 4주 가격 상승 주의 (부류 상위 20%, V27) | DOWN | -1 |
| 4주 가격 상승 경계·심각 (부류 상위 12.5%, V27) | STRONG_DOWN | -2 |
| 김장·설·추석 21~14일 전 | UP | +1 |
| 4주 가격 하락 주의 (부류 하위 20%, V27) | UP | +1 |
| 4주 가격 하락 경계·심각 (부류 하위 12.5%, V27) | STRONG_UP | +2 |

- 제철인 모든 달에 같은 STRONG_UP을 적용합니다. 시작 한 달 전은 실제 제철 구간이 시작되는 월의 직전 달만 연결하며 제철의 매달에 대해 연결하지 않습니다. 예: 7~8월 제철이면 시작 전 규칙은 7월 기준 하나만 연결해 6월에 적용합니다. 1월에 시작하는 제철의 직전 달은 12월입니다. 여러 제철 구간이 겹쳐도 같은 원자재의 같은 제철 신호를 중복 합산하지 않습니다.
- 가격 하락 계열은 같은 1주 비교에서 한 구간만 적용합니다. 4주 비교 가격은 우선 설명용 맥락으로 전달하고, 같은 가격 변화를 중복 가산하지 않습니다.
- 서로 다른 계열은 합산합니다. 예: 제철(+2) + 7% 가격 하락(+1) = +3, 제철(+2) + 12% 하락(+2) = +4입니다.
- 최종 점수가 2 이상이면 강력 구매 권장, 1이면 구매 권장, 0이면 판단 보류, 음수이면 구매 미권장으로 표시하는 초기안입니다. 판단 보류는 합산 결과 표시이며 ingredient_rule.demand에 새로운 값을 추가하지 않습니다.
- 적용 신호가 없거나 필요한 가격이 없으면 중립 점수로 단정하지 않고 근거 부족으로 표시합니다. 일부 근거만 있으면 적용한 신호와 누락된 근거를 함께 전달합니다.
- 점수는 구매 권장 강도를 표현하며, 강추 점수표의 원자재 영향 크기(신호 크기의 절댓값)로 쓰입니다.
- 강도 점수는 서버 정책의 초기안입니다. ingredient_rule 규칙은 V24 기준 32개이며 합산 점수 컬럼·테이블과 서버 계산 로직은 추가하지 않았습니다.
