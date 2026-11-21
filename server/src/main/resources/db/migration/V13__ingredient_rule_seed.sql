DO $migration$
BEGIN
    INSERT INTO ingredient_rule (trigger_code, trigger_text, demand)
    SELECT r.prefix || lpad(m.month::text, 2, '0'),
           m.month::text || r.description, r.demand
    FROM generate_series(1, 12) AS m(month)
    CROSS JOIN (VALUES
        (1, 'IN_SEASON_MONTH_', '월 제철', 'UP'),
        (2, 'BEFORE_SEASON_START_MONTH_', '월 제철 진입 한 달 전', 'UP'),
        (3, 'SEASON_START_MONTH_', '월 제철 진입', 'STRONG_UP')
    ) AS r(sort_order, prefix, description, demand)
    ORDER BY m.month, r.sort_order
    ON CONFLICT (trigger_code) DO UPDATE
        SET trigger_text = EXCLUDED.trigger_text, demand = EXCLUDED.demand;

    INSERT INTO ingredient_rule (trigger_code, trigger_text, demand) VALUES
        ('PRICE_DROP_ONE_WEEK_UNDER_5_PERCENT', '1주 전 대비 0% 초과 5% 미만 가격 하락', 'UP'),
        ('PRICE_DROP_ONE_WEEK_5_TO_10_PERCENT', '1주 전 대비 5% 이상 10% 미만 가격 하락', 'UP'),
        ('PRICE_DROP_ONE_WEEK_AT_LEAST_10_PERCENT', '1주 전 대비 10% 이상 가격 하락', 'STRONG_UP'),
        ('PRICE_RISE_ONE_WEEK', '1주 전 대비 가격 상승', 'DOWN'),
        ('PRICE_BELOW_FOUR_WEEKS', '4주 전 대비 낮은 가격', 'UP')
    ON CONFLICT (trigger_code) DO UPDATE
        SET trigger_text = EXCLUDED.trigger_text, demand = EXCLUDED.demand;
END
$migration$;
