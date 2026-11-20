alter table ingredient_price_history rename column collected_at to created_datetime;
alter table ingredient_price_history
    alter column created_datetime type timestamptz(0)
        using date_trunc('second', created_datetime),
    alter column created_datetime set default current_timestamp(0);
comment on column ingredient_price_history.created_datetime is '가격 이력 최초 생성 시각 (초 단위)';
