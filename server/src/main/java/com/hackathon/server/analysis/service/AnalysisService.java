package com.hackathon.server.analysis.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import lombok.extern.slf4j.Slf4j;

import com.hackathon.server.analysis.dto.AnalysisCreatedResponse;
import com.hackathon.server.analysis.dto.AnalysisFailure;
import com.hackathon.server.analysis.dto.AnalysisInput;
import com.hackathon.server.analysis.dto.AnalysisRequest;
import com.hackathon.server.analysis.dto.AnalysisResponse;
import com.hackathon.server.analysis.dto.AnalysisStatus;
import com.hackathon.server.analysis.dto.AnalysisStep;
import com.hackathon.server.analysis.dto.BriefingResponse;
import com.hackathon.server.analysis.dto.EventSearch;
import com.hackathon.server.analysis.dto.StepCode;
import com.hackathon.server.analysis.dto.StepStatus;
import com.hackathon.server.analysis.dto.StoreSummary;
import com.hackathon.server.analysis.dto.SuggestionFeedbackRequest;
import com.hackathon.server.analysis.dto.SuggestionResponse;
import com.hackathon.server.analysis.entity.AnalysisJob;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.EventEvidenceCollector;
import com.hackathon.server.analysis.evidence.IngredientEvidenceCollector;
import com.hackathon.server.analysis.evidence.IngredientEvidenceResult;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.analysis.evidence.SupplyEvidence;
import com.hackathon.server.analysis.evidence.SupplyEvidenceCollector;
import com.hackathon.server.analysis.repository.AnalysisCatalogRepository;
import com.hackathon.server.analysis.repository.AnalysisJobRepository;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.industry.dto.IndustryResponse;
import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.publicdata.client.KakaoLocalClient;
import com.hackathon.server.publicdata.dto.AdminDong;
import com.hackathon.server.supply.dto.SupplyResponse;

/**
 * 분석 시작·상태·피드백과 4단계 실행. POST는 입력을 확인해 PENDING으로 저장하고, 실행은 applicationTaskExecutor에서 한다.
 * 기준: ai-server/docs/API_CONTRACT_DRAFT.md "역할 나누기"
 */
@Slf4j
@Service
public class AnalysisService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String SEOUL = "서울특별시";
	private static final Map<StepCode, String> FAILURE_MESSAGES = Map.of(
			StepCode.STORE_CHECK, "가게 정보를 확인하지 못했어요",
			StepCode.EVENT_SEARCH, "주변 행사를 불러오지 못했어요",
			StepCode.PRICE_LOOKUP, "가격 자료를 불러오지 못했어요",
			StepCode.TASK_GENERATION, "브리핑을 만들다가 멈췄어요");

	private final AnalysisJobRepository jobRepository;
	private final AnalysisCatalogRepository catalog;
	private final KakaoLocalClient kakao;
	private final EventEvidenceCollector eventCollector;
	private final IngredientEvidenceCollector ingredientCollector;
	private final SupplyEvidenceCollector supplyCollector;
	private final AiServerClient aiServer;
	private final JsonMapper jsonMapper;
	private final Executor executor;

	public AnalysisService(AnalysisJobRepository jobRepository, AnalysisCatalogRepository catalog, KakaoLocalClient kakao,
			EventEvidenceCollector eventCollector, IngredientEvidenceCollector ingredientCollector,
			SupplyEvidenceCollector supplyCollector, AiServerClient aiServer, JsonMapper jsonMapper,
			@Qualifier("applicationTaskExecutor") Executor executor) {
		this.jobRepository = jobRepository;
		this.catalog = catalog;
		this.kakao = kakao;
		this.eventCollector = eventCollector;
		this.ingredientCollector = ingredientCollector;
		this.supplyCollector = supplyCollector;
		this.aiServer = aiServer;
		this.jsonMapper = jsonMapper;
		this.executor = executor;
	}

	public AnalysisCreatedResponse create(AnalysisRequest request) {
		IndustryResponse industry = catalog.industry(request.industryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "없는 업종입니다."));
		List<IngredientResponse> ingredients = items(request.ingredientIds(), catalog::ingredients, IngredientResponse::id, "원자재");
		List<SupplyResponse> supplies = items(request.supplyIds(), catalog::supplies, SupplyResponse::id, "부자재");
		AdminDong dong = kakao.adminDong(request.latitude(), request.longitude());
		if (dong == null || !SEOUL.equals(dong.sido())) {
			throw new BusinessException(ErrorCode.ANALYSIS_OUTSIDE_SEOUL);
		}
		String areaLabel = catalog.nearestAreaName(request.latitude(), request.longitude())
				.map(area -> shortDong(dong.dong()) + " · " + area + " 인근").orElse(shortDong(dong.dong()));
		StoreSummary store = new StoreSummary(request.storeName(), request.roadAddress(), dong.sigungu(), areaLabel, industry,
				request.operationType(), ingredients, supplies);
		AnalysisJob job = new AnalysisJob(UUID.randomUUID(), OffsetDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS),
				json(new AnalysisInput(request, store, dong.code())), json(steps(new EnumMap<>(StepCode.class))));
		jobRepository.save(job);
		executor.execute(() -> run(job.getId()));
		return new AnalysisCreatedResponse(job.getId(), AnalysisStatus.PENDING);
	}

	/** 화면용 동 이름: 성수1가1동 → 성수동, 상계3·4동 → 상계동. 숫자가 없으면 그대로 */
	static String shortDong(String dong) {
		return dong.replaceFirst("^([가-힣]+?)\\d.*동$", "$1동");
	}

	/** 고른 품목이 모두 있고 겹치지 않는지 확인하고, 고른 순서대로 이름을 붙인다. */
	private static <T> List<T> items(List<Long> ids, Function<List<Long>, List<T>> lookup, Function<T, Long> idOf, String label) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		if (new HashSet<>(ids).size() != ids.size()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "같은 " + label + "를 두 번 골랐습니다.");
		}
		Map<Long, T> found = new HashMap<>();
		lookup.apply(ids).forEach(item -> found.put(idOf.apply(item), item));
		if (!found.keySet().containsAll(ids)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "없는 " + label + "가 있습니다.");
		}
		return ids.stream().map(found::get).toList();
	}

	public AnalysisResponse get(UUID id) {
		AnalysisJob job = find(id);
		AnalysisInput input = read(job.getInputSnapshot(), AnalysisInput.class);
		return new AnalysisResponse(job.getId(), job.getStatus(), kst(job), input.store(),
				List.of(read(job.getSteps(), AnalysisStep[].class)), read(job.getEventSearch(), EventSearch.class),
				read(job.getFailure(), AnalysisFailure.class), read(job.getFinalResult(), BriefingResponse.class));
	}

	public void feedback(UUID id, SuggestionFeedbackRequest request) {
		AnalysisJob job = find(id);
		BriefingResponse briefing = read(job.getFinalResult(), BriefingResponse.class);
		if (briefing == null) {
			throw new BusinessException(ErrorCode.CONFLICT, "분석이 끝나지 않아 피드백을 받을 수 없습니다.");
		}
		SuggestionResponse card = briefing.suggestions().stream().filter(s -> s.cardKey().equals(request.cardKey())).findFirst()
				.orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "이 분석에 없는 카드입니다."));
		long industryId = read(job.getInputSnapshot(), AnalysisInput.class).request().industryId();
		catalog.upsertFeedback(id, industryId, card.cardKey(), card.category().name(), card.taskType().name(), card.itemId(),
				request.vote().name());
	}

	private AnalysisJob find(UUID id) {
		return jobRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "없는 분석입니다."));
	}

	/** 4단계를 차례로 실행한다. 어느 단계든 실패하면 그 단계를 FAILED로 두고 멈춘다. */
	void run(UUID id) {
		AnalysisJob job = find(id);
		AnalysisInput input = read(job.getInputSnapshot(), AnalysisInput.class);
		Map<StepCode, StepStatus> steps = new EnumMap<>(StepCode.class);
		StepCode step = StepCode.STORE_CHECK;
		try {
			start(job, steps, step);
			StoreContext store = storeContext(job, input);

			step = next(job, steps, step);
			EventEvidence events = eventCollector.collect(store);
			job.recordEventSearch(json(events.search()));

			step = next(job, steps, step);
			IngredientEvidenceResult ingredients = ingredientCollector.collect(store);
			List<SupplyEvidence> supplies = supplyCollector.collect(store, events);

			step = next(job, steps, step);
			BriefingPlanner planner = new BriefingPlanner(store, events, ingredients, supplies, input.request().excludedKeys());
			job.recordEvidence(json(planner.scores()), BriefingPlanner.SCORE_VERSION);
			List<SuggestionResponse> suggestions = List.of();
			// 칸이 0개면 Python을 부르지 않고 완료한다 (실패 아님)
			if (!planner.cards().isEmpty()) {
				suggestions = planner.suggestions(aiServer.analyze(planner.aiRequest(input.store())));
				if (suggestions.isEmpty()) {
					throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "ai-server 응답에 쓸 수 있는 카드가 없습니다.");
				}
			}
			steps.put(step, StepStatus.DONE);
			job.succeed(json(steps(steps)), json(planner.briefing(suggestions)));
			jobRepository.save(job);
		}
		catch (RuntimeException e) {
			ErrorCode code = e instanceof BusinessException b ? b.getErrorCode() : ErrorCode.INTERNAL_ERROR;
			if (e instanceof BusinessException) {
				log.warn("분석 {} {} 단계 실패: {}", id, step, e.getMessage());
			}
			else {
				log.error("분석 {} {} 단계 실패", id, step, e);
			}
			steps.put(step, StepStatus.FAILED);
			job.fail(json(steps(steps)), json(new AnalysisFailure(step, code.name(), FAILURE_MESSAGES.get(step))));
			jobRepository.save(job);
		}
	}

	private StoreContext storeContext(AnalysisJob job, AnalysisInput input) {
		AnalysisRequest r = input.request();
		StoreSummary s = input.store();
		return new StoreContext(job.getId(), kst(job), kst(job).toLocalDate(), r.latitude(),
				r.longitude(), s.gu(), input.adminDongCode(), s.industry().id(), s.industry().category(), s.industry().name(),
				r.operationType(), s.ingredients().stream().map(IngredientResponse::id).toList(),
				s.supplies().stream().map(SupplyResponse::id).toList());
	}

	/** DB에서 읽은 시각은 UTC로 오므로 화면·Python에 넘길 때 한국 시각으로 바꾼다 */
	private static OffsetDateTime kst(AnalysisJob job) {
		return job.getBaseTime().atZoneSameInstant(KST).toOffsetDateTime();
	}

	private void start(AnalysisJob job, Map<StepCode, StepStatus> steps, StepCode step) {
		steps.put(step, StepStatus.RUNNING);
		job.progress(json(steps(steps)));
		jobRepository.save(job);
	}

	private StepCode next(AnalysisJob job, Map<StepCode, StepStatus> steps, StepCode done) {
		steps.put(done, StepStatus.DONE);
		StepCode next = StepCode.values()[done.ordinal() + 1];
		start(job, steps, next);
		return next;
	}

	/** StepCode 순서로 항상 4개. 아직 시작하지 않은 단계는 WAITING */
	private static List<AnalysisStep> steps(Map<StepCode, StepStatus> statuses) {
		return Arrays.stream(StepCode.values()).map(c -> new AnalysisStep(c, statuses.getOrDefault(c, StepStatus.WAITING))).toList();
	}

	private String json(Object value) {
		return jsonMapper.writeValueAsString(value);
	}

	private <T> T read(String json, Class<T> type) {
		return json == null ? null : jsonMapper.readValue(json, type);
	}
}
