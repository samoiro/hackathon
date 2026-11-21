-- 배추·무: 11월 aT 가격표에는 가을 품종(vrty_cd 03)만 있어 고랭지(02) 규격으로는 가격이 비었다.
-- shortcut: 가을 품종만 추적해 7~10월(고랭지 출하기)에는 다시 비게 된다. 연중 쓰려면 품종을 달마다 바꿔 매칭할 것.
-- 제철·김장 규칙 연결(ingredient_rule_item)은 품목 단위라 그대로 둔다.

update ingredient set variety_code = '03', variety_name = '가을' where id in (9, 18) and variety_code = '02';

-- 일배치는 마지막 조사일 다음 날부터만 받아서, 이미 받은 조사일(11-14~11-20)은 여기서 채운다.
-- 다른 원자재가 아직 받지 않은 날은 넣지 않는다 (넣으면 일배치가 그날까지 건너뛴다. 그날은 일배치가 가을 규격으로 받는다).
-- 출처: aT 가격 추이 B552845/priceSequel, 소매 가을 배추 상품 1포기 · 가을 무 중품 1개, 2026-11-21 조회
insert into ingredient_price_history (ingredient_id, observed_date, price, one_week_before_price, two_weeks_before_price,
    three_weeks_before_price, four_weeks_before_price)
select ingredient_id, observed_date::date, price::numeric, w1::numeric, w2::numeric, w3::numeric, w4::numeric from (values
    (9, '2026-11-14', 2459, 2773, 4131, null, null),
    (9, '2026-11-15', 2459, 2773, 4131, null, null),
    (9, '2026-11-16', 2466, 2749, 3705, null, null),
    (9, '2026-11-17', 2434, 2712, 3723, null, null),
    (9, '2026-11-18', 2390, 2647, 3768, 4648, null),
    (9, '2026-11-19', 2353, 2465, 2829, 4285, null),
    (9, '2026-11-20', 2300, 2469, 2823, 3775, null),
    (18, '2026-11-14', 1722, null, null, null, null),
    (18, '2026-11-15', 1722, null, null, null, null),
    (18, '2026-11-16', 1718, null, null, null, null),
    (18, '2026-11-17', 1691, null, null, null, null),
    (18, '2026-11-18', 1670, null, null, null, null),
    (18, '2026-11-19', 1677, 1760, null, null, null),
    (18, '2026-11-20', 1677, 1718, null, null, null)
) as seed(ingredient_id, observed_date, price, w1, w2, w3, w4)
where observed_date::date <= (select max(observed_date) from ingredient_price_history where ingredient_id not in (9, 18))
on conflict (ingredient_id, observed_date) do nothing;

-- 직전 관측일 가격 (IngredientPriceHistoryRepository.refreshPreviousPrices와 같은 계산)
with previous_prices as (
    select ingredient_id, observed_date, lag(price) over (partition by ingredient_id order by observed_date) as price
    from ingredient_price_history where ingredient_id in (9, 18))
update ingredient_price_history history set previous_day_price = previous_prices.price
from previous_prices
where history.ingredient_id = previous_prices.ingredient_id and history.observed_date = previous_prices.observed_date
    and history.previous_day_price is distinct from previous_prices.price;
