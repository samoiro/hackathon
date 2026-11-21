# ERD 요약

## 현재 업무 테이블

### 업종 — industry

| 한글명 | 영문 필드명 |
| --- | --- |
| 업종 ID | id |
| 상위 분류 | category |
| 업종명 | name |

### 부자재·소모품 및 판매 상품 — supply

| 한글명 | 영문 필드명 |
| --- | --- |
| 물품 ID | id |
| 물품명 | name |

### 준비 유발 상황 — supply_trigger

| 한글명 | 영문 필드명 |
| --- | --- |
| 상황 코드 | code |
| 상황명 | label |
| 상황 유형 | type |

### 부자재 준비 규칙 — supply_rule

| 한글명 | 영문 필드명 |
| --- | --- |
| 규칙 ID | id |
| 업종 ID | industry_id |
| 상황 코드 | trigger_code |
| 상황 설명 | trigger_text |
| 날씨 예보 기준 여부 | forecast |
| 최소 준비 일수 | lead_days_min |
| 최대 준비 일수 | lead_days_max |
| 수요 방향 | demand |

### 규칙별 물품 — supply_rule_item

| 한글명 | 영문 필드명 |
| --- | --- |
| 규칙 ID | rule_id |
| 물품 ID | supply_id |
| 물품 구분 | kind |

### 대표 원자재 — ingredient

| 한글명 | 영문 필드명 |
| --- | --- |
| 원자재 ID | id |
| 품목 코드 | item_code |
| 품목명 | item_name |
| 부류 코드 | category_code |
| 부류명 | category_name |
| 거래 구분 코드 | trade_type_code |
| 거래 구분명 | trade_type_name |
| 품종 코드 | variety_code |
| 품종명 | variety_name |
| 등급 코드 | grade_code |
| 등급명 | grade_name |
| 판매 단위 | unit |
| 판매 단위 크기 | unit_size |

### 원자재 가격 이력 — ingredient_price_history

| 한글명 | 영문 필드명 |
| --- | --- |
| 원자재 ID | ingredient_id |
| 가격 조사일 | observed_date |
| 평균 가격 | price |
| 직전 관측 가격 | previous_day_price |
| 직전 관측 대비 변동률(%) | daily_change_rate |
| 가격 흐름(UP/DOWN/FLAT) | price_trend |
| 1주 전 평균 가격 | one_week_before_price |
| 2주 전 평균 가격 | two_weeks_before_price |
| 3주 전 평균 가격 | three_weeks_before_price |
| 4주 전 평균 가격 | four_weeks_before_price |
| 최초 생성 시각 | created_datetime |

### 도시데이터 지원 지역 — city_data_area

| 한글명 | 영문 필드명 |
| --- | --- |
| 지역 코드 | code |
| 지역명 | name |
| 지역 분류 | category |
| 지역 경계 | boundary |
| 지도 표시 좌표 | coord |
| 생성 시각 | created_datetime |

### 원자재 추천 규칙 — ingredient_rule

| 한글명 | 영문 필드명 |
| --- | --- |
| 규칙 ID | id |
| 조건 코드 | trigger_code |
| 조건 설명 | trigger_text |
| 구매 권장 방향 | demand |
| 최소 준비 일수 | lead_days_min |
| 최대 준비 일수 | lead_days_max |

### 오프라인 문화행사 — cultural_event

| 한글명 | 영문 필드명 |
| --- | --- |
| 행사 ID | id |
| 문화포털 고유번호 | source_key |
| 행사명 | title |
| 분류 / 자치구 | category / district_name |
| 장소 | place |
| 시작일 / 종료일 | start_date / end_date |
| 운영시간 | time_text |
| 필수 공간 좌표(WGS84 Point, 경도·위도) | coord |
| 이용 대상 | target_audience |
| 프로그램 / 안내 | program / notes |
| 요금 / 무료 여부 | fee_text / is_free |
| 원문 URL / 이미지 | source_url / image_url |
| 활성 여부 | is_active |
| 원본 응답 | raw_payload |
| 최초 생성 / 내용 수정 시각 | created_datetime / updated_datetime |

### 규칙별 원자재 — ingredient_rule_item

| 한글명 | 영문 필드명 |
| --- | --- |
| 규칙 ID | rule_id |
| 원자재 ID | ingredient_id |

### 원자재 가격 단계 경계 — ingredient_price_band

| 한글명 | 영문 필드명 |
| --- | --- |
| 부류 | category_name |
| 표본 수 | sample_count |
| 하락 심각 경계 (하위 5%) | p5 |
| 하락 경계 경계 (하위 12.5%) | p12_5 |
| 하락 주의 경계 (하위 20%) | p20 |
| 상승 주의 경계 (상위 20%) | p80 |
| 상승 경계 경계 (상위 12.5%) | p87_5 |
| 상승 심각 경계 (상위 5%) | p95 |
| 자료 기간 | sample_period |

### 업종별 추천 원자재 — industry_ingredient

| 한글명 | 영문 필드명 |
| --- | --- |
| 업종 ID | industry_id |
| 원자재 ID | ingredient_id |
| 고정 추천 순서 (1~3, 업종별 최대 3개) | sort_order |
| 업종명 | industry_name |
| 원자재명 | ingredient_name |

### 업종별 추천 부자재 — industry_supply

| 한글명 | 영문 필드명 |
| --- | --- |
| 업종 ID | industry_id |
| 부자재 ID | supply_id |
| 고정 추천 순서 (1~2, 업종별 최대 2개) | sort_order |
| 업종명 | industry_name |
| 부자재명 | supply_name |

### 분석 작업과 결과 — briefing_analysis_job

| 한글명 | 영문 필드명 |
| --- | --- |
| 분석 번호 | id |
| 진행 상태 | status |
| 단계별 상태 | steps |
| 기준 시각 | base_time |
| 가게 설정·선택 품목 | input_snapshot |
| 근거와 점수 | evidence_snapshot |
| 브리핑 결과 | final_result |
| 실패 정보 | failure |
| 점수표 버전 | score_version |
| 생성 시각 | created_datetime |
| 변경 시각 | updated_datetime |

### 추천 카드 피드백 — suggestion_feedback

| 한글명 | 영문 필드명 |
| --- | --- |
| 피드백 ID | id |
| 분석 번호 | analysis_id |
| 업종 ID | industry_id |
| 카드 키 | card_key |
| 묶음 | category |
| 할 일 유형 | task_type |
| 품목 ID | item_id |
| 피드백 | vote |
| 생성 시각 | created_datetime |
| 변경 시각 | updated_datetime |

### 지원사업 공고 — support_program

| 한글명 | 영문 필드명 |
| --- | --- |
| 공고 ID (pblancId) | id |
| 공고명 | title |
| 공고 URL | url |
| 소관기관명 | agency_name |
| 지원 분야 / 지원 대상 | field_name / target_name |
| 해시태그 | hashtags |
| 신청 기간 원문 / 마감일 | application_period / deadline |
| 등록 일시 | registered_datetime |
| 수집 일시 | created_datetime |

## 현재 관리 테이블

### 작업·재시도 관리 — batch_job

| 한글명 | 영문 필드명 |
| --- | --- |
| 작업 ID | id |
| 수집 작업명 | job_name |
| 실행 조건. 비밀값 저장 금지 | job_parameters |
| 기준일. 원자재 가격은 기준일 전날 하루를 받음 | target_date |
| PENDING / RUNNING / SUCCEEDED / FAILED | status |
| 현재 재시도 횟수. 최초 실행 0 | retry_times |
| 생성 / 변경 시각 | created_datetime / updated_datetime |

### 실행별 이력 — batch_job_history

| 한글명 | 영문 필드명 |
| --- | --- |
| 실행 이력 ID | id |
| 작업과 1:N. 삭제·ID 변경 RESTRICT | batch_job_id |
| 해당 실행의 재시도 차수. 최초 실행 0 | retry_times |
| RUNNING / SUCCEEDED / FAILED | status |
| 시작 시각. 소요시간의 밀리초 정밀도 유지 | started_datetime |
| 종료 시각. 종료 상태에서는 필수 | finished_datetime |
| 종료 − 시작의 밀리초. 실행 중 NULL | running_time |
| 오류 분류 / 비밀값을 제거한 원인 | error_code / error_message |
| 읽기 / 반영 완료 / 제외 건수 | read_count / write_count / skip_count |
| 생성 / 변경 시각 | created_datetime / updated_datetime |

### DB 구조 변경 이력 — flyway_schema_history

| 한글명 | 영문 필드명 |
| --- | --- |
| 적용 순서 | installed_rank |
| 버전 | version |
| 변경 설명 | description |
| 변경 유형 | type |
| 실행 파일명 | script |
| 체크섬 | checksum |
| 실행 사용자 | installed_by |
| 적용 시각 | installed_on |
| 실행 시간 | execution_time |
| 성공 여부 | success |

### 좌표계 정의 — spatial_ref_sys

| 한글명 | 영문 필드명 |
| --- | --- |
| 좌표계 식별자 | srid |
| 정의 기관명 | auth_name |
| 기관 좌표계 코드 | auth_srid |
| WKT 좌표계 정의 | srtext |
| PROJ 좌표계 정의 | proj4text |

## 미생성 설계안

### 근거 자료 — reference_source

| 한글명 | 영문 필드명 |
| --- | --- |
| 출처 ID | id |
| 출처 코드 | code |
| 자료 버전 | version |
| 자료 유형 | kind |
| 자료명 | title |
| 발행 기관·팀 | publisher |
| 자료 URL | url |
| 자료 원문 | content |
| 검토 상태 | review_status |

