# 데이터베이스 테이블 구조

기준일: 2026-11-20. 연결된 PostgreSQL DB의 실제 컬럼·타입·제약조건을 조회해 반영했습니다.
현재 생성된 테이블은 업무용 11개와 관리용 4개입니다. 아래 미생성 설계안은 전달받은 문서를 보존한 것으로, 실제 DB에는 아직 없습니다.

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
| sort_order | smallint | 서버가 관리하는 고정 우선순위. 작은 값부터 우선 |

기본 키는 (rule_id, supply_id)입니다. 규칙 삭제 시 연결 행도 삭제됩니다. 이전 설명의 position은 실제 컬럼명 sort_order로 반영했습니다. 규칙 내 물품 우선순위는 서버가 이 값으로 정하며 LLM이 변경하지 않습니다.

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
| price | numeric(18,4), NULL 허용 | 대표 규격 기준 조사일 평균 가격. 0 이상 |
| created_datetime | timestamptz(0), 기본값 CURRENT_TIMESTAMP(0) | 이력 최초 생성 시각, 초 단위 |

- 기본 키는 (ingredient_id, observed_date)이며 별도의 id는 없습니다. 같은 재료·조사일을 중복 저장하지 않습니다.
- price는 ingredient.unit_size × ingredient.unit 기준입니다. 예: 500 × g 규격 가격입니다.
- API 가격 미제공 값은 NULL입니다. 실제 적재 코드는 대표 규격이 달라지면 적재를 중단합니다.
- 연결된 재료의 삭제와 ID 변경은 RESTRICT입니다.
- 기존 설계안의 collected_at은 현재 created_datetime입니다. price_basis_version, week1_price~week4_price, raw_payload는 현재 없습니다. 주차별 가격은 향후 설계 항목이며 현재는 조사일별 가격을 저장합니다.

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
| demand | text | 구매 권장 방향: UP / STRONG_UP / DOWN |

- 규칙에는 ingredient_id, industry_id, sort_order를 두지 않습니다. 같은 규칙을 여러 원자재가 공유하고, 같은 원자재도 여러 규칙에 연결될 수 있습니다.
- 제철 월은 IN_SEASON_MONTH_01~IN_SEASON_MONTH_12, 제철 시작 한 달 전은 BEFORE_SEASON_START_MONTH_01~12로 구분합니다. 제철의 첫 달과 나머지 달은 구분하지 않습니다. 가격 조건은 서버가 trigger_code별로 판정하며 trigger_text를 파싱하거나 LLM에 조건 충족 여부를 맡기지 않습니다.
- 별도 제철 달력·트리거 마스터 테이블은 만들지 않습니다. 서버가 현재 한국 시간의 월에 대응하는 규칙과 원자재별 가격 조건을 조회합니다.
- demand는 판매 수요나 소비 속도 예측이 아닌 서비스의 구매 권장 방향입니다. UP은 구매 권장, STRONG_UP은 강력 구매 권장, DOWN은 구매 미권장입니다. 같은 컬럼명을 쓰는 supply_rule의 수요 방향과 의미를 구분합니다.
- 제철 시작 한 달 전은 UP, 제철인 모든 달은 STRONG_UP입니다. 7~8월 제철이라면 6월은 UP, 7월·8월은 모두 STRONG_UP입니다. 가격 하락은 UP이며 1주 대비 10% 이상 하락은 STRONG_UP입니다. 가격 상승은 DOWN입니다.
- 제철과 가격을 묶은 복합 규칙은 만들지 않습니다. 서버가 개별 규칙의 구매 신호를 모아 최종 권장 강도를 계산하며 LLM에는 결과와 개별 근거를 함께 전달합니다.
- 가격 하락은 비용 절감에 따른 구매 권장, 가격 상승은 원가 부담에 따른 구매 미권장입니다. 미래 가격이나 판매 수요를 예측한 값이 아니며 미권장은 필수 구매 금지가 아닙니다.
- forecast, lead_days는 넣지 않습니다. 규칙만으로 사전 준비 일수가 정해지지 않기 때문입니다.

실제 DB에는 제철 24개(제철 월·시작 한 달 전 각 12개)와 가격 5개, 총 29개 규칙이 있습니다. 별도로 나눴던 제철 진입 월 규칙은 삭제했습니다. 규칙 초기 데이터는 V13__ingredient_rule_seed.sql과 V14__simplify_ingredient_season_rules.sql로 재현합니다. ingredient_rule_item은 실제 생성·적재했으며 조건 판정·추천 로직은 아직 미구현입니다.

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
- 수집/적재 절차: [server README](../server/README.md#문화행사-수집과-갱신). 거리 조회 예시: [query-cultural-events.sql](../server/scripts/query-cultural-events.sql).

### ingredient_rule_item — 규칙별 원자재

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| rule_id | bigint FK → ingredient_rule.id, 복합 PK | 추천 규칙 |
| ingredient_id | bigint FK → ingredient.id, 복합 PK | 규칙이 적용되는 원자재 |
| sort_order | smallint, 0보다 큼 | 해당 규칙 내 고정 우선순위. 작은 값부터 우선 |

- 기본 키는 (rule_id, ingredient_id)입니다. 규칙 삭제 시 연결 행도 삭제하며 원자재는 유지합니다.
- 업종은 규칙에 저장하지 않습니다. 가게가 선택한 원자재 또는 industry_ingredient로 추천 대상을 제한합니다.
- 서버는 연결된 원자재 중 조건을 충족하는 품목만 선택하고 sort_order로 정렬합니다. LLM은 우선순위를 변경하지 않습니다.
- sort_order는 같은 규칙 안에서 적용합니다. 규칙 간 숫자를 같은 우선순위로 간주하지 않습니다. 통합 목록이 필요하면 서버의 별도 규칙 간 정렬 기준을 정해야 합니다.
- 제철이 여러 달이면 해당 월의 규칙마다 같은 원자재를 연결합니다. 대표 품종·원산지·신선/냉동 상태에 적용되는 제철만 연결하고, 규칙이 없으면 제철 여부 미확인으로 전달합니다.

실제 연결은 총 740개입니다. 제철 원자재 31개에 제철 월 119개·시작 전 31개를 연결하고, 전체 원자재 118개에 가격 규칙 5개씩 590개를 연결했습니다. 대표 원자재는 V10__ingredient_seed.sql, 제철 근거 URL과 연결은 V17__ingredient_rule_item_seed.sql에 보존했습니다. CSV·JSON 사본은 제거했습니다.

- 초기 sort_order는 규칙별 품목 코드순으로 고정했습니다. 구매 효과나 가격 하락폭을 뜻하지 않으며, 데이터 재적재 시 기존 순서를 덮어쓰지 않습니다.
- 제철 없음·확인 필요·참고 범위·저장품 여부 확인 필요·정확한 월 경계 미확정인 87개는 제철 연결에서 제외했습니다. 가격 규칙 대상에는 포함됩니다.
- 원자재 삭제·ID 변경은 RESTRICT입니다. ingredient_id 역조회 인덱스가 있습니다.
- 연결은 적용 가능한 대상이며 조건 충족 결과가 아닙니다. 가격 상승·하락 규칙 모두에 연결되어 있어도 실제 신호는 서버의 조건 판정 후에만 사용합니다.

### 가격 배치 처리 방향 (미구현)

가격 수집 배치는 ingredient_price_history의 같은 원자재·조사일을 upsert하고, 수집 성공 후 최신 가격과 비교 가격으로 조건을 판정하는 방식으로 구성합니다. ingredient_rule_item의 연결·sort_order와 ingredient_rule의 demand는 매번 바꾸지 않습니다. 가격 하락은 UP, 10% 이상 하락은 STRONG_UP, 가격 상승은 DOWN입니다.

판정 결과는 요청 시 조회하거나 향후 결과 저장이 필요할 때 별도로 저장합니다. 현재 ingredient_rule_item에 활성 여부·조사일·판정값 컬럼은 추가하지 않았고, 자동 실행·조건 판정·결과 저장 배치는 등록하지 않았습니다. API의 1주·4주 전 평균 가격을 사용할 때는 문서 마지막의 비교 가격 필드 적재가 먼저 필요합니다. 날짜별 이력의 7일·28일 전 가격을 API 평균 가격과 혼용하지 않습니다. 결측·0 기준 가격은 판정을 건너뛰고, 수집 실패 시 오래된 결과를 현재 결과로 표시하지 않습니다.

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
- V18__batch_job_history.sql을 연결된 DB에 수동 적용했습니다. 검증은 server/scripts/check-batch-job-history.sql로 수행하고 테스트 데이터를 롤백합니다.

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

## 아직 생성하지 않은 테이블 설계안

다음은 전달받은 설계안입니다. 타입·관계·상태값은 향후 구현 시 검토해야 하며 현재 DB 구조로 해석하지 않습니다.
원자재는 ingredient_rule에 제철·가격 조건을 저장하고, ingredient_rule_item에서 원자재를 다대다로 연결합니다. 규칙 내 우선순위는 서버가 연결 행의 sort_order로 확정하며, 적용 규칙과 가격 데이터를 LLM에 전달해 추천 이유와 설명을 작성합니다.

### industry_ingredient — 업종별 원자재 (미생성)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| industry_id | bigint FK, 복합 PK | 업종 |
| ingredient_id | bigint FK, 복합 PK | 추천 원자재 |

### industry_supply — 업종별 부자재 (미생성)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| industry_id | bigint FK, 복합 PK | 업종 |
| supply_id | bigint FK, 복합 PK | 추천 부자재 |
| order_cycle_note | varchar, NULL | 일반 주문 주기 |
| source_id | bigint FK | 추천 품목·주기의 근거 자료 |

### 원자재 규칙 예시 데이터

아래 ingredient_rule은 실제 적재한 29개 규칙이며, ingredient_rule_item은 실제 DB에서 추출한 딸기(설향) 연결 일부입니다.

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
| PRICE_RISE_ONE_WEEK | 조사일 가격 > 1주 전 평균 가격 | DOWN: 구매 미권장 |
| PRICE_BELOW_FOUR_WEEKS | 조사일 가격 < 4주 전 평균 가격 | UP: 구매 권장 |
| BEFORE_SEASON_START_MONTH_07 | 현재 한국 시간 기준 6월 | UP: 제철 시작 한 달 전 |
| PRICE_DROP_ONE_WEEK_5_TO_10_PERCENT | 5% ≤ 1주 대비 하락률 < 10% | UP: 구매 권장 |
| PRICE_DROP_ONE_WEEK_AT_LEAST_10_PERCENT | 1주 대비 하락률 ≥ 10% | STRONG_UP: 강력 구매 권장 |

가격 조건은 각 원자재의 동일 대표 규격끼리 비교하며, 비교 가격은 0보다 크고 두 가격 모두 유효해야 합니다. 가격이 같으면 상승·하락 규칙은 적용하지 않습니다.
하락률은 (1주 전 평균 가격 − 조사일 가격) ÷ 1주 전 평균 가격 × 100입니다. 구간의 5%·10%는 초기 운영 기준으로 규칙 데이터에 적재했습니다. 같은 비교 기간의 하락 구간은 정확히 하나만 적용해야 하며 실제 조건 판정 코드는 아직 미구현입니다.
예를 들어 규칙 37에서 일부 원자재만 해당 하락 구간에 속한다면 서버는 그 원자재만 선택하고 저장된 sort_order순으로 전달합니다. 상승·하락 규칙에 같은 원자재가 연결되어 있어도 같은 비교 가격에서는 동시에 충족되지 않습니다.

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

### task_priority_policy — 우선순위 평가 정책 (미생성)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint PK | 정책 ID |
| version | varchar UNIQUE | 정책 버전 |
| weights | jsonb | 평가 항목별 가중치 |
| score_min / score_max | numeric | 평가 항목의 최소·최대 점수 |
| tie_breakers | jsonb | 동점일 때 정렬 기준 |
| active | boolean | 적용 중 여부 |

### task_priority_score_rule — 우선순위 점수 규칙 (미생성)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | bigint PK | 점수 규칙 ID |
| policy_id | bigint FK | 적용 정책 |
| criterion | varchar | 평가 항목 |
| condition_type | varchar | 평가 조건 유형 |
| condition_params | jsonb | 조건별 수치·설정 |
| score | numeric | 조건 충족 시 부여할 점수 |
| reason_template | varchar | 평가 이유 설명 문구 |

원자재의 가격·제철 규칙은 LLM 설명의 입력 근거입니다. 이 공통 점수 정책 테이블은 별도의 전체 작업 평가 설계이며 제철 규칙 저장에 필수는 아닙니다. 적용하더라도 계산 주체는 서버이며 규칙 내 sort_order를 LLM이 덮어쓰지 않습니다. 입력 방식은 문서 마지막의 원자재 우선순위 설계를 따릅니다.

### briefing_analysis_job — 분석 작업 및 결과 (미생성)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid PK | 분석 작업 ID |
| access_token_hash | varchar | 작업 조회·재시도용 토큰 해시 |
| input_snapshot | jsonb | 분석에 사용한 가게 설정·선택 품목 |
| evidence_snapshot | jsonb | 분석에 사용한 행사·가격·준비 규칙 |
| policy_id | bigint FK | 적용한 우선순위 정책 |
| status | varchar | 전체 진행 상태 |
| stages | jsonb | 단계별 진행 상태 |
| intermediate_result | jsonb, NULL | 중간 분석 결과 |
| final_result | jsonb, NULL | 최종 추천 결과 |
| error_summary | text, NULL | 실패 요약 |
| created_at / expires_at | timestamptz | 생성·만료 시각 |

## 원자재 우선순위 설계 (미구현)

가게가 선택한 원자재 또는 industry_ingredient로 연결된 원자재를 대상으로, 제철·가격 조건을 충족한 ingredient_rule과 가격 데이터를 LLM에 함께 전달합니다.
예를 들어 7월이면 IN_SEASON_MONTH_07 규칙을 조회하고 ingredient_rule_item의 ingredient_id로 대표 품목 및 가격을 연결합니다. 가격 규칙은 연결된 원자재마다 서버가 가격을 비교해 충족 여부를 판정합니다.
입력에는 분석 기준일, 원자재 ID·이름·대표 규격, 조사일, 조사일 가격, 1주·4주 전 가격과 변화율, 규칙의 코드·설명·구매 권장 방향(demand)과 서버가 정한 우선순위를 포함합니다.
서버는 날짜·대상 품목 조회, 변화율 계산, 개별 구매 신호 합산 및 ingredient_rule_item.sort_order 기준 규칙 내 정렬을 맡습니다. LLM은 서버가 확정한 순위·개별 신호·최종 권장 강도를 바꾸지 않고 가격·제철 근거를 설명합니다.
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
- 가격 변화와 제철 규칙은 추천 이유를 설명하는 근거로 사용하며 저장된 sort_order를 변경하지 않습니다. 초기에는 제철 전용 점수표·가중치 테이블을 추가하지 않습니다.
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
| 1주 전 대비 가격 상승 | DOWN | -1 |
| 1주 전 대비 0% 초과 5% 미만 하락 | UP | +1 |
| 1주 전 대비 5% 이상 10% 미만 하락 | UP | +1 |
| 1주 전 대비 10% 이상 하락 | STRONG_UP | +2 |

- 제철인 모든 달에 같은 STRONG_UP을 적용합니다. 시작 한 달 전은 실제 제철 구간이 시작되는 월의 직전 달만 연결하며 제철의 매달에 대해 연결하지 않습니다. 예: 7~8월 제철이면 시작 전 규칙은 7월 기준 하나만 연결해 6월에 적용합니다. 1월에 시작하는 제철의 직전 달은 12월입니다. 여러 제철 구간이 겹쳐도 같은 원자재의 같은 제철 신호를 중복 합산하지 않습니다.
- 가격 하락 계열은 같은 1주 비교에서 한 구간만 적용합니다. 4주 비교 가격은 우선 설명용 맥락으로 전달하고, 같은 가격 변화를 중복 가산하지 않습니다.
- 서로 다른 계열은 합산합니다. 예: 제철(+2) + 7% 가격 하락(+1) = +3, 제철(+2) + 12% 하락(+2) = +4입니다.
- 최종 점수가 2 이상이면 강력 구매 권장, 1이면 구매 권장, 0이면 판단 보류, 음수이면 구매 미권장으로 표시하는 초기안입니다. 판단 보류는 합산 결과 표시이며 ingredient_rule.demand에 새로운 값을 추가하지 않습니다.
- 적용 신호가 없거나 필요한 가격이 없으면 중립 점수로 단정하지 않고 근거 부족으로 표시합니다. 일부 근거만 있으면 적용한 신호와 누락된 근거를 함께 전달합니다.
- 점수는 구매 권장 강도를 표현합니다. 저장된 sort_order를 수정하거나 품목 우선순위를 재정렬하는 값으로 사용하지 않습니다.
- 강도 점수는 서버 정책의 초기안입니다. ingredient_rule과 규칙 29개는 실제 DB에 반영했으며 합산 점수 컬럼·테이블과 서버 계산 로직은 추가하지 않았습니다.

### 가격 이력에 추가할 필드 (미반영)

기존 ingredient_price_history 컬럼에 아래 두 필드를 추가하는 설계입니다. 현재 DB와 적재 코드는 아직 조사일 가격만 저장합니다.

| 컬럼 | 타입 | 설명 | 원본 API 필드 |
| --- | --- | --- | --- |
| one_week_before_price | numeric(18,4), NULL 허용 | 같은 대표 규격의 1주 전 평균 가격 | ww1_bfr_avg_prc |
| four_weeks_before_price | numeric(18,4), NULL 허용 | 같은 대표 규격의 4주 전 평균 가격 | ww4_bfr_avg_prc |

각 값은 해당 observed_date의 API 응답이 제공한 비교 가격입니다. 이를 단순히 7일·28일 전 날짜의 실측 가격 행으로 새로 만들지 않습니다.
향후 반영 시 DB 마이그레이션, API 파싱·검증, 적재 SQL, 테스트를 함께 수정합니다. 기존 이력은 같은 조사일 API 응답으로 확인 가능한 경우에만 역채움하며 미확인 값은 NULL로 둡니다.
