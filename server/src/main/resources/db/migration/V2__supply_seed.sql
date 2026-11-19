-- 노션 "업종별 행사·날씨·시즌 부자재 준비표" 데이터. 표가 바뀌면 이 파일은 그대로 두고 V3 이후 파일로 추가·수정한다.

insert into supply_trigger (code, label, type) values
    ('HEATWAVE', '폭염', 'WEATHER'),
    ('COLD_WAVE', '한파', 'WEATHER'),
    ('RAIN', '비', 'WEATHER'),
    ('FINE_DUST', '미세먼지·황사', 'WEATHER'),
    ('HEAVY_SNOW', '폭설', 'WEATHER'),
    ('FESTIVAL', '축제', 'EVENT'),
    ('PERFORMANCE', '공연', 'EVENT'),
    ('SPORTS', '스포츠 중계', 'EVENT'),
    ('CROWD_SURGE', '혼잡도 급증', 'EVENT'),
    ('TRAFFIC_CONTROL', '교통통제·마라톤', 'EVENT'),
    ('SEOLLAL', '설', 'HOLIDAY'),
    ('CHUSEOK', '추석', 'HOLIDAY'),
    ('VALENTINES_DAY', '발렌타인데이', 'HOLIDAY'),
    ('WHITE_DAY', '화이트데이', 'HOLIDAY'),
    ('PEPERO_DAY', '빼빼로데이', 'HOLIDAY'),
    ('CHRISTMAS', '크리스마스', 'HOLIDAY'),
    ('PARENTS_DAY', '어버이날', 'HOLIDAY'),
    ('TEACHERS_DAY', '스승의날', 'HOLIDAY'),
    ('CHILDRENS_DAY', '어린이날', 'HOLIDAY'),
    ('YEAR_END', '연말·송년회', 'HOLIDAY'),
    ('CSAT', '수능', 'SCHOOL'),
    ('GRADUATION', '졸업', 'SCHOOL'),
    ('SCHOOL_ENTRANCE', '입학', 'SCHOOL'),
    ('SCHOOL_START', '개학', 'SCHOOL'),
    ('SCHOOL_VACATION', '방학', 'SCHOOL'),
    ('SUMMER_VACATION', '휴가철', 'SEASON'),
    ('KIMJANG', '김장철', 'SEASON'),
    ('PICNIC_SEASON', '봄·가을 나들이', 'SEASON'),
    ('WEDDING_SEASON', '웨딩 시즌', 'SEASON'),
    ('SEASON_CHANGE', '환절기', 'SEASON'),
    ('MOVING_SEASON', '이사철', 'SEASON'),
    ('FLU_SEASON', '독감 시즌', 'SEASON'),
    ('RAINY_SEASON', '장마', 'SEASON'),
    ('SUMMER', '여름', 'SEASON'),
    ('WINTER', '겨울', 'SEASON');

-- 노션 표 한 줄 = 아래 한 줄. 트리거가 여러 개면 규칙도 여러 개가 된다
create temporary table notion_row (
    n            serial,
    category     text,
    industry     text,
    triggers     text[],
    trigger_text text,
    supplies     text[],
    stocks       text[],
    forecast     boolean,
    lead_min     integer,
    lead_max     integer,
    demand       text
) on commit drop;

insert into notion_row (category, industry, triggers, trigger_text, supplies, stocks, forecast, lead_min, lead_max, demand) values
    ('음식점', '카페', '{"HEATWAVE"}', '폭염', '{"얼음","아이스컵","빨대","캐리어"}', '{}', true, 3, 3, 'UP'),
    ('음식점', '카페', '{"COLD_WAVE"}', '한파', '{"핫컵","핫컵 리드","컵홀더"}', '{}', true, 3, 3, 'UP'),
    ('음식점', '카페', '{"FESTIVAL","PERFORMANCE","CROWD_SURGE"}', '축제·공연·혼잡도 급증', '{"테이크아웃 컵","캐리어","진동벨","냅킨","화장실 소모품"}', '{}', false, 7, 7, 'UP'),
    ('음식점', '카페', '{"VALENTINES_DAY","PEPERO_DAY","CHRISTMAS"}', '발렌타인·빼빼로데이·크리스마스', '{"선물 상자","리본","OPP 봉투"}', '{}', false, 14, 14, 'UP'),
    ('음식점', '카페', '{"SCHOOL_VACATION"}', '방학(대학가·학원가)', '{"테이크아웃 컵"}', '{}', false, null, null, 'DOWN'),
    ('음식점', '베이커리', '{"CHRISTMAS"}', '크리스마스', '{"케이크 상자","케이크 받침","초","케이크 칼","쇼핑백"}', '{}', false, 21, 21, 'STRONG_UP'),
    ('음식점', '베이커리', '{"PARENTS_DAY","TEACHERS_DAY","GRADUATION"}', '어버이날·스승의날·졸업', '{"케이크 상자","선물 포장지"}', '{}', false, 14, 14, 'UP'),
    ('음식점', '베이커리', '{"VALENTINES_DAY","WHITE_DAY","PEPERO_DAY"}', '발렌타인·화이트·빼빼로데이', '{"선물 상자","리본","OPP 봉투"}', '{}', false, 14, 14, 'UP'),
    ('음식점', '베이커리', '{"CSAT"}', '수능', '{"선물 포장지"}', '{}', false, 14, 14, 'UP'),
    ('음식점', '한식·국밥', '{"COLD_WAVE"}', '한파', '{"국물 포장 용기","미끄럼방지 매트"}', '{}', true, 3, 3, 'UP'),
    ('음식점', '한식·국밥', '{"SEOLLAL","CHUSEOK"}', '설·추석', '{"명절음식 포장 용기","실링 필름","식용유"}', '{}', false, 21, 21, 'UP'),
    ('음식점', '한식·국밥', '{"RAIN","FINE_DUST"}', '비·미세먼지', '{"배달 용기","배달 봉투","봉인 스티커"}', '{}', true, 1, 1, 'UP'),
    ('음식점', '고깃집', '{"YEAR_END","GRADUATION"}', '연말 송년회·졸업 회식', '{"잔","손님용 앞치마","숯","착화제","물티슈"}', '{}', false, 14, 14, 'UP'),
    ('음식점', '고깃집', '{"KIMJANG"}', '김장철(보쌈을 함께 할 때)', '{"고기 포장 트레이"}', '{}', false, 14, 14, 'UP'),
    ('음식점', '고깃집', '{"SUMMER_VACATION"}', '휴가철(캠핑 포장)', '{"고기 포장 트레이","아이스팩"}', '{}', false, 14, 14, 'UP'),
    ('음식점', '치킨·튀김', '{"SPORTS"}', '스포츠 경기·중계', '{"치킨 박스","치킨무 용기","소스컵","손잡이 봉투","튀김유"}', '{}', false, 3, 3, 'STRONG_UP'),
    ('음식점', '치킨·튀김', '{"RAIN","COLD_WAVE"}', '비·한파', '{"배달 용기","배달 봉투","봉인 스티커"}', '{}', true, 1, 1, 'UP'),
    ('음식점', '치킨·튀김', '{"FESTIVAL"}', '축제', '{"치킨 박스","손님용 비닐장갑"}', '{}', false, 7, 7, 'UP'),
    ('음식점', '치킨·튀김', '{"TRAFFIC_CONTROL"}', '교통통제·마라톤', '{"배달 용기"}', '{}', false, null, null, 'DOWN'),
    ('음식점', '분식·김밥', '{"PICNIC_SEASON"}', '봄·가을 소풍·나들이', '{"김밥 호일","포장 용기","일회용 수저"}', '{}', false, 3, 3, 'UP'),
    ('음식점', '분식·김밥', '{"SCHOOL_VACATION"}', '방학(학원가)', '{"포장 용기"}', '{}', false, null, null, 'DOWN'),
    ('음식점', '중식', '{"GRADUATION","SCHOOL_ENTRANCE"}', '졸업·입학', '{"랩","나무젓가락"}', '{}', false, 7, 7, 'UP'),
    ('음식점', '중식', '{"RAIN","COLD_WAVE"}', '비·한파', '{"랩","배달 봉투"}', '{}', true, 1, 1, 'UP'),
    ('음식점', '주점·포차', '{"YEAR_END"}', '연말 송년회', '{"잔","부탄가스","물티슈","얼음"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('음식점', '주점·포차', '{"SPORTS"}', '대형 스포츠 중계', '{"잔","얼음","포장 용기"}', '{}', false, 3, 3, 'UP'),
    ('음식점', '주점·포차', '{"FESTIVAL"}', '축제', '{"포장 용기","쓰레기봉투"}', '{}', false, 7, 7, 'UP'),
    ('음식점', '해물·조개구이', '{"WINTER"}', '겨울 제철(11~2월)', '{"숯","석쇠","손님용 비닐장갑","껍데기통"}', '{}', false, 7, 7, 'UP'),
    ('음식점', '해물·조개구이', '{"HEATWAVE"}', '폭염', '{"얼음","아이스팩","스티로폼 박스"}', '{}', true, 3, 3, 'UP'),
    ('음식점', '배달 전문', '{"RAIN","COLD_WAVE","FINE_DUST"}', '비·한파·미세먼지', '{"배달 용기","배달 봉투","봉인 스티커","보온·보냉 가방"}', '{}', true, 1, 3, 'STRONG_UP'),
    ('음식점', '배달 전문', '{"SPORTS"}', '대형 스포츠 중계', '{"배달 용기","배달 봉투"}', '{}', false, 3, 3, 'UP'),
    ('음식점', '배달 전문', '{"TRAFFIC_CONTROL"}', '교통통제·마라톤', '{"배달 용기"}', '{}', false, null, null, 'DOWN'),
    ('소매점', '편의점·슈퍼', '{"FESTIVAL","PERFORMANCE","CROWD_SURGE"}', '축제·공연·혼잡도 급증', '{"비닐봉투","쓰레기봉투","영수증 롤지"}', '{"생수","주류","돗자리","보조배터리"}', false, 7, 7, 'STRONG_UP'),
    ('소매점', '편의점·슈퍼', '{"RAIN"}', '비', '{"우산 비닐 커버","미끄럼방지 매트"}', '{"우산","우비"}', true, 1, 1, 'UP'),
    ('소매점', '편의점·슈퍼', '{"COLD_WAVE"}', '한파', '{"제설제","미끄럼방지 매트"}', '{"핫팩"}', true, 3, 3, 'UP'),
    ('소매점', '편의점·슈퍼', '{"HEATWAVE"}', '폭염', '{"얼음"}', '{"생수","아이스크림"}', true, 3, 3, 'UP'),
    ('소매점', '편의점·슈퍼', '{"PEPERO_DAY","VALENTINES_DAY","WHITE_DAY"}', '빼빼로·발렌타인·화이트데이', '{"선물 포장지","리본","쇼핑백","진열 POP"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('소매점', '편의점·슈퍼', '{"SUMMER_VACATION"}', '휴가철(관광지 상권)', '{"비닐봉투","얼음"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '정육점', '{"SEOLLAL","CHUSEOK"}', '설·추석', '{"선물 상자","아이스팩","보자기","택배 박스","택배 송장"}', '{}', false, 30, 30, 'STRONG_UP'),
    ('소매점', '정육점', '{"KIMJANG"}', '김장철', '{"고기 포장 트레이","랩"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '정육점', '{"HEATWAVE"}', '폭염', '{"아이스팩","보냉백"}', '{}', true, 3, 3, 'UP'),
    ('소매점', '정육점', '{"SUMMER_VACATION"}', '휴가철(캠핑·바비큐)', '{"진공 포장 봉투","아이스팩"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '수산물', '{"SEOLLAL","CHUSEOK"}', '설·추석', '{"선물 상자","스티로폼 박스","아이스팩"}', '{}', false, 30, 30, 'STRONG_UP'),
    ('소매점', '수산물', '{"KIMJANG"}', '김장철', '{"굴·새우젓 포장 용기","비닐봉투"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '수산물', '{"HEATWAVE"}', '폭염', '{"얼음","아이스팩"}', '{}', true, 3, 3, 'UP'),
    ('소매점', '청과', '{"SEOLLAL","CHUSEOK"}', '설·추석', '{"선물 상자","과일 완충 캡","보자기","택배 박스"}', '{}', false, 30, 30, 'STRONG_UP'),
    ('소매점', '청과', '{"SUMMER"}', '여름(수박철)', '{"수박 망","손잡이 봉투"}', '{}', false, 7, 7, 'UP'),
    ('소매점', '반찬가게', '{"KIMJANG"}', '김장철', '{"김치 비닐","김치통","실링 필름","아이스박스"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('소매점', '반찬가게', '{"SEOLLAL","CHUSEOK"}', '설·추석', '{"명절음식 포장 용기","실링 필름"}', '{}', false, 21, 21, 'STRONG_UP'),
    ('소매점', '반찬가게', '{"HEATWAVE"}', '폭염', '{"아이스팩"}', '{}', true, 3, 3, 'UP'),
    ('소매점', '꽃집', '{"GRADUATION","SCHOOL_ENTRANCE"}', '졸업·입학', '{"꽃 포장지","리본","쇼핑백"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('소매점', '꽃집', '{"PARENTS_DAY","TEACHERS_DAY"}', '어버이날·스승의날', '{"꽃 포장지","리본","플로랄폼","바구니","메시지 카드"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('소매점', '꽃집', '{"VALENTINES_DAY","CHRISTMAS"}', '발렌타인·크리스마스', '{"꽃 포장지","리본","쇼핑백"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '꽃집', '{"PERFORMANCE"}', '콘서트·공연', '{"꽃 포장지"}', '{}', false, 7, 7, 'UP'),
    ('소매점', '약국', '{"FINE_DUST"}', '미세먼지·황사', '{"쇼핑백"}', '{"마스크"}', true, 1, 1, 'UP'),
    ('소매점', '약국', '{"FLU_SEASON"}', '독감 시즌(한파)', '{"약봉투","약포지"}', '{}', false, 7, 7, 'UP'),
    ('소매점', '약국', '{"YEAR_END"}', '연말 송년회', '{}', '{"숙취해소제"}', false, 7, 7, 'UP'),
    ('소매점', '문구·완구', '{"SCHOOL_START"}', '개학(3월)', '{"가격 라벨","쇼핑백"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '문구·완구', '{"CHRISTMAS","CHILDRENS_DAY"}', '크리스마스·어린이날', '{"선물 포장지","리본","쇼핑백"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('소매점', '문구·완구', '{"PEPERO_DAY","VALENTINES_DAY"}', '빼빼로·발렌타인데이', '{"선물 포장지","리본","선물 상자"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '의류·잡화', '{"SEASON_CHANGE"}', '환절기(시즌 교체)', '{"옷걸이","택","쇼핑백"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '의류·잡화', '{"CHRISTMAS","YEAR_END"}', '크리스마스·연말', '{"선물 상자","쇼핑백"}', '{}', false, 14, 14, 'UP'),
    ('소매점', '철물·생활용품', '{"MOVING_SEASON"}', '이사철', '{"이사 박스","테이프"}', '{}', false, 7, 7, 'UP'),
    ('소매점', '철물·생활용품', '{"COLD_WAVE","HEAVY_SNOW"}', '한파·폭설', '{}', '{"제설제","제설 도구"}', true, 7, 7, 'UP'),
    ('단체·개인서비스업', '미용실', '{"SEOLLAL","CHUSEOK"}', '설·추석', '{"염모제","펌제","수건","넥지"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('단체·개인서비스업', '미용실', '{"GRADUATION","SCHOOL_ENTRANCE"}', '졸업·입학', '{"시술 소모품","넥지"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '미용실', '{"CSAT"}', '수능 직후', '{"염모제","탈색제"}', '{}', false, 7, 7, 'UP'),
    ('단체·개인서비스업', '미용실', '{"CHRISTMAS","YEAR_END"}', '크리스마스·연말', '{"시술 소모품"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '미용실', '{"WEDDING_SEASON"}', '웨딩 시즌(봄·가을)', '{"업스타일 핀","헤어 스프레이"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '미용실', '{"RAIN","COLD_WAVE"}', '비·한파', '{"미끄럼방지 매트","우산 비닐 커버"}', '{}', true, 1, 1, 'UP'),
    ('단체·개인서비스업', '네일숍', '{"SUMMER_VACATION"}', '휴가철(여름)', '{"젤","네일 파일","버퍼","페디큐어 소모품"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('단체·개인서비스업', '네일숍', '{"CHRISTMAS","YEAR_END","VALENTINES_DAY"}', '크리스마스·연말·발렌타인', '{"시즌 컬러 젤","네일 파츠"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '네일숍', '{"WEDDING_SEASON","GRADUATION"}', '웨딩·졸업', '{"젤","네일 파일"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '피부관리·왁싱', '{"SUMMER_VACATION"}', '휴가철', '{"왁스","부직포 스트립","일회용 속옷"}', '{}', false, 21, 21, 'STRONG_UP'),
    ('단체·개인서비스업', '피부관리·왁싱', '{"WEDDING_SEASON","YEAR_END"}', '웨딩 시즌·연말', '{"팩","앰플"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '세탁소', '{"SEASON_CHANGE"}', '환절기(4~5월·10~11월)', '{"의류 비닐 커버","옷걸이","번호 택","드라이클리닝 용제"}', '{}', false, 7, 7, 'STRONG_UP'),
    ('단체·개인서비스업', '세탁소', '{"GRADUATION","SCHOOL_ENTRANCE"}', '졸업·입학(교복)', '{"의류 비닐 커버","옷걸이"}', '{}', false, 7, 7, 'UP'),
    ('단체·개인서비스업', '수선집', '{"SCHOOL_ENTRANCE"}', '입학(교복 수선·2~3월)', '{"실","지퍼","단추","수선 봉투"}', '{}', false, 14, 14, 'STRONG_UP'),
    ('단체·개인서비스업', '수선집', '{"SEASON_CHANGE"}', '환절기', '{"실","지퍼"}', '{}', false, 7, 7, 'UP'),
    ('단체·개인서비스업', '반려동물 미용', '{"SUMMER_VACATION"}', '휴가철(호텔링·미용)', '{"반려동물 샴푸","배변패드","수건"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '반려동물 미용', '{"SEOLLAL","CHUSEOK"}', '설·추석(연휴 전 미용)', '{"반려동물 샴푸","클리퍼 날"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '목욕탕·사우나', '{"COLD_WAVE"}', '한파', '{"수건","목욕 일회용품","세정제"}', '{}', false, 7, 7, 'UP'),
    ('단체·개인서비스업', '목욕탕·사우나', '{"SEOLLAL","CHUSEOK"}', '설·추석 전', '{"수건","목욕 일회용품"}', '{}', false, 7, 7, 'UP'),
    ('단체·개인서비스업', '자동차 정비', '{"WINTER"}', '한파 전(11월)', '{"부동액","겨울용 워셔액","배터리"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '자동차 정비', '{"RAINY_SEASON"}', '장마 전', '{"와이퍼"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '자동차 정비', '{"SUMMER_VACATION","SEOLLAL","CHUSEOK"}', '휴가철·명절 전(장거리 점검)', '{"엔진오일","필터","웨스"}', '{}', false, 14, 14, 'UP'),
    ('단체·개인서비스업', '자동차 정비', '{"HEAVY_SNOW"}', '폭설', '{"시트 커버","발판 매트지"}', '{}', true, 3, 3, 'UP');

insert into industry (category, name)
select category, industry from notion_row group by category, industry order by min(n);

insert into supply (name)
select distinct unnest(supplies || stocks) from notion_row;

insert into supply_rule (industry_id, trigger_code, trigger_text, forecast, lead_days_min, lead_days_max, demand)
select i.id, t.code, r.trigger_text, r.forecast, r.lead_min, r.lead_max, r.demand
from notion_row r
join industry i on i.name = r.industry
cross join unnest(r.triggers) as t(code)
order by r.n;

insert into supply_rule_item (rule_id, supply_id, kind, sort_order)
select sr.id, s.id, item.kind, item.ord - 1
from notion_row r
join industry i on i.name = r.industry
join supply_rule sr on sr.industry_id = i.id and sr.trigger_code = any (r.triggers)
cross join lateral (
    select name, 'SUPPLY' as kind, ord from unnest(r.supplies) with ordinality as x(name, ord)
    union all
    select name, 'STOCK', ord from unnest(r.stocks) with ordinality as x(name, ord)
) item
join supply s on s.name = item.name;
