-- 시연용 임의 행사: 성수역(37.5446, 127.0559) 1.5km 안에 서울시 원본 행사가 없어 분석 화면이 비었다.
-- 제목 끝에 "(임의 데이터)"를 붙여 실제 행사와 구분한다. 분석이 보는 축제·공연 분류만 쓰고 7일 창에 하나씩은 걸리게 날짜를 나눴다.
-- shortcut: source_key는 숫자만 허용(V15)이라 실제 cultcode(13만~16만대)와 먼 99만대를 쓴다.
-- 행사 일배치가 돌면 원본에 없는 행으로 보고 is_active = false로 바꾼다. 계속 보여야 하면 배치에서 이 키를 빼거나 다시 활성화한다.
insert into cultural_event (source_key, title, category, district_name, place, start_date, end_date, time_text,
    coord, is_free, source_url, raw_payload)
select source_key, title || ' (임의 데이터)', category, '성동구', place, start_date::date, end_date::date, time_text,
    ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography, is_free, 'https://example.com/demo-event/' || source_key,
    jsonb_build_object('demo', true)
from (values
    ('990001', '성수 골목 문화축제', '축제-문화/예술', '성수동 카페거리', '2026-11-22', '2026-11-23', '12:00~20:00', 127.0565, 37.5440, true),
    ('990002', '서울숲 가을밤 야외 콘서트', '콘서트', '서울숲 야외무대', '2026-11-25', '2026-11-25', '19:00~21:00', 127.0418, 37.5444, true),
    ('990003', '성수 소극장 창작연극 [공장의 밤]', '연극', '성수 소극장', '2026-11-28', '2026-11-30', '19:30', 127.0590, 37.5470, false),
    ('990004', '성수 연말 플리마켓 축제', '축제-기타', '커먼그라운드 야외광장', '2026-12-05', '2026-12-07', '11:00~19:00', 127.0654, 37.5413, true),
    ('990005', '뚝섬 클래식 살롱 콘서트', '클래식', '뚝섬역 문화공간', '2026-12-12', '2026-12-12', '18:00', 127.0470, 37.5472, false),
    ('990006', '성수 크리스마스 뮤지컬 [선물]', '뮤지컬/오페라', '성수아트홀', '2026-12-19', '2026-12-21', '15:00, 19:00', 127.0560, 37.5375, false)
) as demo(source_key, title, category, place, start_date, end_date, time_text, lon, lat, is_free)
on conflict (source_key) do nothing;
