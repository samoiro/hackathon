package com.hackathon.server.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.analysis.dto.AnalysisFailure;
import com.hackathon.server.analysis.dto.AnalysisInput;
import com.hackathon.server.analysis.dto.AnalysisRequest;
import com.hackathon.server.analysis.dto.AnalysisResponse;
import com.hackathon.server.analysis.dto.AnalysisStatus;
import com.hackathon.server.analysis.dto.AnalysisStep;
import com.hackathon.server.analysis.dto.EventSearch;
import com.hackathon.server.analysis.dto.EventSource;
import com.hackathon.server.analysis.dto.FeedbackVote;
import com.hackathon.server.analysis.dto.OperationType;
import com.hackathon.server.analysis.dto.StepCode;
import com.hackathon.server.analysis.dto.StepStatus;
import com.hackathon.server.analysis.dto.StoreSummary;
import com.hackathon.server.analysis.dto.SuggestionFeedbackRequest;
import com.hackathon.server.analysis.dto.SuggestionResponse;
import com.hackathon.server.analysis.entity.AnalysisJob;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.EventEvidenceCollector;
import com.hackathon.server.analysis.evidence.IngredientEvidenceResult;
import com.hackathon.server.analysis.repository.AnalysisCatalogRepository;
import com.hackathon.server.analysis.repository.AnalysisJobRepository;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.industry.dto.IndustryResponse;
import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.publicdata.client.KakaoLocalClient;
import com.hackathon.server.publicdata.dto.AdminDong;
import com.hackathon.server.supply.dto.SupplyResponse;

/** 분석 4단계 흐름. 수집기는 가짜, Python은 MockRestServiceServer, 저장소는 메모리 Map으로 대신한다. */
class AnalysisServiceTest {

	private static final IndustryResponse KOREAN = new IndustryResponse(9, "음식점", "한식·국밥");
	private static final StoreSummary STORE = new StoreSummary("성수동 한식당", "서울 성동구 서울숲2길 32", "성동구", "성수동 · 서울숲공원 인근", KOREAN,
			OperationType.BOTH, List.of(new IngredientResponse(12, "배추"), new IngredientResponse(15, "돼지고기"),
					new IngredientResponse(7, "쌀")),
			List.of(new SupplyResponse(3, "포장 용기"), new SupplyResponse(5, "일회용 수저")));
	private static final AnalysisRequest REQUEST = new AnalysisRequest("성수동 한식당", "서울 성동구 서울숲2길 32", 37.5446, 127.0446, 9L,
			OperationType.BOTH, List.of(12L, 15L, 7L), List.of(3L, 5L), List.of());
	private static final AdminDong SEONGSU = new AdminDong("1120065000", "서울특별시 성동구 성수1가1동", "서울특별시", "성동구", "성수1가1동");

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final Map<UUID, AnalysisJob> jobs = new HashMap<>();
	private final AnalysisJobRepository jobRepository = mock(AnalysisJobRepository.class);
	private final AnalysisCatalogRepository catalog = mock(AnalysisCatalogRepository.class);
	private final KakaoLocalClient kakao = mock(KakaoLocalClient.class);
	private final List<Runnable> queued = new ArrayList<>();
	private final RestClient.Builder aiBuilder = RestClient.builder().baseUrl("http://ai.test").defaultHeader("X-Internal-Token", "t");
	private final MockRestServiceServer ai = MockRestServiceServer.bindTo(aiBuilder).build();

	private EventEvidenceCollector eventCollector = store -> ContractExample.events();
	private IngredientEvidenceResult ingredients = ContractExample.ingredients();

	AnalysisServiceTest() {
		when(jobRepository.save(any())).thenAnswer(inv -> {
			AnalysisJob job = inv.getArgument(0);
			jobs.put(job.getId(), job);
			return job;
		});
		when(jobRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(jobs.get(inv.getArgument(0))));
	}

	private AnalysisService service() {
		return new AnalysisService(jobRepository, catalog, kakao, store -> eventCollector.collect(store), store -> ingredients,
				(store, events) -> events.events().isEmpty() ? List.of() : ContractExample.supplies(), new AiServerClient(aiBuilder.build()),
				jsonMapper, queued::add);
	}

	/** 기준 시각을 고정하려고 POST를 거치지 않고 PENDING 작업을 바로 넣는다. DB에서 읽은 것처럼 기준 시각은 UTC로 둔다. */
	private UUID pending() {
		UUID id = ContractExample.ANALYSIS_ID;
		jobs.put(id, new AnalysisJob(id, ContractExample.BASE_TIME.withOffsetSameInstant(java.time.ZoneOffset.UTC), jsonMapper.writeValueAsString(new AnalysisInput(REQUEST, STORE,
				SEONGSU.code())), "[]"));
		return id;
	}

	private static String aiResponse(String... cardIds) {
		return Stream.of(cardIds)
				.map(id -> """
						{"card_id": "%s", "title": "%s 제목", "expected_effect": null, "sub_tasks": ["할 일 하나", "할 일 둘"]}""".formatted(id, id))
				.collect(Collectors.joining(",", "{\"suggestions\": [", "], \"model\": \"test\", \"generated_at\": \"2026-11-17T07:00:12+09:00\"}"));
	}

	@Test
	void contractExampleSucceedsWithRecommendedC6C7C1() {
		ai.expect(requestTo("http://ai.test/v1/analyze"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("X-Internal-Token", "t"))
				.andExpect(jsonPath("$.request_id").value(ContractExample.ANALYSIS_ID.toString()))
				.andExpect(jsonPath("$.base_time").value("2026-11-17T07:00:00+09:00"))
				.andExpect(jsonPath("$.store.area_label").value("성수동 · 서울숲공원 인근"))
				.andExpect(jsonPath("$.event_search.radius_m").value(1000))
				.andExpect(jsonPath("$.events[0].date_label").value("11.21(토)"))
				.andExpect(jsonPath("$.events[0].peak_label").value("15~20시"))
				.andExpect(jsonPath("$.raw_materials[0].change_4w_pct").value(10.9))
				.andExpect(jsonPath("$.raw_materials[0].price_stage").value("ALERT_HIGH"))
				.andExpect(jsonPath("$.supplies[0].prepare_by_label").value("11.18(수)"))
				.andExpect(jsonPath("$.cards.length()").value(7))
				.andExpect(jsonPath("$.cards[5].evidence_ids[1]").value("E1"))
				// 요청에 없는 C9는 버린다
				.andRespond(withSuccess(aiResponse("C7", "C6", "C5", "C4", "C3", "C2", "C1", "C9"), MediaType.APPLICATION_JSON));
		UUID id = pending();

		service().run(id);

		ai.verify();
		AnalysisResponse response = service().get(id);
		assertThat(response.status()).isEqualTo(AnalysisStatus.SUCCEEDED);
		assertThat(response.baseTime()).isEqualTo(ContractExample.BASE_TIME); // +09:00으로 돌려준다
		assertThat(response.steps()).extracting(AnalysisStep::status).containsOnly(StepStatus.DONE);
		assertThat(response.eventSearch().source()).isEqualTo(EventSource.CULTURAL_EVENT);
		List<SuggestionResponse> suggestions = response.briefing().suggestions();
		assertThat(suggestions).extracting(SuggestionResponse::title).containsExactly("C1 제목", "C2 제목", "C3 제목", "C4 제목", "C5 제목",
				"C6 제목", "C7 제목");
		assertThat(suggestions).filteredOn(SuggestionResponse::recommended).extracting(SuggestionResponse::cardKey)
				.containsExactly("EVENT:STAFFING:-", "SUPPLY:RESTOCK:3", "SUPPLY:RESTOCK:5");
		assertThat(response.briefing().ingredients()).hasSize(3);
		assertThat(jobs.get(id).getScoreVersion()).isEqualTo(BriefingPlanner.SCORE_VERSION);
		assertThat(jobs.get(id).getEvidenceSnapshot()).contains("\"evidenceId\":\"S1\"", "\"total\":8");
	}

	@Test
	void cardsMissingFromAiResponseAreDropped() {
		ai.expect(requestTo("http://ai.test/v1/analyze")).andRespond(withSuccess(aiResponse("C4"), MediaType.APPLICATION_JSON));
		UUID id = pending();

		service().run(id);

		List<SuggestionResponse> suggestions = service().get(id).briefing().suggestions();
		assertThat(suggestions).extracting(SuggestionResponse::cardKey, SuggestionResponse::recommended)
				.containsExactly(tuple("INGREDIENT:PRICE_CHECK:12", true));
	}

	@Test
	void aiServerErrorFailsTaskGeneration() {
		ai.expect(requestTo("http://ai.test/v1/analyze")).andRespond(withStatus(HttpStatus.BAD_GATEWAY)
				.contentType(MediaType.APPLICATION_JSON).body("{\"code\": \"INVALID_LLM_OUTPUT\", \"detail\": \"남은 칸이 없음\"}"));
		UUID id = pending();

		service().run(id);

		AnalysisResponse response = service().get(id);
		assertThat(response.status()).isEqualTo(AnalysisStatus.FAILED);
		assertThat(response.failure()).isEqualTo(new AnalysisFailure(StepCode.TASK_GENERATION, "EXTERNAL_API_ERROR", "브리핑을 만들다가 멈췄어요"));
		assertThat(response.steps()).extracting(AnalysisStep::status)
				.containsExactly(StepStatus.DONE, StepStatus.DONE, StepStatus.DONE, StepStatus.FAILED);
		assertThat(response.briefing()).isNull();
		// 실패해도 왜 그 카드였는지 볼 수 있게 근거 점수는 남긴다
		assertThat(jobs.get(id).getEvidenceSnapshot()).isNotNull();
	}

	@Test
	void noUsableCardFromAiServerFails() {
		ai.expect(requestTo("http://ai.test/v1/analyze")).andRespond(withSuccess(aiResponse("C99"), MediaType.APPLICATION_JSON));
		UUID id = pending();

		service().run(id);

		assertThat(service().get(id).failure().step()).isEqualTo(StepCode.TASK_GENERATION);
	}

	@Test
	void noCardsSucceedsWithoutCallingAiServer() {
		eventCollector = store -> new EventEvidence(new EventSearch(7, store.baseDate(), 0, EventSource.NONE), List.of(), List.of(), null,
				List.of());
		ingredients = new IngredientEvidenceResult(List.of(ContractExample.R3), List.of());
		UUID id = pending();

		service().run(id);

		ai.verify(); // 기대한 요청이 없으므로 Python을 불렀다면 run에서 AssertionError가 났다
		AnalysisResponse response = service().get(id);
		assertThat(response.status()).isEqualTo(AnalysisStatus.SUCCEEDED);
		assertThat(response.briefing().suggestions()).isEmpty();
		assertThat(response.briefing().ingredients()).hasSize(1);
	}

	@Test
	void collectorFailureStopsAtEventSearch() {
		eventCollector = store -> {
			throw new BusinessException(ErrorCode.PUBLIC_API_CONNECTION_FAILED, "서울시 API 호출 실패");
		};
		UUID id = pending();

		service().run(id);

		AnalysisResponse response = service().get(id);
		assertThat(response.failure()).isEqualTo(new AnalysisFailure(StepCode.EVENT_SEARCH, "PUBLIC_API_CONNECTION_FAILED",
				"주변 행사를 불러오지 못했어요"));
		assertThat(response.steps()).extracting(AnalysisStep::status)
				.containsExactly(StepStatus.DONE, StepStatus.FAILED, StepStatus.WAITING, StepStatus.WAITING);
		assertThat(response.eventSearch()).isNull();
	}

	@Test
	void createSavesPendingJobAndQueuesRun() {
		stubCatalog();
		when(kakao.adminDong(37.5446, 127.0446)).thenReturn(SEONGSU);

		var created = service().create(REQUEST);

		assertThat(created.status()).isEqualTo(AnalysisStatus.PENDING);
		assertThat(queued).hasSize(1);
		AnalysisResponse response = service().get(created.analysisId());
		assertThat(response.status()).isEqualTo(AnalysisStatus.PENDING);
		assertThat(response.store()).isEqualTo(STORE);
		assertThat(response.steps()).extracting(AnalysisStep::status).containsOnly(StepStatus.WAITING);
	}

	@Test
	void outsideSeoulIsRejected() {
		stubCatalog();
		when(kakao.adminDong(anyDouble(), anyDouble()))
				.thenReturn(new AdminDong("4113510300", "경기도 성남시 분당구 정자동", "경기도", "성남시 분당구", "정자동"));

		var error = assertThrows(BusinessException.class, () -> service().create(REQUEST));

		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ANALYSIS_OUTSIDE_SEOUL);
		assertThat(jobs).isEmpty();
	}

	@Test
	void duplicateItemIsRejectedBeforeKakao() {
		stubCatalog();
		var duplicated = new AnalysisRequest("성수동 한식당", "서울 성동구 서울숲2길 32", 37.5446, 127.0446, 9L, null, List.of(12L, 12L),
				List.of(), List.of());

		var error = assertThrows(BusinessException.class, () -> service().create(duplicated));

		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
		verifyNoInteractions(kakao);
	}

	@Test
	void unknownSupplyIsRejected() {
		stubCatalog();
		var unknown = new AnalysisRequest("성수동 한식당", "서울 성동구 서울숲2길 32", 37.5446, 127.0446, 9L, null, List.of(), List.of(3L, 999L),
				List.of());

		var error = assertThrows(BusinessException.class, () -> service().create(unknown));

		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
		verifyNoInteractions(kakao);
	}

	@Test
	void feedbackCopiesIndustryAndCardColumns() {
		ai.expect(requestTo("http://ai.test/v1/analyze")).andRespond(withSuccess(aiResponse("C4"), MediaType.APPLICATION_JSON));
		UUID id = pending();
		service().run(id);

		service().feedback(id, new SuggestionFeedbackRequest("INGREDIENT:PRICE_CHECK:12", FeedbackVote.NOT_RELEVANT));

		verify(catalog).upsertFeedback(id, 9L, "INGREDIENT:PRICE_CHECK:12", "INGREDIENT", "PRICE_CHECK", 12L, "NOT_RELEVANT");
	}

	@Test
	void feedbackForCardNotInAnalysisIsRejected() {
		ai.expect(requestTo("http://ai.test/v1/analyze")).andRespond(withSuccess(aiResponse("C4"), MediaType.APPLICATION_JSON));
		UUID id = pending();
		service().run(id);

		var error = assertThrows(BusinessException.class,
				() -> service().feedback(id, new SuggestionFeedbackRequest("EVENT:HOURS:-", FeedbackVote.HELPFUL)));

		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
	}

	@Test
	void unknownAnalysisIsNotFound() {
		var error = assertThrows(BusinessException.class, () -> service().get(UUID.fromString("00000000-0000-0000-0000-000000000000")));

		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	void shortDongDropsNumbers() {
		assertThat(AnalysisService.shortDong("성수1가1동")).isEqualTo("성수동");
		assertThat(AnalysisService.shortDong("상계3·4동")).isEqualTo("상계동");
		assertThat(AnalysisService.shortDong("청운효자동")).isEqualTo("청운효자동");
	}

	private void stubCatalog() {
		when(catalog.industry(9L)).thenReturn(Optional.of(KOREAN));
		when(catalog.nearestAreaName(37.5446, 127.0446)).thenReturn(Optional.of("서울숲공원"));
		when(catalog.ingredients(anyList())).thenReturn(List.of(new IngredientResponse(7, "쌀"), new IngredientResponse(12, "배추"),
				new IngredientResponse(15, "돼지고기")));
		when(catalog.supplies(anyList())).thenReturn(List.of(new SupplyResponse(5, "일회용 수저"), new SupplyResponse(3, "포장 용기")));
	}
}
