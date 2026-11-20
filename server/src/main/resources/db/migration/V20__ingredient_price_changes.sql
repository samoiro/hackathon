alter table ingredient_price_history
    add column previous_day_price numeric(18,4),
    add column one_week_before_price numeric(18,4),
    add column two_weeks_before_price numeric(18,4),
    add column three_weeks_before_price numeric(18,4),
    add column four_weeks_before_price numeric(18,4),
    add column daily_change_rate numeric generated always as (
        case when price is not null and previous_day_price > 0
            then round((price - previous_day_price) / previous_day_price * 100, 4) end
    ) stored,
    add column price_trend text generated always as (
        case when price is null or previous_day_price is null or previous_day_price <= 0 then null
            when price > previous_day_price then 'UP'
            when price < previous_day_price then 'DOWN'
            else 'FLAT' end
    ) stored,
    add constraint ingredient_price_history_finite_prices check (
        (price is null or price between 0 and 99999999999999.9999)
        and (previous_day_price is null or previous_day_price between 0 and 99999999999999.9999)
        and (one_week_before_price is null or one_week_before_price between 0 and 99999999999999.9999)
        and (two_weeks_before_price is null or two_weeks_before_price between 0 and 99999999999999.9999)
        and (three_weeks_before_price is null or three_weeks_before_price between 0 and 99999999999999.9999)
        and (four_weeks_before_price is null or four_weeks_before_price between 0 and 99999999999999.9999)
    );

update ingredient_price_history current_day
set previous_day_price = previous_day.price
from ingredient_price_history previous_day
where previous_day.ingredient_id = current_day.ingredient_id
    and previous_day.observed_date = current_day.observed_date - 1;

comment on column ingredient_price_history.previous_day_price is '동일 대표 규격의 달력상 전날 조사 가격. 전날 미조사·누락은 NULL';
comment on column ingredient_price_history.daily_change_rate is '전일 대비 변동률(%), (당일-전일)/전일*100, 소수 4자리. 전일 미제공·0은 NULL';
comment on column ingredient_price_history.price_trend is '동일 규격의 전일 대비 UP / DOWN / FLAT. 비교 불가능하면 NULL';
comment on column ingredient_price_history.one_week_before_price is '해당 조사일 API ww1_bfr_avg_prc. kg 환산값이 아닌 대표 판매 규격 가격';
comment on column ingredient_price_history.two_weeks_before_price is '해당 조사일 API ww2_bfr_avg_prc';
comment on column ingredient_price_history.three_weeks_before_price is '해당 조사일 API ww3_bfr_avg_prc';
comment on column ingredient_price_history.four_weeks_before_price is '해당 조사일 API ww4_bfr_avg_prc';
