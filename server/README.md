# server

Java 25 · Spring Boot 4.1 · PostgreSQL · Gradle

## 로컬 실행

Docker(Docker Desktop, colima 등)를 켠 뒤:

- IntelliJ: 상단 실행 구성에서 **Server** 선택 후 ▶ (`.run/Server.run.xml`)
- 터미널: `cd server && ./gradlew bootRun`

`compose.yaml`의 Postgres가 자동으로 뜨고 연결됩니다. (JDK 25가 없으면 Gradle이 자동 설치)

인증키는 `.env.example`을 복사해 `server/.env`에 넣으세요. (서울 열린데이터광장 키, 공공데이터포털 키)

<details><summary>colima를 쓰는 경우</summary>

```bash
brew install docker-compose                      # Spring Boot가 docker-compose 명령으로 Postgres를 띄움
colima start hackathon --cpu 2 --memory 2 --disk 20   # 이 프로젝트 전용 VM (Docker context도 자동 전환)
```
테스트(Testcontainers)는 아래 환경변수가 필요할 수 있어요.
```bash
export DOCKER_HOST=unix://$HOME/.colima/hackathon/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```
</details>

- API 명세서(Swagger UI): http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- 헬스체크: http://localhost:8080/actuator/health

## 공공데이터 API

응답 필드명은 원본 API 그대로입니다. 자세한 파라미터는 Swagger UI 참고.

| API | 원본 데이터 |
| --- | --- |
| `GET /api/seoul/necessaries-prices` | 서울시 생필품 농수축산물 가격 (OA-1170) |
| `GET /api/seoul/cultural-events` | 서울시 문화행사 정보 (OA-23041) |
| `GET /api/seoul/city-data/{장소}` | 서울시 실시간 도시데이터 (OA-21285) |
| `GET /api/seoul/population/{장소}` | 서울시 실시간 인구 (OA-21778) |
| `GET /api/seoul/commercial/{장소}` | 서울시 실시간 상권 현황 (OA-22385) |
| `GET /api/agro/price-trends?date=YYYYMMDD` | aT 가격 추이 정보 (data.go.kr 15156069) |

- `{장소}`: 장소명 또는 코드 (예: `광화문·덕수궁`, `POI009`), 서울시 주요 120개 장소 중 하나
- 목록 API는 `page`(1부터), `size`(최대 1000) 지원, 응답은 `{ totalCount, items }`
- 외부 API 오류는 502, 인증키 미설정은 503, 잘못된 파라미터는 400

## 재료와 가격 이력 적재

`ingredient`는 재료별 대표 규격 하나를 저장합니다. API 값을 저장하되 컬럼명은 풀어 씁니다.
`item_code/item_name`(품목), `category_code/category_name`(부류),
`trade_type_code/trade_type_name`(거래 구분), `variety_code/variety_name`(품종),
`grade_code/grade_name`(등급), `unit`(단위), `unit_size`(숫자형 단위 크기)를 사용합니다.
API의 `item_cd`, `ctgry_cd`, `se_cd`, `vrty_cd`, `grd_cd`, `unit_sz` 등은 적재 시 위 이름으로 매핑합니다.
`ingredient_price_history.price`는 해당 `unit_size × unit` 규격의 조사일 평균가격입니다.
서로 다른 규격의 가격을 그대로 비교하지 마세요.

기존 DB에는 `src/main/resources/db/migration/V8__ingredient_api_fields.sql`,
`V9__ingredient_descriptive_column_names.sql`을 순서대로 한 번씩 적용합니다.
기존 ID와 가격 이력은 유지하며, 원본 API 응답으로 확인한 대표 규격 114개를 역채움합니다.
일치하지 않는 기존 재료가 있으면 전체 변경을 취소합니다. 이 프로젝트는 마이그레이션을 자동 실행하지 않습니다.

모든 페이지를 합친 같은 조사일의 API 응답 JSON을 준비한 뒤, 저장소 루트에서 실행합니다.

```sh
node server/scripts/prepare-ingredient-prices.mjs <complete-api-response.json>
node --test server/scripts/prepare-ingredient-prices.test.mjs
```

생성된 `<조사일>-import.sql`을 DB에 실행하면 재료와 일별 가격 이력을 적재합니다.
`ingredient-price-basis.json`에도 대표 규격을 풀어 쓴 필드명으로 저장합니다.
이후 조회에서는 같은 코드·단위·단위 크기만 선택하며, 대표 규격이 응답에 없으면 건너뜁니다.
대표 규격 변경은 DB에서도 차단하고, 가격 미제공 값은 `NULL`로 저장합니다.

## 원자재 규칙 연결

`ingredient_rule_item`은 연결된 DB에 생성했고 총 740개를 적재했습니다.
제철 31개 원자재의 제철 월 119개·시작 한 달 전 31개, 전체 118개 원자재의 가격 규칙 590개입니다.
`ingredient-seasons.json`에 대표 품종별 제철 월과 출처를, `ingredient-rule-items.csv`에 실제 연결과 순서를 저장했습니다.

다른 DB에서는 기존 원자재·규칙 적재 후 `V16__ingredient_rule_item.sql`, `V17__ingredient_rule_item_seed.sql` 순으로 적용합니다.
`scripts/check-ingredient-rule-items.sql`은 초기 적재 건수·전체 가격 대상·연도 경계·중복 방지·양수 순서를 검증합니다.
제철 데이터나 연결을 운영 중 변경하면 초기 건수 검증 기준도 함께 수정해야 합니다.

초기 `sort_order`는 규칙별 품목 코드순이며 재적재로 덮어쓰지 않습니다.
가격 연결은 적용 가능한 대상입니다. 최신 가격에 따른 실제 조건 충족 여부는 서버에서 별도로 판정해야 합니다.
가격 수집 배치는 가격 이력을 갱신하고, 수집 성공 후 조건을 판정하는 방향을 권장합니다.
가격 변동으로 연결이나 고정 순서를 갱신하지 않습니다. 조건 판정과 주기 실행은 아직 구현하지 않았습니다.
상세 기준과 제한은 [DB 구조 문서](../docs/database-schema.md)에 정리했습니다.

## 문화행사 수집과 갱신

`cultural_event`는 실제 `aiViewCulture` 응답을 기준으로 만든 오프라인 행사 테이블입니다.
최초 DB 생성은 `src/main/resources/db/migration/V15__cultural_event.sql`을 한 번 적용합니다.
연결된 DB에는 이미 적용하고 18,838건을 적재했습니다(2026-11-20 한국 시간).
기존 `/api/seoul/cultural-events`는 원본 API 중계이며 DB 조회로 바꾸지 않았습니다.

저장소 루트에서 전체 페이지를 수집합니다. `SEOUL_API_KEY`는 환경변수 또는 `server/.env`를 사용합니다.

```sh
node --test server/scripts/prepare-cultural-events.test.mjs
node server/scripts/collect-cultural-events.mjs
```

출력된 `output` 폴더에는 원본 페이지, 제외 사유가 담긴 `report.json`, 적재용 `import.sql`이 생성됩니다.
수집만으로 DB를 변경하지 않습니다. DB 접속 환경변수(PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD 등)를 설정한 환경에서:

```sh
psql -v ON_ERROR_STOP=1 -f <output>/import.sql
psql -v ON_ERROR_STOP=1 -f server/scripts/check-cultural-events.sql
```

`HMPG_ADDR`의 `cultcode`로 upsert하므로 일정·제목이 바뀌어도 ID와 `created_datetime`은 유지합니다.
내용·활성 상태가 바뀔 때만 `updated_datetime`을 갱신합니다. API의 `RGSTDATE`는 원본 등록일입니다.
제목·장소에 온라인/비대면 등이 명시되거나 좌표·일정이 유효하지 않은 행사는 제외합니다.
누락·제외된 기존 행사는 삭제하지 않고 비활성화하며, 다시 유효해지면 활성화합니다.
전체 페이지 건수 불일치·식별자 누락·중복·빈 응답이면 적재 SQL을 생성하지 않습니다.

`coord`는 필수 `geography(Point,4326)`이며 경도 LOT, 위도 LAT 순입니다.
`ST_Distance`로 target 좌표와 미터 단위 거리를 계산하고 `ST_DWithin`으로 반경을 제한합니다.
`scripts/query-cultural-events.sql`에 활성 여부·기간·반경을 함께 적용한 조회 예시가 있습니다.

[공식 데이터셋](https://data.seoul.go.kr/dataList/OA-23041/S/1/datasetView.do)의 갱신 주기는 하루 1회입니다.
주기 수집은 하루 1회 전체 수집 성공 후 **그 실행에서 생성된** `import.sql`을 적용하는 방식으로 구성할 수 있습니다.
오래된 SQL 재적용은 최신 내용을 되돌릴 수 있으므로 실행을 겹치지 않게 하고 최신 결과만 순서대로 적용하세요.
주기 실행 자체는 아직 등록하지 않았습니다.

## 작업과 재시도 이력

연결된 DB에 V18__batch_job_history.sql을 적용했습니다. batch_job과 batch_job_history는 1:N 관계이며, 최초 실행은 retry_times=0입니다.
application.yml의 batch.max-retry-times는 3(최초 포함 최대 4회)입니다. running_time은 시작·종료 시각에서 자동 계산한 밀리초입니다.
검증: psql -v ON_ERROR_STOP=1 -f server/scripts/check-batch-job-history.sql (저장소 루트 기준). 테스트 데이터는 롤백합니다.
재시도 실행·상태 전이·중단 복구 로직은 아직 연결하지 않았습니다.

## 배포 (Render + Neon, 무료)

1. [Neon](https://neon.tech)에서 프로젝트 생성 (Region: **AWS Asia Pacific (Singapore)**)
   → Connection string에서 host, user, password 확인
2. [Render](https://render.com) → New → **Blueprint** → 이 레포 선택 (루트의 `render.yaml` 사용)
3. 입력 요청되는 변수 채우기
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-host>/neondb?sslmode=require
   SPRING_DATASOURCE_USERNAME=<neon user>
   SPRING_DATASOURCE_PASSWORD=<neon password>
   SEOUL_API_KEY=<서울 열린데이터광장 인증키>
   DATA_GO_KR_API_KEY=<공공데이터포털 인증키(Decoding)>
   CORS_ALLOWED_ORIGINS=https://<프로젝트>.pages.dev,https://*.<프로젝트>.pages.dev
   ```
4. 배포 후 `https://<서비스>.onrender.com/swagger-ui.html` 확인

무료 인스턴스는 15분간 요청이 없으면 잠들고, 깨어나는 데 1~2분 걸립니다.
데모 전에는 [cron-job.org](https://cron-job.org) 등으로 `/actuator/health`를 10분마다 호출해 깨워 두세요.

## 환경 변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PGHOST` / `PGPORT` / `PGDATABASE` / `PGUSER` / `PGPASSWORD` | localhost / 5432 / hackathon / hackathon / hackathon | DB 접속 정보 |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | (없음) | 지정하면 위 PG* 대신 사용 (배포 시 Neon) |
| `PORT` | 8080 | Render가 자동 주입 |
| `JPA_DDL_AUTO` | update | 스키마 자동 생성 전략 |
| `SEOUL_API_KEY` | (없음) | 서울 열린데이터광장 인증키 |
| `DATA_GO_KR_API_KEY` | (없음) | 공공데이터포털 인증키 (Decoding) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:*` | 프론트 주소, 쉼표 구분·와일드카드 가능 (예: `https://*.pages.dev`) |

에러 응답은 [ProblemDetail](https://www.rfc-editor.org/rfc/rfc9457) 형식(`application/problem+json`)으로 통일돼 있어요.
