-- 복제한 이름이 ID와 일치하도록 묶고 원본 이름 변경은 연결 테이블에 전파한다.
alter table industry add constraint industry_id_name_key unique (id, name);
alter table ingredient add constraint ingredient_id_item_name_key unique (id, item_name);
alter table supply add constraint supply_id_name_key unique (id, name);

alter table industry_ingredient
    add column industry_name text,
    add column ingredient_name text;

update industry_ingredient item
set industry_name = industry.name, ingredient_name = ingredient.item_name
from industry, ingredient
where industry.id = item.industry_id and ingredient.id = item.ingredient_id;

alter table industry_ingredient
    alter column industry_name set not null,
    alter column ingredient_name set not null,
    add constraint industry_ingredient_industry_name_foreign_key
        foreign key (industry_id, industry_name) references industry(id, name)
        on delete cascade on update cascade,
    add constraint industry_ingredient_ingredient_name_foreign_key
        foreign key (ingredient_id, ingredient_name) references ingredient(id, item_name)
        on delete restrict on update cascade;

alter table industry_supply
    add column industry_name text,
    add column supply_name text;

update industry_supply item
set industry_name = industry.name, supply_name = supply.name
from industry, supply
where industry.id = item.industry_id and supply.id = item.supply_id;

alter table industry_supply
    alter column industry_name set not null,
    alter column supply_name set not null,
    add constraint industry_supply_industry_name_foreign_key
        foreign key (industry_id, industry_name) references industry(id, name)
        on delete cascade on update cascade,
    add constraint industry_supply_supply_name_foreign_key
        foreign key (supply_id, supply_name) references supply(id, name)
        on delete restrict on update cascade;

comment on column industry_ingredient.industry_name is '업종명. industry_id에 해당하는 원본 이름과 동기화';
comment on column industry_ingredient.ingredient_name is '원자재명. ingredient_id에 해당하는 원본 이름과 동기화';
comment on column industry_supply.industry_name is '업종명. industry_id에 해당하는 원본 이름과 동기화';
comment on column industry_supply.supply_name is '부자재명. supply_id에 해당하는 원본 이름과 동기화';
