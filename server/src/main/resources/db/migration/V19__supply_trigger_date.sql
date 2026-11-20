-- 날짜로 정해지는 트리거(기념일·학사·시즌)가 실제로 언제인지. 날씨·행사처럼 그때그때 판정하는 트리거는 넣지 않는다.
-- 학교마다·해마다 다른 날은 대부분이 몰리는 기간을 범위로 넣었다. 근거는 줄마다 주석으로 남긴다.
-- shortcut: 2026~2027년만 넣었다, 다음 해 월력요항·학사일정·수능일이 나오면 다음 파일에 추가한다.

create table supply_trigger_date (
    trigger_code text not null references supply_trigger (code),
    start_date   date not null,
    end_date     date not null, -- 하루짜리면 start_date와 같다
    primary key (trigger_code, start_date),
    check (end_date >= start_date)
);

create index supply_trigger_date_range_idx on supply_trigger_date (start_date, end_date);

comment on table supply_trigger_date is '기념일·학사·시즌 트리거의 연도별 기간. supply_rule.lead_days_* 만큼 앞당겨 준비 시점을 계산한다';
comment on column supply_trigger_date.end_date is '연휴는 마지막 날(대체공휴일 포함). 학교마다 다른 날은 대부분이 몰리는 기간의 끝';

-- 해마다 달라지는 날
insert into supply_trigger_date (trigger_code, start_date, end_date) values
    -- 설·추석: https://github.com/hyunbinseo/holidays-kr (우주항공청 월력요항) 2026.json, 2027.json
    ('SEOLLAL', '2026-02-16', '2026-02-18'),
    ('SEOLLAL', '2027-02-06', '2027-02-09'), -- 2027-02-09 대체공휴일(설날)
    ('CHUSEOK', '2026-09-24', '2026-09-26'),
    ('CHUSEOK', '2027-09-14', '2027-09-16'),
    -- 수능: 2027학년도 평가원 시행세부계획, 2028학년도 교육부 발표(2025-06-02)
    ('CSAT', '2026-11-19', '2026-11-19'),
    ('CSAT', '2027-11-18', '2027-11-18'),
    -- 입학·1학기 개학: 학년도는 3월 1일 시작, 3·1절 다음 첫 평일. 2026-03-02는 대체공휴일
    ('SCHOOL_ENTRANCE', '2026-03-03', '2026-03-03'),
    ('SCHOOL_ENTRANCE', '2027-03-02', '2027-03-02'),
    ('SCHOOL_START', '2026-03-03', '2026-03-03'),
    ('SCHOOL_START', '2027-03-02', '2027-03-02'),
    -- 2학기 개학: 학교마다 다름. 여름방학이 7월 셋째·넷째 주에 시작해 약 4주 뒤인 8월 셋째 주로 추정
    ('SCHOOL_START', '2026-08-17', '2026-08-21'),
    ('SCHOOL_START', '2027-08-16', '2027-08-20'),
    -- 방학: 여름은 7월 넷째 주 방학식(2026-07-22 서울 초교 사례)부터 2학기 개학 전까지,
    -- 겨울은 12월 말~1월 초 방학식(졸업·종업식 겸)부터 3월 개학 전까지
    ('SCHOOL_VACATION', '2025-12-31', '2026-03-02'),
    ('SCHOOL_VACATION', '2026-07-22', '2026-08-16'),
    ('SCHOOL_VACATION', '2026-12-31', '2027-03-01'),
    ('SCHOOL_VACATION', '2027-07-21', '2027-08-15'),
    ('SCHOOL_VACATION', '2027-12-31', '2028-03-01'),
    -- 졸업: 나이스 학사일정 전국 초교 표본에서 12/31, 1/6, 1/7이 가장 많고 1월이 절반 이상. 그 기간만 넣었다
    ('GRADUATION', '2025-12-31', '2026-01-09'),
    ('GRADUATION', '2026-12-31', '2027-01-08'),
    ('GRADUATION', '2027-12-30', '2028-01-07'),
    -- 휴가철: 정부 하계 휴가철 특별교통대책기간(2026-07-25~08-10). 2027년은 같은 기간으로 추정
    ('SUMMER_VACATION', '2026-07-25', '2026-08-10'),
    ('SUMMER_VACATION', '2027-07-24', '2027-08-09'),
    -- 장마(중부): 2026년은 기상청 실적(7/1 시작, 7/26 잠정 종료), 2027년은 평년(1991~2020) 6/25~7/26
    ('RAINY_SEASON', '2026-07-01', '2026-07-26'),
    ('RAINY_SEASON', '2027-06-25', '2027-07-26'),
    -- 독감: 질병청 인플루엔자 유행주의보 발령~해제. 2026-2027절기는 9/11 발령, 해제일은 지난 절기(5/15)로 추정
    ('FLU_SEASON', '2025-10-17', '2026-05-15'),
    ('FLU_SEASON', '2026-09-11', '2027-05-14'),
    -- 겨울: 기상청 계절 구분 12~2월
    ('WINTER', '2025-12-01', '2026-02-28'),
    ('WINTER', '2026-12-01', '2027-02-28'),
    ('WINTER', '2027-12-01', '2028-02-29');

-- 해마다 같은 날·기간
insert into supply_trigger_date (trigger_code, start_date, end_date)
select t.code, make_date(y, t.m, t.d), make_date(y, t.end_m, t.end_d)
from generate_series(2026, 2027) as y
cross join (values
    -- 양력 기념일
    ('VALENTINES_DAY', 2, 14, 2, 14),
    ('WHITE_DAY', 3, 14, 3, 14),
    ('CHILDRENS_DAY', 5, 5, 5, 5),
    ('PARENTS_DAY', 5, 8, 5, 8),
    ('TEACHERS_DAY', 5, 15, 5, 15),
    ('PEPERO_DAY', 11, 11, 11, 11),
    ('CHRISTMAS', 12, 25, 12, 25),
    ('YEAR_END', 12, 1, 12, 31),          -- 송년회 시즌을 12월 한 달로 본다
    -- 시즌
    ('SUMMER', 6, 1, 8, 31),              -- 기상청 계절 구분 6~8월
    ('KIMJANG', 11, 21, 12, 10),          -- 서울·중부 김장 적기 11월 하순~12월 초 (웨더아이 전망 등 보도)
    -- 아래는 통계 근거가 약한 관행상 기간
    ('PICNIC_SEASON', 4, 1, 5, 31),       -- 봄 나들이
    ('PICNIC_SEASON', 9, 15, 11, 15),     -- 가을 나들이·단풍
    ('WEDDING_SEASON', 4, 1, 5, 31),      -- 봄 예식. 혼인신고는 5월이 상위권(통계청 혼인·이혼 통계)
    ('WEDDING_SEASON', 10, 1, 11, 30),    -- 가을 예식
    ('SEASON_CHANGE', 3, 1, 4, 30),       -- 겨울→봄
    ('SEASON_CHANGE', 9, 1, 10, 31),      -- 여름→가을
    ('MOVING_SEASON', 2, 1, 3, 31),       -- 새 학기 전 봄 이사철
    ('MOVING_SEASON', 9, 1, 10, 31)       -- 가을 이사철
) as t(code, m, d, end_m, end_d);

-- 날짜로 정해지는 트리거는 2026·2027년마다 그해에 걸친 기간이 하나 이상 있어야 한다
do $check$
begin
    if exists (
        select 1
        from supply_trigger st
        cross join generate_series(2026, 2027) as y
        where st.type in ('HOLIDAY', 'SCHOOL', 'SEASON')
          and not exists (
              select 1 from supply_trigger_date d
              where d.trigger_code = st.code
                and daterange(d.start_date, d.end_date, '[]') && daterange(make_date(y, 1, 1), make_date(y, 12, 31), '[]')
          )
    ) then
        raise exception '날짜가 빠진 연도가 있는 HOLIDAY·SCHOOL·SEASON 트리거가 있다';
    end if;
end
$check$;
