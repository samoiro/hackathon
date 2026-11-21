# 사모이로 AI 서버 (FastAPI)

Java 서버(`server/`)가 정한 추천 카드 칸마다 OpenAI로 제목·기대 효과·세부 할 일 문장을 써서 돌려주는 서버입니다.
고르고 순위를 매기는 일은 Java가 하고, 여기서는 문장만 쓰고 검증합니다.

- 계약(요청·응답·오류): [`docs/API_CONTRACT_DRAFT.md`](docs/API_CONTRACT_DRAFT.md)
- 프롬프트와 검사 규칙: [`docs/PROMPT_DRAFT.md`](docs/PROMPT_DRAFT.md)
- 카드 칸을 만드는 규칙(Java): [`docs/SUGGESTION_TODOS_DRAFT.md`](docs/SUGGESTION_TODOS_DRAFT.md)

## API

| 메서드 · 경로 | 설명 |
| --- | --- |
| `GET /health` | 상태 확인 |
| `POST /v1/analyze` | 헤더 `X-Internal-Token` 필요. 카드 칸(`cards`)마다 문장을 써서 돌려줌 |

`/v1/analyze`가 하는 일:

1. 요청 JSON에서 `request_id`를 뺀 나머지를 시스템 프롬프트와 함께 OpenAI에 보냅니다(구조화 출력, strict JSON Schema).
2. 답을 검사합니다: `card_id` 빠짐·중복, 제목 24자·세부 할 일 2~3개(각 24자), 제목·세부 할 일의 **입력에 없는 숫자**, 기대 효과의 숫자·금지어(매출·수익·방문객·손님이 늘)·끝맺음("수 있어요.").
3. 어기면 위반 내용을 붙여 **1번만** 다시 요청합니다. 그래도 기대 효과만 어긴 칸은 기대 효과를 `null`로, 다른 규칙을 어긴 칸은 뺍니다.
4. 재요청을 포함해 `LLM_TIMEOUT_SECONDS`(기본 40초) 안에서 끝냅니다. 재요청이 시간 초과·오류면 첫 답에서 남길 수 있는 칸으로 응답합니다.

| 오류 | `code` | 언제 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 토큰이 없거나 틀림 (서버 토큰이 비어 있어도 막음) |
| 422 | `INVALID_REQUEST` | 요청 형식 오류. 입력값은 응답에 넣지 않음 |
| 502 | `INVALID_LLM_OUTPUT` | 재요청 후에도 남은 칸이 0개 |
| 503 | `LLM_UNAVAILABLE` | `LLM_API_KEY`·`LLM_MODEL` 미설정, 공급자 오류 |
| 504 | `LLM_TIMEOUT` | 첫 호출이 시간 초과 |

로그에는 `request_id`, 칸 수, 시도 횟수, 걸린 시간만 남깁니다. API 키와 요청 원문은 남기지 않습니다.

## 실행

Python 3.11 이상이 필요합니다.

```sh
cd ai-server
python3 -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
cp .env.example .env   # LLM_API_KEY, LLM_MODEL, AI_SERVER_TOKEN 채우기
uvicorn app.main:app --reload --port 8000 --env-file .env
```

확인: http://localhost:8000/health · http://localhost:8000/docs

테스트(LLM은 가짜로 바꿔서 키 없이 돕니다):

```sh
pytest -q
```

로컬 Python이 3.11보다 낮으면 Docker로 돌립니다(저장소 루트의 `ai-server`에서):

```sh
docker run --rm -v "$PWD":/src -w /src python:3.12-slim sh -c "pip install -q -e '.[dev]' && pytest -q"
```

배포 이미지: `docker build -t hackathon-ai ./ai-server` (저장소 루트에서). Render 설정은 루트 `render.yaml`에 있습니다.

## 손으로 확인하기

```sh
curl -s -X POST localhost:8000/v1/analyze \
  -H "Content-Type: application/json" -H "X-Internal-Token: $AI_SERVER_TOKEN" \
  --data @tests/fixtures/analyze_request.json
```

`tests/fixtures/analyze_request.json`은 계약의 요청 예시(성수동 한식당, 칸 7개)입니다.
