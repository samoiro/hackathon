-- 대표 품종별 제철 월. 미확정·참고·저장품 항목은 제외. 출처는 각 품목 행의 주석에 보존.
DO $migration$
BEGIN
    CREATE TEMP TABLE ingredient_season_seed (
        item_code text PRIMARY KEY,
        item_name text NOT NULL,
        variety_name text NOT NULL,
        months integer[] NOT NULL
    ) ON COMMIT DROP;
    INSERT INTO ingredient_season_seed VALUES
        -- 고구마: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219875
        ('151', '고구마', '밤', ARRAY[9,10]),
        -- 배추: https://www.korea.kr/news/policyNewsView.do?newsId=148969042
        ('211', '배추', '여름(고랭지)', ARRAY[7,8,9,10]),
        -- 양배추: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219841
        ('212', '양배추', '양배추', ARRAY[5,6,7]),
        -- 시금치: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219817
        ('213', '시금치', '시금치', ARRAY[11,12,1,2]),
        -- 얼갈이배추: https://www.kamis.or.kr/customer/archive/archive.do?action=detail&archiveNo=114
        ('215', '얼갈이배추', '얼갈이배추', ARRAY[11,12]),
        -- 갓: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219907
        ('216', '갓', '갓', ARRAY[9,10,11,12]),
        -- 연근: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219819
        ('217', '연근', '연근', ARRAY[9,10,11,12,1,2,3]),
        -- 우엉: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219816
        ('218', '우엉', '우엉', ARRAY[1,2,3]),
        -- 수박: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219887
        ('221', '수박', '수박', ARRAY[6,7,8]),
        -- 호박: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219863
        ('224', '호박', '애호박', ARRAY[6,7,8,9,10]),
        -- 딸기: https://ares.gangwon.kr/gwares/living_culture/gw_recipe/seasonal_specialty?articleSeq=27130
        ('226', '딸기', '설향', ARRAY[11,12,1,2,3,4,5]),
        -- 무: https://www.krei.re.kr/attach/pdf/605ba745-99dd-2a94-e054-b09928988b3c.pdf
        ('231', '무', '고랭지', ARRAY[7,8,9,10]),
        -- 당근: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219814
        ('232', '당근', '무세척(국산)', ARRAY[10,11,12]),
        -- 열무: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219872
        ('233', '열무', '열무', ARRAY[5,6]),
        -- 붉은고추: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219909
        ('243', '붉은고추', '붉은고추', ARRAY[6,7,8,9,10,11]),
        -- 파: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219944
        ('246', '파', '대파', ARRAY[9,10,11,12]),
        -- 가지: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219855
        ('251', '가지', '가지', ARRAY[6,7,8,9,10]),
        -- 미나리: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219825
        ('252', '미나리', '미나리', ARRAY[3,4]),
        -- 부추: https://www.foodnuri.go.kr/portal/bbs/B0000283/view.do?menuNo=300063&nttId=219866
        ('254', '부추', '부추', ARRAY[6,7,8,9,10]),
        -- 브로콜리: https://www.foodnuri.go.kr/portal/bbs/B0000277/view.do?menuNo=300088&nttId=214502&pageIndex=340
        ('280', '브로콜리', '브로콜리(국산)', ARRAY[11,12,1,2,3]),
        -- 사과: https://www.rda.go.kr/board/board.do?dataNo=100000730345&mode=view&prgId=day_farmprmninfoEntry
        ('411', '사과', '홍로', ARRAY[9]),
        -- 감귤: https://agri.jeju.go.kr/files/board/4de5d275-7731-4313-aaa5-ec043d39a29f.pdf
        ('415', '감귤', '노지', ARRAY[11,12]),
        -- 단감: https://www.rda.go.kr/middlePopOpenPopNongsaroDBView.do?no=1996
        ('416', '단감', '부유', ARRAY[10,11]),
        -- 오렌지: https://www.citrusaustralia.com.au/buying-australian/season/
        ('421', '오렌지', '네이블 호주', ARRAY[6,7,8,9,10]),
        -- 고등어: https://fsis.go.kr/front/contents/cmsView.do?cate_id=0301&cnts_id=13682&select_list_no=123
        ('611', '고등어', '국산(신선 냉장)', ARRAY[9,10,11]),
        -- 갈치: https://fsis.go.kr/front/contents/cmsView.do?cate_id=0301&cnts_id=5651&select_list_no=155
        ('613', '갈치', '국산(냉장)', ARRAY[10,11,12]),
        -- 조기: https://www.fsis.go.kr/front/contents/cmsView.do?cate_id=0301&cnts_id=34244&select_list_no=1
        ('614', '조기', '참조기(신선 냉장)', ARRAY[9,10,11,12,1,2]),
        -- 전복: https://fsis.go.kr/front/contents/cmsView.do?cate_id=0301&cnts_id=25483&select_list_no=81
        ('653', '전복', '냉장', ARRAY[8,9,10]),
        -- 가리비: https://www.pcafrp.go.kr/boardDownload.es?bid=0001&list_no=215&mid=a10402000000&seq=3
        ('659', '가리비', '해만가리비(홍가리비)', ARRAY[11,12,1,2,3]),
        -- 바지락: https://snvision.seongnam.go.kr/6282
        ('661', '바지락', '냉장', ARRAY[2,3,4]),
        -- 꼬막: https://www.fsis.go.kr/front/contents/cmsView.do?cate_id=0301&cnts_id=13870
        ('665', '꼬막', '국산(새꼬막)', ARRAY[11,12,1,2,3,4]);

    IF EXISTS (
        SELECT 1 FROM ingredient_season_seed seed
        LEFT JOIN ingredient ON ingredient.item_code = seed.item_code
        WHERE ingredient.id IS NULL OR ingredient.item_name <> seed.item_name
            OR ingredient.variety_name <> seed.variety_name
    ) THEN
        RAISE EXCEPTION '제철 원자재의 대표 품목·품종이 일치하지 않습니다';
    END IF;
    IF (SELECT count(*) FROM ingredient_rule WHERE trigger_code ~ '^(IN_SEASON_MONTH_|BEFORE_SEASON_START_MONTH_)(0[1-9]|1[0-2])$') <> 24
        OR (SELECT count(*) FROM ingredient_rule WHERE trigger_code IN (
            'PRICE_DROP_ONE_WEEK_UNDER_5_PERCENT', 'PRICE_DROP_ONE_WEEK_5_TO_10_PERCENT',
            'PRICE_DROP_ONE_WEEK_AT_LEAST_10_PERCENT', 'PRICE_RISE_ONE_WEEK', 'PRICE_BELOW_FOUR_WEEKS'
        )) <> 5 THEN
        RAISE EXCEPTION '필수 원자재 규칙 29개가 필요합니다';
    END IF;

    WITH targets AS (
        SELECT ingredient.id AS ingredient_id, ingredient.item_code,
            'IN_SEASON_MONTH_' || lpad(month::text, 2, '0') AS trigger_code
        FROM ingredient_season_seed seed
        JOIN ingredient USING (item_code)
        CROSS JOIN LATERAL unnest(seed.months) AS month
        UNION ALL
        SELECT ingredient.id, ingredient.item_code,
            'BEFORE_SEASON_START_MONTH_' || lpad(seed.months[1]::text, 2, '0')
        FROM ingredient_season_seed seed JOIN ingredient USING (item_code)
        UNION ALL
        SELECT ingredient.id, ingredient.item_code, ingredient_rule.trigger_code
        FROM ingredient CROSS JOIN ingredient_rule
        WHERE ingredient_rule.trigger_code IN (
            'PRICE_DROP_ONE_WEEK_UNDER_5_PERCENT', 'PRICE_DROP_ONE_WEEK_5_TO_10_PERCENT',
            'PRICE_DROP_ONE_WEEK_AT_LEAST_10_PERCENT', 'PRICE_RISE_ONE_WEEK', 'PRICE_BELOW_FOUR_WEEKS'
        )
    )
    INSERT INTO ingredient_rule_item(rule_id, ingredient_id, sort_order)
    SELECT ingredient_rule.id, targets.ingredient_id,
        row_number() OVER (PARTITION BY ingredient_rule.id ORDER BY targets.item_code COLLATE "C")::smallint
    FROM targets JOIN ingredient_rule USING (trigger_code)
    ON CONFLICT (rule_id, ingredient_id) DO NOTHING;
END
$migration$;
