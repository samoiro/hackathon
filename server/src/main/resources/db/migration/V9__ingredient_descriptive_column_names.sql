DO $migration$
BEGIN
    ALTER TABLE ingredient RENAME COLUMN item_cd TO item_code;
    ALTER TABLE ingredient RENAME COLUMN item_nm TO item_name;
    ALTER TABLE ingredient RENAME COLUMN ctgry_cd TO category_code;
    ALTER TABLE ingredient RENAME COLUMN ctgry_nm TO category_name;
    ALTER TABLE ingredient RENAME COLUMN se_cd TO trade_type_code;
    ALTER TABLE ingredient RENAME COLUMN se_nm TO trade_type_name;
    ALTER TABLE ingredient RENAME COLUMN vrty_cd TO variety_code;
    ALTER TABLE ingredient RENAME COLUMN vrty_nm TO variety_name;
    ALTER TABLE ingredient RENAME COLUMN grd_cd TO grade_code;
    ALTER TABLE ingredient RENAME COLUMN grd_nm TO grade_name;
    ALTER TABLE ingredient RENAME COLUMN unit_sz TO unit_size;
    COMMENT ON COLUMN ingredient.unit_size IS '판매 단위 크기; price는 unit_size × unit 기준';
END
$migration$;
