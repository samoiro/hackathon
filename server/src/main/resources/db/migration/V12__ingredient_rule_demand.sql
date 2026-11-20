DO $migration$
BEGIN
    ALTER TABLE ingredient_rule
        ADD COLUMN demand text not null
            CHECK (demand IN ('UP', 'STRONG_UP', 'DOWN'));
    COMMENT ON COLUMN ingredient_rule.demand IS '구매 권장 방향: UP 권장, STRONG_UP 강력 권장, DOWN 미권장. 판매 수요 예측이 아님';
END
$migration$;
