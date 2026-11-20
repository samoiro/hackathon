DO $migration$
BEGIN
    CREATE TABLE IF NOT EXISTS ingredient_rule_item (
        rule_id bigint NOT NULL REFERENCES ingredient_rule(id) ON DELETE CASCADE,
        ingredient_id bigint NOT NULL REFERENCES ingredient(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
        sort_order smallint NOT NULL CHECK (sort_order > 0),
        PRIMARY KEY (rule_id, ingredient_id)
    );
    CREATE INDEX IF NOT EXISTS ingredient_rule_item_ingredient_id_index ON ingredient_rule_item(ingredient_id);
    COMMENT ON TABLE ingredient_rule_item IS '원자재 규칙의 고정 적용 대상. 실제 조건 충족 여부는 서버에서 별도 판정';
    COMMENT ON COLUMN ingredient_rule_item.rule_id IS '원자재 추천 규칙 ID';
    COMMENT ON COLUMN ingredient_rule_item.ingredient_id IS '규칙 적용 대상 원자재 ID';
    COMMENT ON COLUMN ingredient_rule_item.sort_order IS '규칙 내 고정 우선순위. 작은 값부터 우선하며 가격 배치로 변경하지 않음';
END
$migration$;
