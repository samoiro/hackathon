# 성수동 시연용 공공 API 원본 응답

시연 당일 공공 API가 느리거나 실패할 때 대신 보여 줄 데이터입니다. 아직 이 파일을 읽는 코드는 없습니다(대체 로직은 본선에서 만듭니다).

- 받은 시각: **2026-11-21(토) 09:50~09:51 KST**
- 예시 가게: 성수동 한식당, 서울 성동구 서울숲2길 32, 위도 37.5446 · 경도 127.0446, 업종 한식·국밥
- 행정동: 성수1가1동 `1120065000` (카카오 좌표 → 행정동 H 코드)
- 원자재: 쌀 · 돼지 · 배추 + 업종 기본 파 / 부자재: 국물 포장 용기 · 배달 봉투 (부자재는 공공 API가 없어 받은 것 없음)
- 모든 파일은 응답 본문을 그대로 `jq .`로 줄 바꿈만 한 것입니다. 바꾼 곳은 아래 "자르거나 지운 것"에 적은 두 가지뿐입니다.
- 인증키는 요청 URL·헤더에만 쓰여서 응답에 없습니다. 네 키 모두 원문·URL 인코딩 형태로 이 폴더를 grep해 0건임을 확인했습니다.

## 파일

| 파일 | API (서버 클라이언트) | 요청 조건 | 내용 |
| --- | --- | --- | --- |
| `culture-events.json` | 서울시 문화행사 `aiViewCulture` (`SeoulOpenApiClient.list`) | 전체 19,782건을 받아 가게 **3km 안**, 기간이 2026-11-21~11-28과 겹치는 행만 남김 | 15건. 형식은 원본 `{aiViewCulture: {list_total_count, RESULT, row}}`이고 `list_total_count`는 자르기 전 전체 건수 |
| `agro-price-trends-111.json` | aT 가격 추이 `B552845/priceSequel` (`AgroPriceClient`) | 조사일 `20261120`, `item_cd` 111 쌀, 구분·품종·등급 전체 | 4건 (소매·중도매, 10kg·20kg) |
| `agro-price-trends-4304.json` | 〃 | 〃 4304 돼지 | 4건 (소매 앞다리·삼겹살·갈비·목심, 100g) |
| `agro-price-trends-211.json` | 〃 | 〃 211 배추 | 3건 (가을배추 소매 1포기, 중도매 10kg). 4주 전 가격은 `null` |
| `agro-price-trends-246.json` | 〃 | 〃 246 파 | 6건 (대파·쪽파, 소매·중도매, 1kg) |
| `kakao-coord2regioncode.json` | 카카오 좌표 → 행정구역 (`KakaoLocalClient`) | x=127.0446, y=37.5446 | 법정동(B) 성수동1가, 행정동(H) 성수1가1동 `1120065000` |
| `kweather-warnings.json` | 케이웨더 동별 기상특보 `kw-warning1` (`KweatherClient`) | 행정동 `1120065000` | HTTP 206 `{"error": "001"}`: 발효 중인 특보 없음. 클라이언트는 이 응답을 빈 객체로 바꿉니다 |
| `kweather-short-forecast.json` | 케이웨더 단기예보 `kw-fct-dl1` | 〃 | 발표 05시, 오늘 오전부터 오전/오후 6칸 (기온 −5~9℃, 23일 비 60%) |
| `kweather-weekly-forecast.json` | 케이웨더 주간예보 `kw-fct-wl1` | 〃 | 발표 06시, 11-24 ~ 12-01 13칸 |
| `citydata-seoul-forest-park.json` | 서울 실시간 도시데이터 `citydata` (`SeoulOpenApiClient.realtime`) | 장소 `서울숲공원` (POI101) | 09:20 기준 인구 혼잡도 '여유', 도로·주차·지하철·버스·날씨·상권 등 전체. 컨트롤러는 이 중 `CITYDATA`만 돌려줍니다 |
| `citydata-seongsu-cafe-street.json` | 〃 | 장소 `성수카페거리` (POI068) | 〃 |
| `bizinfo-support-programs.json` | 기업마당 지원사업 `1421000/bizinfo/pblancBsnsService` (`DataGoKrClient`) | `hashtags=소상공인`, 1페이지 100건 | 전체 207건 중 최신 등록순 100건 |

## 자르거나 지운 것

1. **문화행사를 3km·8일로 자름.** API에 위치 조건이 없어 전체를 받아 걸렀습니다. 계약의 행사 후보 기준(`ai-server/docs/API_CONTRACT_DRAFT.md` "행사 후보와 할 일 유형": 분류 13개, 진행 중이거나 7일 안에 시작, 1km → 1.5km)으로는 **0건**입니다. 분류와 상관없이 1.5km 안에 있는 행사는 247건이지만 모두 이미 끝났고, 30일 뒤까지 넓혀도 0건입니다. 그래서 반경을 3km로 넓히고 분류는 거르지 않았습니다. 이 중 후보 분류는 2건뿐입니다.
   - 뮤지컬/오페라 `[성동문화재단] 오페레타 [유쾌한 미망인]`, 소월아트홀, 약 1.8km, 11-27~28
   - 축제-문화/예술 `2026 서울어텀페스타`, 서울 전역, 약 2.8km, 09-18~11-29
   - 기준대로라면 시연 흐름은 행사 없음 → 시즌 행사 → 날씨(`EventSource`)로 넘어갑니다. 거리는 하버사인으로 계산했고, LAT·LOT가 뒤바뀐 행은 작은 값을 위도로 봅니다.
2. **연락처를 지움.** 문화행사 `INQUIRY`, 지원사업 `refrncNm`(문의처)·`reqstMthPapersCn`(접수 방법)의 이메일은 `[이메일 삭제]`, 전화번호(`02-123-4567`, `1588-1234` 꼴)는 `[전화번호 삭제]`로 바꿨습니다. 기관 대표번호라도 담당자 직통일 수 있어 모두 지웠고, `120`·`1357` 같은 3~4자리 대표번호와 내선 번호 조각(`, 4863`)은 남아 있습니다. 다른 필드에는 이메일·전화번호가 없습니다.

## 알아 둘 점

- aT 조사일은 2026-11-20(금)입니다. 토요일인 받은 날에는 조사 자료가 없습니다.
- DB `ingredient` 시드와 품종이 다른 품목이 있습니다. 쌀(소매 10kg 상품), 돼지(앞다리), 파(대파)는 시드와 같은 행이 있지만, 배추 시드는 `vrty_cd 02 여름(고랭지)`이고 11월 응답에는 `03 가을`만 있습니다.
- 로컬 DB `city_data_area`가 비어 있어서 가까운 장소는 이름으로 골랐습니다. 가게 바로 옆인 서울숲공원과, 같은 성수동 상권인 성수카페거리입니다.
- 실시간 도시데이터와 단기예보는 받은 시각 기준입니다. 시연에서 쓸 때는 "11-21 09:50 기준"처럼 시각을 함께 보여 주는 게 맞습니다.

## 다시 받는 방법

레포 루트에서 bash로 실행합니다. 키는 `server/.env`에서 읽고, `curl`과 `jq`가 필요합니다. 날짜(`TODAY`, `UNTIL`, `DAY`)는 받는 날에 맞게 바꾸세요. aT `DAY`는 가장 최근 평일 조사일입니다. 받은 뒤 아래 확인 명령으로 키가 들어가지 않았는지 꼭 보세요.

```bash
set -a; . server/.env; set +a
D=server/src/main/resources/demo/seongsu
TODAY=2026-11-21; UNTIL=2026-11-28; DAY=20261120
MASK='def mask: if type == "string" then gsub("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+"; "[이메일 삭제]") | gsub("(\\(?0\\d{1,2}\\)?[-. )]?)?\\d{3,4}[-. ]\\d{4}"; "[전화번호 삭제]") else . end;'

# 1. 서울시 문화행사: 전체(약 2만 건, 1000건씩)를 받아 가게 3km 안 · 기간이 TODAY~UNTIL과 겹치는 행만 남김
for s in $(seq 1 1000 20000); do
  curl -sS "http://openapi.seoul.go.kr:8088/$SEOUL_API_KEY/json/aiViewCulture/$s/$((s+999))"
done | jq -s --argjson lat 37.5446 --argjson lon 127.0446 --arg from "$TODAY" --arg to "$UNTIL" "$MASK"'
  def rad: . * 3.141592653589793 / 180;
  def dist($a; $b): ((($a - $lat) | rad) / 2 | sin) as $x | ((($b - $lon) | rad) / 2 | sin) as $y
    | ($x * $x + ($lat | rad | cos) * ($a | rad | cos) * $y * $y) | sqrt | asin * 2 * 6371000;
  {aiViewCulture: {list_total_count: .[0].aiViewCulture.list_total_count, RESULT: .[0].aiViewCulture.RESULT,
    row: [.[].aiViewCulture.row[]?
      | select((.END_DATE // "")[:10] >= $from and (.STRTDATE // "9")[:10] <= $to)
      | ([.LAT, .LOT] | map(tonumber? // empty) | sort) as $c   # 일부 행은 LAT·LOT가 뒤바뀜
      | select(($c | length) == 2 and dist($c[0]; $c[1]) <= 3000)
      | .INQUIRY |= mask]}}' > $D/culture-events.json

# 2. aT 가격 추이: 쌀 111 · 돼지 4304 · 배추 211 · 파 246 (모든 구분·품종·등급), 최근 조사일 DAY
for code in 111 4304 211 246; do
  curl -sSg -G "https://apis.data.go.kr/B552845/priceSequel/info" --data-urlencode "serviceKey=$DATA_GO_KR_API_KEY" \
    -d pageNo=1 -d numOfRows=100 -d returnType=JSON \
    --data-urlencode "cond[exmn_ymd::EQ]=$DAY" --data-urlencode "cond[item_cd::EQ]=$code" | jq . > $D/agro-price-trends-$code.json
done

# 3. 카카오 좌표 → 행정동(H 코드 1120065000 성수1가1동), 케이웨더 특보·단기·주간
curl -sS -H "Authorization: KakaoAK $KAKAO_REST_API_KEY" \
  "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=127.0446&y=37.5446" | jq . > $D/kakao-coord2regioncode.json
HCODE=$(jq -r '.documents[] | select(.region_type == "H") | .code' $D/kakao-coord2regioncode.json)
for api in kw-warning1:warnings kw-fct-dl1:short-forecast kw-fct-wl1:weekly-forecast; do
  curl -sS -H "Kweather-Api-Key: $KWEATHER_API_KEY" "https://apigw.kweather.co.kr/${api%%:*}/$HCODE" | jq . > "$D/kweather-${api#*:}.json"
done

# 4. 서울 실시간 도시데이터: 서울숲공원 · 성수카페거리
for area in 서울숲공원:seoul-forest-park 성수카페거리:seongsu-cafe-street; do
  curl -sS "http://openapi.seoul.go.kr:8088/$SEOUL_API_KEY/json/citydata/1/5/$(jq -rn --arg s "${area%%:*}" '$s | @uri')" \
    | jq . > "$D/citydata-${area#*:}.json"
done

# 5. 기업마당 지원사업: 해시태그 소상공인, 최신 등록순 100건. 문의처·접수 방법의 이메일·전화번호는 지움
curl -sS -G "https://apis.data.go.kr/1421000/bizinfo/pblancBsnsService" --data-urlencode "serviceKey=$DATA_GO_KR_API_KEY" \
  -d dataType=json -d pageNo=1 -d numOfRows=100 --data-urlencode "hashtags=소상공인" \
  | jq "$MASK"'.response.body.items.item |= map(.refrncNm |= mask | .reqstMthPapersCn |= mask)' > $D/bizinfo-support-programs.json
```

확인: 모든 파일이 파싱되는지, 키가 없는지 봅니다(키 값은 출력하지 않고 건수만 출력).

```bash
D=server/src/main/resources/demo/seongsu; for f in $D/*.json; do jq -e . "$f" > /dev/null || echo "BAD $f"; done
bash -c 'set -a; . server/.env; for n in SEOUL_API_KEY DATA_GO_KR_API_KEY KWEATHER_API_KEY KAKAO_REST_API_KEY; do k="${!n}"; echo "$n $(cat server/src/main/resources/demo/seongsu/*.json | grep -cF -- "$k")"; done'
```
