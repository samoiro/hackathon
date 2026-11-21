with previous_prices as (
    select ingredient_id, observed_date,
        lag(price) over (partition by ingredient_id order by observed_date) as price
    from ingredient_price_history
)
update ingredient_price_history history
set previous_day_price = previous_prices.price
from previous_prices
where history.ingredient_id = previous_prices.ingredient_id
    and history.observed_date = previous_prices.observed_date
    and history.previous_day_price is distinct from previous_prices.price;

comment on column ingredient_price_history.previous_day_price is '동일 원자재·규격의 직전 관측일 가격. 컬럼명은 호환성을 위해 유지. 첫 관측 또는 직전 관측 가격 미제공은 NULL';
comment on column ingredient_price_history.daily_change_rate is '직전 관측일 대비 변동률(%), (현재-직전)/직전*100, 소수 4자리. 이전 가격 미제공·0은 NULL. 컬럼명은 호환성을 위해 유지';
comment on column ingredient_price_history.price_trend is '동일 규격의 직전 관측일 대비 UP / DOWN / FLAT. 비교 불가능하면 NULL';
