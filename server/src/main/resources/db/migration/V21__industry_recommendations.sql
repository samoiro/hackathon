create table industry_ingredient (
    industry_id bigint not null references industry(id) on delete cascade on update restrict,
    ingredient_id bigint not null references ingredient(id) on delete restrict on update restrict,
    sort_order smallint not null check (sort_order between 1 and 3),
    primary key (industry_id, ingredient_id),
    unique (industry_id, sort_order)
);

create index industry_ingredient_ingredient_id_index on industry_ingredient(ingredient_id);

create table industry_supply (
    industry_id bigint not null references industry(id) on delete cascade on update restrict,
    supply_id bigint not null references supply(id) on delete restrict on update restrict,
    sort_order smallint not null check (sort_order between 1 and 2),
    primary key (industry_id, supply_id),
    unique (industry_id, sort_order)
);

create index industry_supply_supply_id_index on industry_supply(supply_id);

comment on table industry_ingredient is '업종별 추천 원자재. 업종당 최대 3개';
comment on column industry_ingredient.industry_id is '추천 대상 업종 ID';
comment on column industry_ingredient.ingredient_id is '추천 원자재 ID';
comment on column industry_ingredient.sort_order is '업종 내 고정 추천 순서 1~3';
comment on table industry_supply is '업종별 추천 부자재. 업종당 최대 2개';
comment on column industry_supply.industry_id is '추천 대상 업종 ID';
comment on column industry_supply.supply_id is '추천 부자재 ID';
comment on column industry_supply.sort_order is '업종 내 고정 추천 순서 1~2';

-- 환경마다 ID가 달라도 컨펌한 동일 품목을 연결한다.
create temporary table industry_recommendation_seed (
    industry_name text primary key,
    ingredient_names text[] not null,
    supply_names text[] not null
) on commit drop;

insert into industry_recommendation_seed values
    ('카페', array['우유', '딸기', '바나나'], array['테이크아웃 컵', '빨대']),
    ('베이커리', array['계란', '우유', '아몬드'], array['OPP 봉투', '케이크 상자']),
    ('한식·국밥', array['쌀', '돼지', '파'], array['국물 포장 용기', '배달 봉투']),
    ('고깃집', array['수입 돼지고기', '수입 소고기', '상추'], array['물티슈', '손님용 앞치마']),
    ('치킨·튀김', array['닭고기', '감자', '양배추'], array['튀김유', '치킨 박스']),
    ('분식·김밥', array['쌀', '김', '계란'], array['김밥 호일', '포장 용기']),
    ('중식', array['돼지', '양파', '양배추'], array['배달 용기', '나무젓가락']),
    ('주점·포차', array['물오징어', '마른오징어', '두부'], array['물티슈', '냅킨']),
    ('해물·조개구이', array['가리비', '바지락', '새우'], array['손님용 비닐장갑', '껍데기통']),
    ('배달 전문', array[]::text[], array['배달 용기', '배달 봉투']),
    ('편의점·슈퍼', array['우유', '계란', '즉석밥'], array['비닐봉투', '영수증 롤지']),
    ('정육점', array['돼지', '수입 돼지고기', '소'], array['고기 포장 트레이', '랩']),
    ('수산물', array['고등어', '갈치', '물오징어'], array['스티로폼 박스', '아이스팩']),
    ('청과', array['사과', '배', '바나나'], array['과일 완충 캡', '손잡이 봉투']),
    ('반찬가게', array['배추', '무', '시금치'], array['포장 용기', '실링 필름']),
    ('꽃집', array[]::text[], array['꽃 포장지', '리본']),
    ('약국', array[]::text[], array['약봉투', '약포지']),
    ('문구·완구', array[]::text[], array['선물 포장지', '쇼핑백']),
    ('의류·잡화', array[]::text[], array['쇼핑백', '택']),
    ('철물·생활용품', array[]::text[], array['테이프', '쇼핑백']),
    ('미용실', array[]::text[], array['넥지', '염모제']),
    ('네일숍', array[]::text[], array['젤', '네일 파일']),
    ('피부관리·왁싱', array[]::text[], array['팩', '왁스']),
    ('세탁소', array[]::text[], array['옷걸이', '의류 비닐 커버']),
    ('수선집', array[]::text[], array['실', '지퍼']),
    ('반려동물 미용', array[]::text[], array['반려동물 샴푸', '클리퍼 날']),
    ('목욕탕·사우나', array[]::text[], array['수건', '목욕 일회용품']),
    ('자동차 정비', array[]::text[], array['엔진오일', '필터']);

do $$ begin
    if exists (
        select 1 from industry_recommendation_seed seed
        left join industry on industry.name = seed.industry_name
        where industry.id is null
    ) or exists (
        select 1 from industry_recommendation_seed seed
        cross join lateral unnest(seed.ingredient_names) item(name)
        left join ingredient on ingredient.item_name = item.name
        where ingredient.id is null
    ) or exists (
        select 1 from industry_recommendation_seed seed
        cross join lateral unnest(seed.supply_names) item(name)
        left join supply on supply.name = item.name
        where supply.id is null
    ) then
        raise exception '컨펌한 업종 또는 추천 품목이 없습니다. 기존 기준 데이터를 확인하세요';
    end if;
end $$;

insert into industry_ingredient(industry_id, ingredient_id, sort_order)
select industry.id, ingredient.id, item.position::smallint
from industry_recommendation_seed seed
join industry on industry.name = seed.industry_name
cross join lateral unnest(seed.ingredient_names) with ordinality item(name, position)
join ingredient on ingredient.item_name = item.name;

insert into industry_supply(industry_id, supply_id, sort_order)
select industry.id, supply.id, item.position::smallint
from industry_recommendation_seed seed
join industry on industry.name = seed.industry_name
cross join lateral unnest(seed.supply_names) with ordinality item(name, position)
join supply on supply.name = item.name;
