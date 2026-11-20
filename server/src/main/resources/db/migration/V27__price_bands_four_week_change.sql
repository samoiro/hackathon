-- 원자재 가격 단계 기준을 평년 비율·1주 변동률(V26)에서 4주 변동률 하나로 바꾼다.
-- V26은 이미 main에 올라가 배포 DB에 적용됐을 수 있어 고치지 않고, 바뀐 내용을 이 파일로 더한다.
-- 이유: 평년(5년)은 물가 상승이 섞이고(평년 비율 중앙값 1.09) 원자재 118개 중 67개만 5년 가격이 이어진다.
--       4주 전 평균가는 모든 품목에 있다. 1주 변동은 화면의 가격 흐름(trend)으로만 쓰고 같은 변화를 두 번 세지 않는다.
-- 기준 설명: docs/database-schema.md ingredient_price_band

-- 1. 경계 표는 기준 데이터뿐이라 다시 만든다. 평년·1주 두 기준(basis)을 없애고 부류당 한 행.
drop table ingredient_price_band;

create table ingredient_price_band (
    category_name  text    primary key check (btrim(category_name) <> ''),
    sample_count   integer not null check (sample_count > 0),
    p5             numeric(8, 4) not null,
    p12_5          numeric(8, 4) not null,
    p20            numeric(8, 4) not null,
    p80            numeric(8, 4) not null,
    p87_5          numeric(8, 4) not null,
    p95            numeric(8, 4) not null,
    sample_period  text    not null,
    check (p5 <= p12_5 and p12_5 <= p20 and p20 <= p80 and p80 <= p87_5 and p87_5 <= p95)
);

comment on table ingredient_price_band is '원자재 부류별 4주 변동률 단계 경계(분위수). 값 = 조사일 가격 ÷ 4주 전 평균 가격 − 1';
comment on column ingredient_price_band.category_name is 'ingredient.category_name (식량작물·채소류·과일류·특용작물·축산물·수산물·식품)';
comment on column ingredient_price_band.p5 is '하위 5%: 하락 심각 경계';
comment on column ingredient_price_band.p12_5 is '하위 12.5%: 하락 경계';
comment on column ingredient_price_band.p20 is '하위 20%: 하락 주의';
comment on column ingredient_price_band.p80 is '상위 20%: 상승 주의';
comment on column ingredient_price_band.p87_5 is '상위 12.5%: 상승 경계';
comment on column ingredient_price_band.p95 is '상위 5%: 상승 심각';
comment on column ingredient_price_band.sample_period is '분위수를 계산한 자료 기간과 표본 방식';

-- aT 가격 추이 API(B552845/priceSequel) 대표 규격 가격, 2019-01~2026-11 조사일 2주 간격으로 계산했다.
insert into ingredient_price_band (category_name, sample_count, p5, p12_5, p20, p80, p87_5, p95, sample_period)
select v.*, '2019-01~2026-11 2주 간격 조사일, 4주 전 평균가(ww4_bfr_avg_prc) 대비'
from (values
    ('채소류', 5423, -0.2676, -0.1735, -0.1181, 0.1308, 0.2119, 0.3842),
    ('수산물', 3120, -0.0855, -0.0408, -0.0212, 0.0263, 0.0469, 0.0907),
    ('식량작물', 2130, -0.0849, -0.0439, -0.0257, 0.0291, 0.0494, 0.0896),
    ('특용작물', 1829, -0.0904, -0.0465, -0.0283, 0.0343, 0.0539, 0.1124),
    ('과일류', 1305, -0.1834, -0.1029, -0.0639, 0.0725, 0.1085, 0.1814),
    ('식품', 1099, -0.1407, -0.0699, -0.0376, 0.0434, 0.0841, 0.1872),
    ('축산물', 892, -0.0820, -0.0488, -0.0274, 0.0343, 0.0541, 0.0865)
) as v(category_name, sample_count, p5, p12_5, p20, p80, p87_5, p95);

-- 2. V26의 평년·1주 단계 규칙 10개를 지운다. 연결(ingredient_rule_item)은 ON DELETE CASCADE로 함께 지워진다.
delete from ingredient_rule where trigger_code in (
    'PRICE_ABOVE_NORMAL_SEVERE', 'PRICE_ABOVE_NORMAL_ALERT', 'PRICE_ABOVE_NORMAL_WATCH',
    'PRICE_BELOW_NORMAL_WATCH', 'PRICE_BELOW_NORMAL_ALERT', 'PRICE_BELOW_NORMAL_SEVERE',
    'PRICE_RISE_ONE_WEEK_WATCH', 'PRICE_RISE_ONE_WEEK_ALERT',
    'PRICE_DROP_ONE_WEEK_WATCH', 'PRICE_DROP_ONE_WEEK_ALERT');

-- 3. 4주 단계 규칙. 오르면 원가 부담이라 구매 미권장, 내리면 구매 권장(기존 demand 뜻과 같음). 심각·경계는 같은 강도로 본다.
insert into ingredient_rule (trigger_code, trigger_text, demand) values
    ('PRICE_RISE_FOUR_WEEKS_SEVERE', '4주 전보다 크게 오름 (상승 심각: 부류 상위 5%)', 'STRONG_DOWN'),
    ('PRICE_RISE_FOUR_WEEKS_ALERT', '4주 전보다 많이 오름 (상승 경계: 부류 상위 12.5%)', 'STRONG_DOWN'),
    ('PRICE_RISE_FOUR_WEEKS_WATCH', '4주 전보다 오름 (상승 주의: 부류 상위 20%)', 'DOWN'),
    ('PRICE_DROP_FOUR_WEEKS_WATCH', '4주 전보다 내림 (하락 주의: 부류 하위 20%)', 'UP'),
    ('PRICE_DROP_FOUR_WEEKS_ALERT', '4주 전보다 많이 내림 (하락 경계: 부류 하위 12.5%)', 'STRONG_UP'),
    ('PRICE_DROP_FOUR_WEEKS_SEVERE', '4주 전보다 크게 내림 (하락 심각: 부류 하위 5%)', 'STRONG_UP');

-- 가격 규칙은 다른 가격 규칙처럼 전체 원자재에 연결한다.
insert into ingredient_rule_item (rule_id, ingredient_id)
select r.id, i.id
from ingredient_rule r cross join ingredient i
where r.trigger_code like 'PRICE_%FOUR_WEEKS_%';
