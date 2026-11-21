# 예외 작성 가이드

서버의 API 에러는 모두 [`GlobalExceptionHandler`](../src/main/java/com/hackathon/server/common/exception/GlobalExceptionHandler.java)가 받아서
같은 형식으로 응답하고 `error_log` 테이블에 남깁니다. 서비스 코드는 **`BusinessException`을 던지기만** 하면 됩니다.

## 응답 형식

RFC 9457 ProblemDetail에 `code`를 더한 JSON입니다. 프론트는 `code`로 분기하고 `detail`을 사용자에게 보여줍니다.

```json
{
  "title": "Service Unavailable",
  "status": 503,
  "detail": "KWEATHER_API_KEY가 설정되지 않았습니다.",
  "instance": "/api/kweather/warnings",
  "code": "PUBLIC_API_KEY_MISSING"
}
```

## 예외 던지기

```java
throw new BusinessException(ErrorCode.NOT_FOUND, "이 좌표에 해당하는 행정동이 없습니다.");
throw new BusinessException(ErrorCode.CONFLICT);   // detail을 생략하면 ErrorCode의 기본 메시지
```

- HTTP status는 `ErrorCode`가 정합니다. status를 따로 넘기지 않습니다.
- `detail`에는 무엇이 왜 실패했는지 한 문장으로 씁니다. 사용자에게 그대로 보입니다.
- Controller에서 try/catch로 감싸지 않습니다. 던지면 핸들러가 응답과 로그를 처리합니다.
- `@Valid` 검증 실패, 없는 경로, 잘못된 메서드처럼 Spring이 던지는 예외는 손대지 않아도 status에 맞는 공통 코드(`BAD_REQUEST`, `NOT_FOUND` 등)가 붙습니다.
- 예상하지 못한 예외는 `INTERNAL_ERROR`(500)로 응답합니다. 이때 응답에는 원래 메시지를 숨기고, `error_log`에 stack trace를 남깁니다.

## 에러 코드 고르기

| 코드 | status | 쓰는 경우 |
| --- | --- | --- |
| `BAD_REQUEST` | 400 | 요청 값이 규칙에 맞지 않음 |
| `NOT_FOUND` | 404 | 요청한 대상이 없음 |
| `METHOD_NOT_ALLOWED` | 405 | (Spring이 자동으로 붙임) |
| `CONFLICT` | 409 | 이미 있는 데이터, 현재 상태에서 할 수 없는 요청 |
| `TOO_MANY_REQUESTS` | 429 | 호출 한도 초과 |
| `INTERNAL_ERROR` | 500 | 서버 버그 (직접 던지지 않음) |
| `EXTERNAL_API_ERROR` | 502 | 공공데이터가 아닌 외부 API(ai-server 등) 실패 |
| `SERVICE_UNAVAILABLE` | 503 | 서버 설정이 빠져서 기능을 쓸 수 없음 |
| `GATEWAY_TIMEOUT` | 504 | 외부 API 응답 시간 초과 |
| `PUBLIC_API_KEY_MISSING` | 503 | 공공데이터 API 인증키 환경변수가 비어 있음 |
| `PUBLIC_API_CONNECTION_FAILED` | 502 | 연결 실패·타임아웃 (`RestClientException`) |
| `PUBLIC_API_BAD_REQUEST` | 400 | 공공데이터 API가 우리 요청 값을 거부함 (HTTP 400, 서울시 `ERROR-3xx`) |
| `PUBLIC_API_ERROR` | 502 | 공공데이터 API가 오류를 응답함 (HTTP 4xx·5xx, 본문의 오류 코드) |
| `PUBLIC_API_INVALID_RESPONSE` | 502 | 응답이 비었거나 형식을 알 수 없음 |

맞는 코드가 없으면 아래 순서로 새로 만듭니다.

## 새 에러 코드 추가

1. [`ErrorCode`](../src/main/java/com/hackathon/server/common/exception/ErrorCode.java)에 상수를 추가합니다. 이름은 `{도메인}_{상황}` 대문자입니다 (예: `SUGGESTION_FEEDBACK_DUPLICATED`).
2. 새 Flyway 마이그레이션에서 `error_code`에 같은 행을 넣습니다. 이미 배포된 마이그레이션 파일은 고치지 않습니다.

   ```sql
   insert into error_code (code, http_status, message) values
       ('SUGGESTION_FEEDBACK_DUPLICATED', 409, '이미 피드백을 남긴 카드입니다.');
   ```

3. `./gradlew test`를 돌립니다. `ErrorLogMigrationTest`가 enum과 테이블이 다르면 실패합니다.
   행을 빠뜨린 채 배포하면 `error_log` 저장이 외래키 오류로 실패합니다. 응답은 정상적으로 나가지만 로그가 남지 않습니다.

## 외부 API를 호출할 때

공공데이터 client들은 실패 원인을 아래처럼 나눕니다. 새 client도 같은 순서를 따릅니다.

```java
if (!StringUtils.hasText(apiKey)) {
    throw new BusinessException(ErrorCode.PUBLIC_API_KEY_MISSING, "XXX_API_KEY가 설정되지 않았습니다.");
}
try {
    body = restClient.get().uri(...).retrieve().body(String.class);
}
catch (RestClientResponseException e) {          // 상대가 오류 status로 응답
    ErrorCode code = e.getStatusCode().value() == 400 ? ErrorCode.PUBLIC_API_BAD_REQUEST : ErrorCode.PUBLIC_API_ERROR;
    throw new BusinessException(code, "XXX API 오류 (HTTP " + e.getStatusCode().value() + "): " + 응답의_오류_메시지);
}
catch (RestClientException e) {                  // 연결 실패·타임아웃
    throw new BusinessException(ErrorCode.PUBLIC_API_CONNECTION_FAILED, "XXX API 호출 실패: " + e.getMostSpecificCause().getMessage());
}
if (body == null || body.isBlank()) {
    throw new BusinessException(ErrorCode.PUBLIC_API_INVALID_RESPONSE, "XXX API 응답이 비어 있습니다.");
}
```

`RestClientResponseException`이 `RestClientException`의 하위 클래스라서 catch 순서가 중요합니다.

## 하지 말 것

- **인증키를 detail에 넣지 않습니다.** `e.getMessage()`에는 인증키가 든 요청 URL이 섞여 있습니다. `e.getMostSpecificCause().getMessage()`만 씁니다.
- **원래 예외를 cause로 넘기지 않습니다.** `BusinessException`이 cause를 받지 않는 것도 같은 이유입니다. 5xx는 stack trace가 `error_log`에 저장됩니다.
- 개인정보(전화번호, 주소 원문 등)를 detail에 넣지 않습니다.
- `ResponseStatusException`이나 `ErrorResponseException`을 직접 만들지 않습니다. 이렇게 던지면 status로 공통 코드만 붙습니다.
- 예외를 catch해서 로그만 찍고 삼키지 않습니다. 처리할 수 없으면 그대로 던집니다.

## 테스트

- Client·Service 단위 테스트에서는 코드로 검증합니다. status는 코드에 따라오므로 따로 확인하지 않습니다.

  ```java
  var error = assertThrows(BusinessException.class, () -> client.list(PATH, Map.of()));
  assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PUBLIC_API_CONNECTION_FAILED);
  ```

- 응답 JSON의 `code`는 [`GlobalExceptionHandlerTest`](../src/test/java/com/hackathon/server/common/exception/GlobalExceptionHandlerTest.java)가 확인하므로 Controller 테스트에서 다시 검증하지 않아도 됩니다.

## 에러 로그 보기

```sql
-- 최근 에러
select created_datetime, error_code, http_status, http_method, request_path, message
from error_log order by id desc limit 50;

-- 하루 동안 코드별 건수
select error_code, count(*) from error_log
where created_datetime > now() - interval '1 day' group by 1 order by 2 desc;
```

- 정적 리소스 404(봇이 없는 경로를 찌르는 요청)는 남기지 않습니다.
- `stack_trace`는 5xx일 때만 저장합니다.
- 배치 실패는 `error_log`가 아니라 `batch_job_history.error_code`에 같은 코드로 남습니다.
