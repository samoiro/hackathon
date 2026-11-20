-- 강추 순위를 근거 점수로 바꾸면서 정리한 규칙과 분석 작업 테이블.
-- 기준: ai-server/docs/API_CONTRACT_DRAFT.md "강추 고르기 (Java)", docs/database-schema.md

-- 1. 규칙 안 순서(sort_order)는 읽는 곳이 없고 뜻도 없어서 지운다. 순위는 서버가 근거 점수로 정한다.
alter table supply_rule_item drop column sort_order;
alter table ingredient_rule_item drop column sort_order;

-- 2. 켤 데이터가 없는 트리거의 규칙을 지운다. (연결은 cascade)
--    CROWD_SURGE: 혼잡도는 강추 계산에 쓰지 않는다. 같은 노션 행의 FESTIVAL·PERFORMANCE 규칙에 물품이 똑같이 있다.
--    SPORTS·TRAFFIC_CONTROL: 경기·마라톤 일정을 주는 API가 없고 날짜를 직접 관리하지 않기로 했다.
delete from supply_rule where trigger_code in ('CROWD_SURGE', 'SPORTS', 'TRAFFIC_CONTROL');
delete from supply_trigger where code in ('CROWD_SURGE', 'SPORTS', 'TRAFFIC_CONTROL');

-- 3. 규칙이 2개뿐이던 업종 채우기
create temporary table new_supply_rule (
    industry     text,
    trigger_code text,
    trigger_text text,
    lead_days    integer,
    supplies     text[]
) on commit drop;

insert into new_supply_rule values
    ('분식·김밥', 'SCHOOL_START', '개학(학원가)', 7, '{"포장 용기"}'),
    ('해물·조개구이', 'YEAR_END', '연말 송년회', 14, '{"숯","손님용 비닐장갑"}'),
    ('수선집', 'GRADUATION', '졸업(정장·교복 수선)', 14, '{"실","수선 봉투"}');

do $$
begin
    if exists (
        select 1 from new_supply_rule r
        cross join lateral unnest(r.supplies) as s(name)
        left join industry i on i.name = r.industry
        left join supply on supply.name = s.name
        where i.id is null or supply.id is null
    ) then
        raise exception '새 부자재 규칙의 업종 또는 물품이 없습니다';
    end if;
end $$;

insert into supply_rule (industry_id, trigger_code, trigger_text, forecast, lead_days_min, lead_days_max, demand)
select i.id, r.trigger_code, r.trigger_text, false, r.lead_days, r.lead_days, 'UP'
from new_supply_rule r
join industry i on i.name = r.industry;

insert into supply_rule_item (rule_id, supply_id, kind)
select sr.id, s.id, 'SUPPLY'
from new_supply_rule r
join industry i on i.name = r.industry
join supply_rule sr on sr.industry_id = i.id and sr.trigger_code = r.trigger_code
cross join lateral unnest(r.supplies) as x(name)
join supply s on s.name = x.name;

-- 4. 원자재 규칙: 4주 비교는 1주 비교와 같은 가격 변화를 두 번 세서 지운다. 4주 변화는 LLM이 change_4w_pct로 받는다.
delete from ingredient_rule where trigger_code = 'PRICE_BELOW_FOUR_WEEKS';

-- 크게 오른 품목이 1% 오른 품목과 같은 -1이 되지 않게 강한 미권장을 둔다.
alter table ingredient_rule drop constraint ingredient_rule_demand_check;
alter table ingredient_rule add constraint ingredient_rule_demand_check
    check (demand in ('UP', 'STRONG_UP', 'DOWN', 'STRONG_DOWN'));
comment on column ingredient_rule.demand is '구매 권장 방향: UP 권장, STRONG_UP 강력 권장, DOWN 미권장, STRONG_DOWN 강한 미권장. 판매 수요 예측이 아님';

-- 명절·김장 규칙만 며칠 전에 걸지 정한다. 날짜는 supply_trigger_date의 같은 행사 기간을 쓴다.
alter table ingredient_rule
    add column lead_days_min integer,
    add column lead_days_max integer,
    add constraint ingredient_rule_lead_days_pair check ((lead_days_min is null) = (lead_days_max is null)),
    add constraint ingredient_rule_lead_days_range check (lead_days_min between 0 and lead_days_max);
comment on column ingredient_rule.lead_days_min is '명절·김장 규칙의 최소 준비 일수. 제철·가격 규칙은 null';
comment on column ingredient_rule.lead_days_max is '명절·김장 규칙의 최대 준비 일수. 제철·가격 규칙은 null';

update ingredient_rule set trigger_text = '1주 전 대비 0% 초과 10% 미만 가격 상승'
where trigger_code = 'PRICE_RISE_ONE_WEEK';

insert into ingredient_rule (trigger_code, trigger_text, demand, lead_days_min, lead_days_max) values
    ('PRICE_RISE_ONE_WEEK_AT_LEAST_10_PERCENT', '1주 전 대비 10% 이상 가격 상승', 'STRONG_DOWN', null, null),
    ('BEFORE_KIMJANG', '김장철 전 가격 오르기 전 구매', 'UP', 14, 21),
    ('BEFORE_SEOLLAL', '설 전 가격 오르기 전 구매', 'UP', 14, 21),
    ('BEFORE_CHUSEOK', '추석 전 가격 오르기 전 구매', 'UP', 14, 21);

-- 가격 규칙은 다른 가격 규칙처럼 전체 원자재에 연결한다.
insert into ingredient_rule_item (rule_id, ingredient_id)
select r.id, i.id
from ingredient_rule r cross join ingredient i
where r.trigger_code = 'PRICE_RISE_ONE_WEEK_AT_LEAST_10_PERCENT';

create temporary table holiday_ingredient (trigger_code text, item_names text[]) on commit drop;
insert into holiday_ingredient values
    ('BEFORE_KIMJANG', '{"배추","무","고춧가루","건고추","깐마늘(국산)","파","생강","갓","새우젓","멸치액젓","굴"}'),
    ('BEFORE_SEOLLAL', '{"사과","배","수입조기","명태","계란","수입 소고기"}'),
    ('BEFORE_CHUSEOK', '{"사과","배","수입조기","명태","계란","수입 소고기"}');

do $$
begin
    if exists (
        select 1 from holiday_ingredient h
        cross join lateral unnest(h.item_names) as x(name)
        left join ingredient i on i.item_name = x.name
        where i.id is null
    ) then
        raise exception '명절·김장 원자재가 ingredient에 없습니다';
    end if;
end $$;

insert into ingredient_rule_item (rule_id, ingredient_id)
select r.id, i.id
from holiday_ingredient h
join ingredient_rule r on r.trigger_code = h.trigger_code
cross join lateral unnest(h.item_names) as x(name)
join ingredient i on i.item_name = x.name;

-- 5. 분석 작업. 분석이 1분쯤 걸려 POST로 시작하고 GET으로 상태를 다시 묻는다.
--    할 일은 로그인이 없어서 브라우저 localStorage에 둔다(서버 테이블 없음).
create table briefing_analysis_job (
    id                uuid primary key,
    status            text not null default 'PENDING' check (status in ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    steps             jsonb not null default '[]' check (jsonb_typeof(steps) = 'array'),
    base_time         timestamptz(0) not null,
    input_snapshot    jsonb not null check (jsonb_typeof(input_snapshot) = 'object'),
    evidence_snapshot jsonb check (jsonb_typeof(evidence_snapshot) = 'array'),
    final_result      jsonb check (jsonb_typeof(final_result) = 'object'),
    failure           jsonb check (jsonb_typeof(failure) = 'object'),
    score_version     text check (btrim(score_version) <> ''),
    created_datetime  timestamptz(0) not null default current_timestamp(0),
    updated_datetime  timestamptz(0) not null default current_timestamp(0),
    check (status <> 'SUCCEEDED' or final_result is not null),
    check (status <> 'FAILED' or failure is not null)
);

create trigger briefing_analysis_job_timestamps before update on briefing_analysis_job
for each row execute function batch_job_set_timestamps();

comment on table briefing_analysis_job is '브리핑 분석 작업과 결과. 점수 기준은 코드 상수이고 score_version으로 구분한다';
comment on column briefing_analysis_job.steps is '4단계 진행 상태 (AnalysisStep 배열)';
comment on column briefing_analysis_job.input_snapshot is '가게 정보·고른 품목·제외 목록 (AnalysisRequest)';
comment on column briefing_analysis_job.evidence_snapshot is '근거(E·H·W·R·S)와 근거별 급한 정도·영향 크기·확실성 점수. 왜 강추였는지 다시 보여줄 때 쓴다';
comment on column briefing_analysis_job.final_result is '브리핑 결과 (BriefingResponse). SUCCEEDED면 필수';
comment on column briefing_analysis_job.failure is '실패 정보 (AnalysisFailure). FAILED면 필수';
comment on column briefing_analysis_job.score_version is '강추 점수표 버전. 점수표는 서버 코드 상수';
