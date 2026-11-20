-- 원자재 가격 신호를 정부 농산물 수급조절매뉴얼 방식으로 바꾼다.
-- 근거: 수급조절매뉴얼은 가격 수준을 안정 / 상승·하락 주의·경계·심각 7단계로 나누고,
--       상승 구간은 가격 분포의 상위 비율로 정한다(배추·무·건고추: 심각 5%, 경계 12.5%. 주의는 품목별, 감자 수미 15~20%).
--       평년은 최근 5년 같은 시기 가격 중 최고·최저를 뺀 3년 평균이다(KAMIS·KREI 공식 정의).
--       (KREI 「감자 수급조절매뉴얼 도입 및 운영」 2020, 표 3-2 · 표 3-10 · 부표 3)
-- 하락 구간은 정부가 생산비·경영비로 정하지만 원자재 118개의 비용 자료가 없어서 상승과 같은 분위수를 거꾸로 쓴다.
-- 고정 % 규칙(5%·10%)은 품목마다 평소 변동폭이 달라 채소는 자주, 쌀·가공식품은 거의 안 걸려서 지운다.
-- 기준: docs/database-schema.md "ingredient_price_band" (자료 2019-01~2026-11, aT 가격 추이 API)

-- 1. 부류별 분위수 경계. 서버는 이 표로 단계를 판정하고, 값은 매년 초 다시 계산해 이 표만 고친다.
create table ingredient_price_band (
    basis          text    not null check (basis in ('NORMAL_RATIO', 'WEEKLY_CHANGE')),
    category_name  text    not null check (btrim(category_name) <> ''),
    sample_count   integer not null check (sample_count > 0),
    p5             numeric(8, 4) not null,
    p12_5          numeric(8, 4) not null,
    p20            numeric(8, 4) not null,
    p80            numeric(8, 4) not null,
    p87_5          numeric(8, 4) not null,
    p95            numeric(8, 4) not null,
    sample_period  text    not null,
    primary key (basis, category_name),
    check (p5 <= p12_5 and p12_5 <= p20 and p20 <= p80 and p80 <= p87_5 and p87_5 <= p95)
);

comment on table ingredient_price_band is '원자재 부류별 가격 단계 경계(분위수). 수급조절매뉴얼의 상승 구간 확률분포 할당 방식';
comment on column ingredient_price_band.basis is 'NORMAL_RATIO: 조사일 가격 ÷ 평년 가격. WEEKLY_CHANGE: 조사일 가격 ÷ 1주 전 평균 가격 − 1 (평년을 못 구한 품목에만 씀)';
comment on column ingredient_price_band.category_name is 'ingredient.category_name (식량작물·채소류·과일류·특용작물·축산물·수산물·식품)';
comment on column ingredient_price_band.p5 is '하위 5%: 하락 심각 경계';
comment on column ingredient_price_band.p12_5 is '하위 12.5%: 하락 경계';
comment on column ingredient_price_band.p20 is '하위 20%: 하락 주의';
comment on column ingredient_price_band.p80 is '상위 20%: 상승 주의';
comment on column ingredient_price_band.p87_5 is '상위 12.5%: 상승 경계';
comment on column ingredient_price_band.p95 is '상위 5%: 상승 심각';
comment on column ingredient_price_band.sample_period is '분위수를 계산한 자료 기간과 표본 방식';

-- 2. 고정 % 가격 규칙을 지운다. 연결(ingredient_rule_item)은 ON DELETE CASCADE로 함께 지워진다.
delete from ingredient_rule where trigger_code in (
    'PRICE_DROP_ONE_WEEK_UNDER_5_PERCENT',
    'PRICE_DROP_ONE_WEEK_5_TO_10_PERCENT',
    'PRICE_DROP_ONE_WEEK_AT_LEAST_10_PERCENT',
    'PRICE_RISE_ONE_WEEK',
    'PRICE_RISE_ONE_WEEK_AT_LEAST_10_PERCENT');

-- 3. 단계 규칙. 한 품목에는 가격 규칙이 1개만 걸린다: 평년을 구할 수 있으면 NORMAL, 없으면 ONE_WEEK.
--    가격이 오르면 원가 부담이라 구매 미권장, 내리면 구매 권장(기존 demand 뜻과 같음). 심각·경계는 같은 강도로 본다.
--    하락 심각에는 농산물가격안정제 기준가격(평년 도매가격의 80%, 2026.9 농식품부)도 함께 쓴다.
insert into ingredient_rule (trigger_code, trigger_text, demand) values
    ('PRICE_ABOVE_NORMAL_SEVERE', '평년보다 크게 비쌈 (상승 심각: 부류 상위 5%)', 'STRONG_DOWN'),
    ('PRICE_ABOVE_NORMAL_ALERT', '평년보다 비쌈 (상승 경계: 부류 상위 12.5%)', 'STRONG_DOWN'),
    ('PRICE_ABOVE_NORMAL_WATCH', '평년보다 조금 비쌈 (상승 주의: 부류 상위 20%)', 'DOWN'),
    ('PRICE_BELOW_NORMAL_WATCH', '평년보다 조금 쌈 (하락 주의: 부류 하위 20%)', 'UP'),
    ('PRICE_BELOW_NORMAL_ALERT', '평년보다 쌈 (하락 경계: 부류 하위 12.5%)', 'STRONG_UP'),
    ('PRICE_BELOW_NORMAL_SEVERE', '평년보다 크게 쌈 (하락 심각: 부류 하위 5% 또는 평년의 80% 이하)', 'STRONG_UP'),
    ('PRICE_RISE_ONE_WEEK_WATCH', '1주 전보다 오름 (평년 없음, 부류 상위 20%)', 'DOWN'),
    ('PRICE_RISE_ONE_WEEK_ALERT', '1주 전보다 크게 오름 (평년 없음, 부류 상위 12.5%)', 'STRONG_DOWN'),
    ('PRICE_DROP_ONE_WEEK_WATCH', '1주 전보다 내림 (평년 없음, 부류 하위 20%)', 'UP'),
    ('PRICE_DROP_ONE_WEEK_ALERT', '1주 전보다 크게 내림 (평년 없음, 부류 하위 12.5%)', 'STRONG_UP');

insert into ingredient_rule_item (rule_id, ingredient_id)
select r.id, i.id
from ingredient_rule r cross join ingredient i
where r.trigger_code like 'PRICE_%';

-- 4. 경계값. aT 가격 추이 API(B552845/priceSequel)의 대표 규격 가격으로 계산했다.
-- 표본이 50개 미만인 부류(평년 비율의 축산물 7개 · 식품 28개)는 넣지 않는다. 부류 행이 없으면 서버는 '전체' 행을 쓴다.
-- 평년 비율은 물가 상승 때문에 1보다 큰 쪽으로 치우친다(전체 중앙값 약 1.1). 수급조절매뉴얼도 가격 수준 그대로 분위수를 잡는다.
-- 1주 변동률이 ±1% 미만이면 경계값과 관계없이 안정으로 본다(Trend FLAT과 같은 기준). 식량작물 상위 20%가 0.86%라서다.
insert into ingredient_price_band (basis, category_name, sample_count, p5, p12_5, p20, p80, p87_5, p95, sample_period) values
    ('NORMAL_RATIO', '과일류', 71, 0.8623, 0.8854, 0.9845, 1.3015, 1.5409, 1.9028, '2024-01~2026-11 2주 간격 조사일, 평년 = 직전 5년 같은 날 가격 중 최고·최저를 뺀 3년 평균'),
    ('NORMAL_RATIO', '수산물', 156, 0.7877, 0.8795, 0.9587, 1.1853, 1.2204, 1.3308, '2024-01~2026-11 2주 간격 조사일, 평년 = 직전 5년 같은 날 가격 중 최고·최저를 뺀 3년 평균'),
    ('NORMAL_RATIO', '식량작물', 72, 0.8434, 0.9254, 0.9536, 1.2194, 1.2709, 1.7339, '2024-01~2026-11 2주 간격 조사일, 평년 = 직전 5년 같은 날 가격 중 최고·최저를 뺀 3년 평균'),
    ('NORMAL_RATIO', '채소류', 274, 0.8958, 0.9523, 0.9768, 1.2863, 1.4018, 1.6763, '2024-01~2026-11 2주 간격 조사일, 평년 = 직전 5년 같은 날 가격 중 최고·최저를 뺀 3년 평균'),
    ('NORMAL_RATIO', '특용작물', 84, 0.8722, 0.9765, 1.0111, 1.2084, 1.2486, 1.2739, '2024-01~2026-11 2주 간격 조사일, 평년 = 직전 5년 같은 날 가격 중 최고·최저를 뺀 3년 평균'),
    ('WEEKLY_CHANGE', '과일류', 1373, -0.0937, -0.0517, -0.0325, 0.0332, 0.0487, 0.0887, '2019-01~2026-11 2주 간격 조사일, 1주 전 평균가(ww1_bfr_avg_prc) 대비'),
    ('WEEKLY_CHANGE', '수산물', 3173, -0.058, -0.0236, -0.0103, 0.0122, 0.0256, 0.0603, '2019-01~2026-11 2주 간격 조사일, 1주 전 평균가(ww1_bfr_avg_prc) 대비'),
    ('WEEKLY_CHANGE', '식량작물', 2195, -0.0383, -0.0158, -0.0072, 0.0086, 0.0167, 0.0411, '2019-01~2026-11 2주 간격 조사일, 1주 전 평균가(ww1_bfr_avg_prc) 대비'),
    ('WEEKLY_CHANGE', '식품', 1107, -0.1273, -0.0422, -0.0198, 0.0206, 0.0446, 0.1387, '2019-01~2026-11 2주 간격 조사일, 1주 전 평균가(ww1_bfr_avg_prc) 대비'),
    ('WEEKLY_CHANGE', '채소류', 5561, -0.1213, -0.0722, -0.0461, 0.0428, 0.0728, 0.1375, '2019-01~2026-11 2주 간격 조사일, 1주 전 평균가(ww1_bfr_avg_prc) 대비'),
    ('WEEKLY_CHANGE', '축산물', 922, -0.0562, -0.0322, -0.0179, 0.0167, 0.0298, 0.0636, '2019-01~2026-11 2주 간격 조사일, 1주 전 평균가(ww1_bfr_avg_prc) 대비'),
    ('WEEKLY_CHANGE', '특용작물', 1853, -0.0509, -0.0243, -0.013, 0.0146, 0.0246, 0.0535, '2019-01~2026-11 2주 간격 조사일, 1주 전 평균가(ww1_bfr_avg_prc) 대비'),
    ('NORMAL_RATIO', '전체', 692, 0.8657, 0.9357, 0.9804, 1.2413, 1.3022, 1.5836, '2024-01~2026-11 2주 간격 조사일, 평년 = 직전 5년 같은 날 가격 중 최고·최저를 뺀 3년 평균'),
    ('WEEKLY_CHANGE', '전체', 16184, -0.0885, -0.0434, -0.023, 0.0221, 0.0429, 0.0931, '2019-01~2026-11 2주 간격 조사일, 1주 전 평균가(ww1_bfr_avg_prc) 대비');
