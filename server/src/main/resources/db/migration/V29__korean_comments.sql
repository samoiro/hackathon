-- 테이블·컬럼 comment는 한글 이름으로 통일한다
comment on table batch_job is '배치 작업';
comment on table batch_job_history is '배치 작업 이력';
comment on table briefing_analysis_job is '브리핑 분석 작업';
comment on table city_data_area is '도시데이터 지역';
comment on table cultural_event is '문화 행사';
comment on table industry is '업종';
comment on table industry_ingredient is '업종별 추천 원자재';
comment on table industry_supply is '업종별 추천 부자재';
comment on table ingredient is '원자재';
comment on table ingredient_price_band is '원자재 가격 변동 구간';
comment on table ingredient_price_history is '원자재 가격 이력';
comment on table ingredient_rule is '원자재 추천 규칙';
comment on table ingredient_rule_item is '원자재 추천 규칙 대상';
comment on table suggestion_feedback is '추천 카드 피드백';
comment on table supply is '부자재';
comment on table supply_rule is '부자재 준비 규칙';
comment on table supply_rule_item is '부자재 준비 규칙 품목';
comment on table supply_trigger is '부자재 준비 트리거';
comment on table supply_trigger_date is '부자재 트리거 기간';

comment on column batch_job.id is 'ID';
comment on column batch_job.job_name is '작업명';
comment on column batch_job.job_parameters is '작업 파라미터';
comment on column batch_job.status is '상태';
comment on column batch_job.retry_times is '재시도 횟수';
comment on column batch_job.created_datetime is '생성 일시';
comment on column batch_job.updated_datetime is '수정 일시';

comment on column batch_job_history.id is 'ID';
comment on column batch_job_history.batch_job_id is '배치 작업 ID';
comment on column batch_job_history.retry_times is '재시도 차수';
comment on column batch_job_history.status is '상태';
comment on column batch_job_history.started_datetime is '시작 일시';
comment on column batch_job_history.finished_datetime is '종료 일시';
comment on column batch_job_history.running_time is '소요 시간(ms)';
comment on column batch_job_history.error_code is '에러 코드';
comment on column batch_job_history.error_message is '에러 메시지';
comment on column batch_job_history.read_count is '읽은 건수';
comment on column batch_job_history.write_count is '저장 건수';
comment on column batch_job_history.skip_count is '건너뛴 건수';
comment on column batch_job_history.created_datetime is '생성 일시';
comment on column batch_job_history.updated_datetime is '수정 일시';

comment on column briefing_analysis_job.id is 'ID';
comment on column briefing_analysis_job.status is '상태';
comment on column briefing_analysis_job.steps is '진행 단계';
comment on column briefing_analysis_job.base_time is '기준 시각';
comment on column briefing_analysis_job.input_snapshot is '입력 스냅샷';
comment on column briefing_analysis_job.evidence_snapshot is '근거 스냅샷';
comment on column briefing_analysis_job.final_result is '최종 결과';
comment on column briefing_analysis_job.failure is '실패 정보';
comment on column briefing_analysis_job.score_version is '점수표 버전';
comment on column briefing_analysis_job.created_datetime is '생성 일시';
comment on column briefing_analysis_job.updated_datetime is '수정 일시';

comment on column city_data_area.code is '지역 코드';
comment on column city_data_area.name is '지역명';
comment on column city_data_area.category is '지역 분류';
comment on column city_data_area.boundary is '영역';
comment on column city_data_area.coord is '영역 좌표';
comment on column city_data_area.created_datetime is '생성 일시';

comment on column cultural_event.id is 'ID';
comment on column cultural_event.source_key is '원본 키';
comment on column cultural_event.title is '행사명';
comment on column cultural_event.category is '분류';
comment on column cultural_event.district_name is '자치구명';
comment on column cultural_event.place is '장소';
comment on column cultural_event.start_date is '시작일';
comment on column cultural_event.end_date is '종료일';
comment on column cultural_event.time_text is '시간 안내';
comment on column cultural_event.coord is '좌표';
comment on column cultural_event.target_audience is '이용 대상';
comment on column cultural_event.program is '프로그램';
comment on column cultural_event.notes is '기타 내용';
comment on column cultural_event.fee_text is '이용 요금';
comment on column cultural_event.is_free is '무료 여부';
comment on column cultural_event.source_url is '원본 URL';
comment on column cultural_event.image_url is '이미지 URL';
comment on column cultural_event.is_active is '활성 여부';
comment on column cultural_event.raw_payload is '원본 응답';
comment on column cultural_event.created_datetime is '생성 일시';
comment on column cultural_event.updated_datetime is '수정 일시';

comment on column error_code.code is '에러 코드';
comment on column error_code.http_status is 'HTTP 상태 코드';
comment on column error_code.message is '기본 메시지';

comment on column error_log.id is 'ID';
comment on column error_log.error_code is '에러 코드';
comment on column error_log.http_status is 'HTTP 상태 코드';
comment on column error_log.http_method is 'HTTP 메서드';
comment on column error_log.request_path is '요청 경로';
comment on column error_log.message is '에러 메시지';
comment on column error_log.exception_class is '예외 클래스';
comment on column error_log.stack_trace is '스택 트레이스';
comment on column error_log.created_datetime is '생성 일시';

comment on column industry.id is 'ID';
comment on column industry.category is '업종 분류';
comment on column industry.name is '업종명';

comment on column industry_ingredient.industry_id is '업종 ID';
comment on column industry_ingredient.ingredient_id is '원자재 ID';
comment on column industry_ingredient.sort_order is '정렬 순서';
comment on column industry_ingredient.industry_name is '업종명';
comment on column industry_ingredient.ingredient_name is '원자재명';

comment on column industry_supply.industry_id is '업종 ID';
comment on column industry_supply.supply_id is '부자재 ID';
comment on column industry_supply.sort_order is '정렬 순서';
comment on column industry_supply.industry_name is '업종명';
comment on column industry_supply.supply_name is '부자재명';

comment on column ingredient.id is 'ID';
comment on column ingredient.item_code is '품목 코드';
comment on column ingredient.item_name is '품목명';
comment on column ingredient.category_name is '부류명';
comment on column ingredient.category_code is '부류 코드';
comment on column ingredient.trade_type_code is '거래 구분 코드';
comment on column ingredient.trade_type_name is '거래 구분명';
comment on column ingredient.variety_code is '품종 코드';
comment on column ingredient.variety_name is '품종명';
comment on column ingredient.grade_code is '등급 코드';
comment on column ingredient.grade_name is '등급명';
comment on column ingredient.unit is '단위';
comment on column ingredient.unit_size is '단위 크기';

comment on column ingredient_price_band.category_name is '부류명';
comment on column ingredient_price_band.sample_count is '표본 수';
comment on column ingredient_price_band.p5 is '하위 5% 경계';
comment on column ingredient_price_band.p12_5 is '하위 12.5% 경계';
comment on column ingredient_price_band.p20 is '하위 20% 경계';
comment on column ingredient_price_band.p80 is '상위 20% 경계';
comment on column ingredient_price_band.p87_5 is '상위 12.5% 경계';
comment on column ingredient_price_band.p95 is '상위 5% 경계';
comment on column ingredient_price_band.sample_period is '표본 기간';

comment on column ingredient_price_history.ingredient_id is '원자재 ID';
comment on column ingredient_price_history.observed_date is '조사일';
comment on column ingredient_price_history.price is '가격';
comment on column ingredient_price_history.created_datetime is '생성 일시';
comment on column ingredient_price_history.previous_day_price is '직전 조사일 가격';
comment on column ingredient_price_history.one_week_before_price is '1주 전 평균 가격';
comment on column ingredient_price_history.two_weeks_before_price is '2주 전 평균 가격';
comment on column ingredient_price_history.three_weeks_before_price is '3주 전 평균 가격';
comment on column ingredient_price_history.four_weeks_before_price is '4주 전 평균 가격';
comment on column ingredient_price_history.daily_change_rate is '직전 대비 변동률';
comment on column ingredient_price_history.price_trend is '가격 추세';

comment on column ingredient_rule.id is 'ID';
comment on column ingredient_rule.trigger_code is '트리거 코드';
comment on column ingredient_rule.trigger_text is '트리거 설명';
comment on column ingredient_rule.demand is '구매 권장 방향';
comment on column ingredient_rule.lead_days_min is '최소 준비 일수';
comment on column ingredient_rule.lead_days_max is '최대 준비 일수';

comment on column ingredient_rule_item.rule_id is '규칙 ID';
comment on column ingredient_rule_item.ingredient_id is '원자재 ID';

comment on column suggestion_feedback.id is 'ID';
comment on column suggestion_feedback.analysis_id is '분석 ID';
comment on column suggestion_feedback.industry_id is '업종 ID';
comment on column suggestion_feedback.card_key is '카드 키';
comment on column suggestion_feedback.category is '카드 묶음';
comment on column suggestion_feedback.task_type is '할 일 유형';
comment on column suggestion_feedback.item_id is '품목 ID';
comment on column suggestion_feedback.vote is '투표';
comment on column suggestion_feedback.created_datetime is '생성 일시';
comment on column suggestion_feedback.updated_datetime is '수정 일시';

comment on column supply.id is 'ID';
comment on column supply.name is '부자재명';

comment on column supply_rule.id is 'ID';
comment on column supply_rule.industry_id is '업종 ID';
comment on column supply_rule.trigger_code is '트리거 코드';
comment on column supply_rule.trigger_text is '트리거 원문';
comment on column supply_rule.forecast is '예보 기준 여부';
comment on column supply_rule.lead_days_min is '최소 준비 일수';
comment on column supply_rule.lead_days_max is '최대 준비 일수';
comment on column supply_rule.demand is '수요 방향';

comment on column supply_rule_item.rule_id is '규칙 ID';
comment on column supply_rule_item.supply_id is '부자재 ID';
comment on column supply_rule_item.kind is '품목 종류';

comment on column supply_trigger.code is '트리거 코드';
comment on column supply_trigger.label is '트리거명';
comment on column supply_trigger.type is '트리거 유형';

comment on column supply_trigger_date.trigger_code is '트리거 코드';
comment on column supply_trigger_date.start_date is '시작일';
comment on column supply_trigger_date.end_date is '종료일';
