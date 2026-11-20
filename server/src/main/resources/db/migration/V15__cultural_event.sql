begin;

create extension if not exists postgis;

create table cultural_event (
    id bigint generated always as identity primary key,
    source_key text not null unique check (source_key ~ '^[0-9]+$'),
    title text not null check (btrim(title) <> ''),
    category text,
    district_name text,
    place text not null check (btrim(place) <> ''),
    start_date date not null,
    end_date date not null check (end_date >= start_date),
    time_text text,
    coord geography(Point,4326) not null,
    target_audience text,
    program text,
    notes text,
    fee_text text,
    is_free boolean,
    source_url text not null,
    image_url text,
    is_active boolean not null default true,
    raw_payload jsonb not null check (jsonb_typeof(raw_payload) = 'object'),
    created_datetime timestamptz(0) not null default current_timestamp(0),
    updated_datetime timestamptz(0) not null default current_timestamp(0),
    check (not ST_IsEmpty(coord::geometry)),
    check (ST_X(coord::geometry) between -180 and 180
        and ST_Y(coord::geometry) between -90 and 90),
    check (ST_X(coord::geometry) <> 0 and ST_Y(coord::geometry) <> 0)
);

create index cultural_event_coord_idx on cultural_event using gist (coord) where is_active;
create index cultural_event_dates_idx on cultural_event (end_date, start_date) where is_active;

create function cultural_event_set_timestamps() returns trigger language plpgsql as $$
begin
    new.created_datetime := old.created_datetime;
    new.updated_datetime := old.updated_datetime;
    if new is distinct from old then
        new.updated_datetime := clock_timestamp()::timestamptz(0);
    end if;
    return new;
end;
$$;

create trigger cultural_event_timestamps before update on cultural_event
for each row execute function cultural_event_set_timestamps();

comment on table cultural_event is '서울시 aiViewCulture의 좌표·일정이 유효한 오프라인 행사. 전체 재수집 후 제외·누락 행사는 비활성화';
comment on column cultural_event.source_key is '서울문화포털 HMPG_ADDR의 cultcode. 일정·제목 변경에도 동일 행사로 갱신';
comment on column cultural_event.coord is 'WGS84 Point(경도 LOT, 위도 LAT). ST_Distance/ST_DWithin의 거리 단위는 미터';
comment on column cultural_event.created_datetime is 'DB 최초 적재 시각. API RGSTDATE(원본 등록일)와 구분';
comment on column cultural_event.updated_datetime is '저장 내용·활성 상태가 마지막으로 변경된 시각. 동일 응답 재수집 시 유지';

commit;
