-- 분석 API: 서울 밖 주소 오류 코드와, 2단계(주변 행사 조회)가 끝나면 보여 줄 조회 조건.
-- 기준: ai-server/docs/API_CONTRACT_DRAFT.md, server/docs/exception-guide.md

insert into error_code (code, http_status, message) values
    ('ANALYSIS_OUTSIDE_SEOUL', 422, '서울 밖 주소는 아직 지원하지 않아요.');

alter table briefing_analysis_job
    add column event_search jsonb check (jsonb_typeof(event_search) = 'object');

comment on column briefing_analysis_job.event_search is '행사 조회 조건과 건수 (EventSearch). EVENT_SEARCH 단계가 끝나면 채운다';
