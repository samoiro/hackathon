-- 기업마당 지원사업 공고(공공데이터포털 15157820). 대시보드 "마감 임박 지원사업" 카드가 읽는다.
-- API는 지금 접수 중인 공고만 준다(2026-11-21 1,303건, 마감 지난 공고 0건). 일배치가 전부 받아 한 트랜잭션에서 통째로 바꾼다.
-- 서울·소상공인·7일 안 마감 조건은 조회할 때 건다. 조건을 바꿔도 다시 받을 필요가 없다.

create table support_program (
    id                  text primary key check (id ~ '^PBLN_[0-9]+$'),
    title               text not null check (btrim(title) <> ''),
    url                 text not null check (url like 'https://www.bizinfo.go.kr/%'),
    agency_name         text not null check (btrim(agency_name) <> ''),
    field_name          text,
    target_name         text not null check (btrim(target_name) <> ''),
    hashtags            text[] not null,
    application_period  text not null check (btrim(application_period) <> ''),
    deadline            date,
    registered_datetime timestamptz(0) not null,
    created_datetime    timestamptz(0) not null default current_timestamp(0)
);

comment on table support_program is '지원사업 공고';
comment on column support_program.id is '공고 ID';
comment on column support_program.title is '공고명';
comment on column support_program.url is '공고 URL';
comment on column support_program.agency_name is '소관기관명';
comment on column support_program.field_name is '지원 분야';
comment on column support_program.target_name is '지원 대상';
comment on column support_program.hashtags is '해시태그';
comment on column support_program.application_period is '신청 기간';
comment on column support_program.deadline is '마감일';
comment on column support_program.registered_datetime is '등록 일시';
comment on column support_program.created_datetime is '수집 일시';
