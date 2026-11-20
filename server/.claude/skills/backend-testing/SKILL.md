---
name: backend-testing
description: Use when writing or reviewing tests for the Spring Boot server (`server/`), choosing which test layer fits a change, or setting up test tags, ArchUnit, or CI for the server. Covers layer-by-layer templates (domain unit, Mockito service, MockMvc controller, external API client with MockRestServiceServer, JPA with Testcontainers Postgres, ArchUnit) and how to run and report results. Also use for "백엔드 테스트", "테스트 코드 작성", "Repository 테스트", "외부 API 테스트".
---

# Backend testing

`server/` (Java 25, Spring Boot 4.1, PostgreSQL) 테스트를 계층별로 고르고 작성하는 방법입니다.
기본 `./gradlew test`가 외부 API 키 없이 빠르게 돌게 하고, 실제 DB 검증은 Testcontainers로 합니다.

## 핵심 원칙

- 기본은 mock·fake 데이터와 실제 Domain 객체입니다. 실제 공공데이터 API나 공용 DB에 연결하지 않습니다.
- 대체하는 것은 경계(Repository, 외부 API client, 시각·파일·네트워크)뿐입니다. 값을 준비하려고
  Entity나 record를 Mockito mock으로 만들지 않습니다.
- 출력·상태·오류 계약을 검증합니다. mock이 반환하도록 정한 값을 그대로 확인하는 테스트나
  구현의 내부 호출을 복제하는 테스트는 만들지 않습니다. `verify`는 "잘못된 입력이면 외부 API를
  부르지 않는다"처럼 업무 의미가 있을 때만 씁니다.
- Fixture는 의미가 드러나는 고정값으로 만듭니다. `LocalDate.now()`나 무작위 값처럼 실행마다
  결과가 달라지는 입력은 쓰지 않고, 필요하면 `Clock`을 주입합니다.
- 같은 동작을 여러 계층에서 중복으로 검증하지 않습니다. 버그를 고치면 그 버그를 재현하는 테스트를
  하나 남깁니다.
- 외부 API의 `null`이나 빈 응답을 테스트 기대값에서 `0`이나 빈 문자열로 바꾸지 않습니다.

## 계층 선택

먼저 검증하려는 대상을 정하고, 그 대상을 검증할 수 있는 가장 아래 계층을 고릅니다.

| 검증 대상 | 계층 | 실제로 실행되는 범위 | 대체하는 범위 | Docker |
| --- | --- | --- | --- | --- |
| 계산·불변식·상태 변경 | Domain 단위 | 도메인 객체 | 없음 | 불필요 |
| 입력 검증·분기·집계·예외 | Service 단위 | Service | Repository, 외부 client | 불필요 |
| 요청 binding·validation·HTTP status·응답 JSON | Controller 슬라이스 | Controller, Spring MVC | Service나 client | 불필요 |
| 외부 API 요청 URL·인코딩·응답 파싱·오류 변환 | Client 단위 | client, `RestClient` | HTTP 서버 (`MockRestServiceServer`) | 불필요 |
| JPA mapping·쿼리·constraint·transaction | JPA 슬라이스 | Repository, JPA, 실제 Postgres | 없음 | 필요 |
| 애플리케이션이 뜨는지 | Context 로드 | 전체 context | 없음 | 필요 |
| 레이어·의존 규약 | ArchUnit (선택) | 클래스 의존 관계 (코드를 실행하지 않음) | 해당 없음 | 불필요 |

- Repository를 mock한 Service 테스트는 SQL, constraint, cascade, dirty checking, rollback의 근거가
  되지 않습니다. 이것들이 검증 대상이면 JPA 슬라이스로 올립니다.
- Controller 테스트에서 persistence 동작을 다시 흉내 내지 않습니다.
- 실제 공공데이터 API를 부르는 테스트는 기본 `test`에서 빼고 `@Tag("external")`로 따로 실행합니다
  (아래 "Tag 분리").

## 계층별 템플릿

import 문은 생략했습니다. Spring Boot 4는 테스트 auto-configuration이 모듈별로 나뉘어 있어서
`@WebMvcTest`, `@DataJpaTest`의 package가 Boot 3와 다릅니다. IDE 자동 import를 쓰고, 해당
`spring-boot-starter-*-test` 의존성이 `build.gradle`에 있는지 확인합니다.

### Domain 단위

```java
class PriceChangeTest {
    @Test
    void weekOverWeekRateIsNullWhenPreviousPriceIsMissing() {
        var change = new PriceChange(new BigDecimal("1200"), null);   // 실제 객체로 만듭니다
        assertThat(change.rateOrNull()).isNull();
    }
}
```

### Service 단위 (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {
    @Mock AgroPriceClient agroPriceClient;
    @InjectMocks BriefingService service;

    @Test
    void summarizesTopMovers() { /* 정상, 빈 결과, null 필드, 외부 오류를 각각 확인 */ }

    @Test
    void rejectsFutureDateBeforeCallingApi() {
        assertThrows(ErrorResponseException.class, () -> service.briefing("29991231"));
        verifyNoInteractions(agroPriceClient);
    }
}
```

### Controller 슬라이스 (standalone MockMvc)

Spring context 없이 Controller 하나만 올립니다. 가장 빠르고 Docker도 필요 없습니다.

```java
class AgroPriceControllerTest {
    private final AgroPriceClient client = mock(AgroPriceClient.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AgroPriceController(client)).build();

    @Test
    void passesQueryToClient() throws Exception {
        when(client.priceTrends(eq("20261106"), anyMap(), eq(1), eq(100))).thenReturn(sampleResponse());
        mvc.perform(get("/api/agro/price-trends").param("date", "20261106"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].item_nm").value("배추"));
    }
}
```

`@Pattern`, `@Min` 같은 메서드 파라미터 검증은 standalone 설정에서 동작하지 않을 수 있습니다.
검증 오류의 HTTP status까지 확인하려면 `@WebMvcTest(AgroPriceController.class)`와
`@MockitoBean AgroPriceClient client`를 씁니다.

```java
@WebMvcTest(AgroPriceController.class)
class AgroPriceControllerValidationTest {
    @Autowired MockMvc mvc;
    @MockitoBean AgroPriceClient client;

    @Test
    void invalidDateIsRejectedBeforeCallingApi() throws Exception {
        mvc.perform(get("/api/agro/price-trends").param("date", "2026-11-06"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }
}
```

### 외부 API client (`MockRestServiceServer`)

실제 공공데이터 API 대신 가짜 HTTP 서버를 `RestClient`에 연결합니다. 키 인코딩, query 조건,
응답 파싱, 4xx·5xx 오류 변환을 키 없이 검증합니다.

```java
class AgroPriceClientTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AgroPriceClient client = new AgroPriceClient(builder.build(), JsonMapper.builder().build(),
            "https://api.example.test", "key+with/special=");

    @Test
    void encodesKeyAndParsesItems() {
        server.expect(requestTo(containsString("serviceKey=key%2Bwith%2Fspecial%3D")))
                .andRespond(withSuccess(SAMPLE_JSON, MediaType.APPLICATION_JSON));
        var response = client.priceTrends("20261106", Map.of(), 1, 100);
        assertThat(response.items()).hasSize(1);
        server.verify();
    }

    @Test
    void missingKeyIsServiceUnavailable() {
        var noKey = new AgroPriceClient(builder.build(), JsonMapper.builder().build(), "https://api.example.test", "");
        var error = assertThrows(ErrorResponseException.class, () -> noKey.priceTrends("20261106", Map.of(), 1, 100));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
```

응답 fixture(`SAMPLE_JSON`)는 실제 응답을 한 번 받아서 개인정보나 키를 지운 뒤
`src/test/resources`에 둡니다.

### JPA 슬라이스 (Testcontainers Postgres)

`TestcontainersConfiguration`이 `postgres:17-alpine` 컨테이너를 띄우고 `@ServiceConnection`으로
datasource를 연결합니다. H2가 아니라 실제 Postgres라서 방언, constraint, 쿼리를 그대로 검증합니다.

```java
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // 내장 DB로 바꾸지 않습니다
class StoreRepositoryTest {
    @Autowired StoreRepository repository;
    @Autowired TestEntityManager em;

    @Test
    void saveAndReload() {
        var saved = repository.save(new Store("사모이로 1호점"));
        em.flush();   // SQL과 constraint를 실제로 실행
        em.clear();   // 1차 cache를 비우고 DB에서 다시 읽습니다
        assertThat(repository.findById(saved.getId())).get().extracting(Store::getName).isEqualTo("사모이로 1호점");
    }
}
```

- `@DataJpaTest`는 테스트마다 transaction을 열고 끝나면 rollback합니다. `@Commit`이나
  `@Rollback(false)`는 쓰지 않습니다.
- DB 반영 여부는 `flush()` → `clear()` → 다시 조회로 확인합니다. 메모리 객체의 값만 확인하면
  DB에 반영됐는지 검증한 것이 아닙니다.
- `TestcontainersConfiguration`은 `com.hackathon.server` package의 package-private class입니다.
  하위 package의 테스트에서 `@Import`하려면 class와 `postgresContainer()`를 `public`으로 바꿉니다.
- `REQUIRES_NEW`, `@Async`, 별도 thread에서 실행되는 코드는 테스트 transaction의 rollback 범위
  밖입니다. 테스트가 만든 row만 직접 정리합니다.

### ArchUnit 규약 (선택)

레이어 규칙을 코드로 강제하고 싶을 때 추가합니다. `testImplementation
'com.tngtech.archunit:archunit-junit5:<최신 버전>'`이 필요합니다.

```java
@AnalyzeClasses(packages = "com.hackathon.server", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureConventionTest {
    @ArchTest
    static final ArchRule controllersDoNotUseRepositories =
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .because("Controller는 Service만 호출한다");
}
```

새 프로젝트라 기존 위반이 없으므로 `FreezingArchRule` 없이 바로 실패시킵니다.

## Tag 분리

실제 외부 API를 부르는 테스트가 생기면 기본 `test`에서 빼서 키 없이도 테스트가 통과하게 합니다.

```groovy
tasks.named('test') {
    useJUnitPlatform { excludeTags 'external' }
}
tasks.register('externalTest', Test) {
    description = 'Runs tests tagged with @Tag("external") against real public APIs'
    group = 'verification'
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform { includeTags 'external' }
}
```

`externalTest`는 `server/.env`에 키가 있을 때만 개발자가 직접 실행합니다. 호출 한도가 있는
API는 테스트 횟수에 주의합니다.

## CI를 추가할 때

지금은 CI workflow가 없습니다. GitHub Actions를 추가한다면 `ubuntu-latest` runner에 Docker가
있으므로 Testcontainers 테스트도 그대로 돌아갑니다.

```yaml
- uses: actions/setup-java@v4
  with: { distribution: temurin, java-version: '25' }
- run: ./gradlew test
  working-directory: server
```

실패 시 `server/build/reports/tests/test`를 artifact로 올리면 원인을 보기 쉽습니다.

## 실행과 보고

```bash
cd server && ./gradlew test
cd server && ./gradlew test --tests '*AgroPriceClientTest'
```

- `ServerApplicationTests.contextLoads`와 JPA 슬라이스는 Docker가 필요합니다. colima를 쓰면
  [`server/README.md`](../../../README.md)의 `DOCKER_HOST`, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`를 설정합니다.
- Docker 없이 빠르게 확인하려면 Docker가 필요 없는 테스트만 `--tests`로 지정해 실행합니다.
- 결과는 Gradle 종료 코드와 테스트 성공·실패 수를 함께 보고합니다. 보고서는
  `server/build/reports/tests/test/index.html`에 생성됩니다. 실행하지 못한 테스트가 있으면 이유를 적습니다.
- 인증키나 실제 응답의 개인정보를 테스트 코드, fixture, 로그에 남기지 않습니다.
