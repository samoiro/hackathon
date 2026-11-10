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
