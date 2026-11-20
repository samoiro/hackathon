create extension if not exists postgis;

create table city_data_area (
    code text primary key,
    name text not null unique,
    category text not null,
    boundary geography(MultiPolygon, 4326) not null,
    coord jsonb generated always as (
        (ST_AsGeoJSON(boundary::geometry, 15)::jsonb) -> 'coordinates'
    ) stored,
    created_datetime timestamptz(0) not null default current_timestamp(0),
    check (not ST_IsEmpty(boundary::geometry)),
    check (ST_IsValid(boundary::geometry))
);

create index city_data_area_boundary_idx on city_data_area using gist (boundary);
comment on column city_data_area.coord is 'GeoJSON MultiPolygon 좌표 배열, [경도, 위도]. boundary에서 자동 생성';
comment on column city_data_area.boundary is 'WGS84 전체 영역. 거리 검색 및 영역 포함 판정';
