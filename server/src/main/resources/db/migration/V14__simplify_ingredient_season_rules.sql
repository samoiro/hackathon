DO $migration$
BEGIN
    UPDATE ingredient_rule
    SET demand = 'STRONG_UP'
    WHERE trigger_code ~ '^IN_SEASON_MONTH_(0[1-9]|1[0-2])$';

    UPDATE ingredient_rule
    SET trigger_text = substring(trigger_code from '(0[1-9]|1[0-2])$')::integer::text
        || '월 제철 시작 한 달 전'
    WHERE trigger_code ~ '^BEFORE_SEASON_START_MONTH_(0[1-9]|1[0-2])$';

    DELETE FROM ingredient_rule
    WHERE trigger_code ~ '^SEASON_START_MONTH_(0[1-9]|1[0-2])$';
END
$migration$;
