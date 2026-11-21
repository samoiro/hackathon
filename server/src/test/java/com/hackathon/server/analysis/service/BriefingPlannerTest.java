package com.hackathon.server.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.analysis.dto.AiAnalyzeResponse;
import com.hackathon.server.analysis.dto.EventSearch;
import com.hackathon.server.analysis.dto.EventSource;
import com.hackathon.server.analysis.dto.OperationType;
import com.hackathon.server.analysis.dto.SeasonEventResponse;
import com.hackathon.server.analysis.dto.SuggestionCategory;
import com.hackathon.server.analysis.dto.SuggestionResponse;
import com.hackathon.server.analysis.dto.SupplyTimingResponse;
import com.hackathon.server.analysis.dto.TaskType;
import com.hackathon.server.analysis.dto.WeatherForecast;
import com.hackathon.server.analysis.dto.WeatherImpact;
import com.hackathon.server.analysis.dto.WeatherImpactStart;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.dto.WeatherWarning;
import com.hackathon.server.analysis.dto.DayPart;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.IngredientEvidenceResult;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.analysis.evidence.SupplyEvidence;
import com.hackathon.server.supply.dto.Demand;
import com.hackathon.server.supply.dto.TriggerType;

/** 근거 점수·카드 칸·사유·준비 기한·강추. 기준: ai-server/docs/API_CONTRACT_DRAFT.md, SUGGESTION_TODOS_DRAFT.md */
class BriefingPlannerTest {

	@Test
	void scoresEachEvidenceOnThreeAxes() {
		var planner = ContractExample.planner(List.of());

		// 쌀(R3)은 신호 0이라 채우기 후보(단가 확인)로만 쓰여 급한 정도·영향이 0이다
		assertThat(planner.scores()).extracting(s -> s.evidenceId(), s -> s.urgency(), s -> s.impact(), s -> s.certainty(), s -> s.total())
				.containsExactly(
						tuple("E1", 1, 2, 3, 6),
						tuple("R1", 1, 2, 3, 6),
						tuple("R2", 1, 1, 3, 5),
						tuple("R3", 0, 0, 3, 3),
						tuple("S1", 3, 2, 3, 8),
						tuple("S2", 2, 2, 3, 7));
	}

	@Test
	void contractExampleMakesSevenCardsWithReasonsAndDueDates() {
		var cards = ContractExample.planner(List.of()).cards();

		assertThat(cards).extracting(c -> c.cardId(), c -> c.key(), c -> c.evidenceIds(), c -> c.reason(), c -> c.dueDate())
				.containsExactly(
						tuple("C1", "EVENT:STAFFING:-", List.of("E1"), "11.21(토) 650m 거리에서 공연이 열려요.", LocalDate.of(2026, 11, 21)),
						tuple("C2", "EVENT:STOCK:-", List.of("E1"), "11.21(토) 650m 거리에서 공연이 열려요.", LocalDate.of(2026, 11, 21)),
						tuple("C3", "EVENT:PICKUP:-", List.of("E1"), "11.21(토) 650m 거리에서 공연이 열려요.", LocalDate.of(2026, 11, 21)),
						tuple("C4", "INGREDIENT:PRICE_CHECK:12", List.of("R1"), "배추가 4주 전보다 10.9% 올랐어요.", null),
						tuple("C5", "INGREDIENT:ORDER_PLAN:15", List.of("R2"), "돼지고기가 4주 전보다 6.3% 내렸어요.", null),
						tuple("C6", "SUPPLY:RESTOCK:3", List.of("S1", "E1"), "공연 준비는 11.18(수)까지예요.", LocalDate.of(2026, 11, 18)),
						tuple("C7", "SUPPLY:RESTOCK:5", List.of("S2", "E1"), "공연 준비는 11.19(목)까지예요.", LocalDate.of(2026, 11, 19)));
	}

	@Test
	void recommendsTopThreeByScoreWithCategoryLimit() {
		var planner = ContractExample.planner(List.of());

		List<SuggestionResponse> suggestions = planner.suggestions(written(planner, "C1", "C2", "C3", "C4", "C5", "C6", "C7"));

		assertThat(suggestions).filteredOn(SuggestionResponse::recommended).extracting(SuggestionResponse::cardKey)
				.containsExactly("EVENT:STAFFING:-", "SUPPLY:RESTOCK:3", "SUPPLY:RESTOCK:5");
		assertThat(suggestions).extracting(SuggestionResponse::id).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L);
	}

	@Test
	void sameRepresentativeEvidenceGetsOneRecommendation() {
		var planner = ContractExample.planner(List.of());

		// 부자재 칸이 빠지면 E1 카드 셋 중 첫 번째만 강추이고, 나머지 자리는 원자재가 채운다
		List<SuggestionResponse> suggestions = planner.suggestions(written(planner, "C1", "C2", "C3", "C4", "C5"));

		assertThat(suggestions).filteredOn(SuggestionResponse::recommended).extracting(SuggestionResponse::cardKey)
				.containsExactly("EVENT:STAFFING:-", "INGREDIENT:PRICE_CHECK:12", "INGREDIENT:ORDER_PLAN:15");
	}

	@Test
	void excludedKeyLetsNextEventTypeFillTheSlot() {
		var cards = ContractExample.planner(List.of("EVENT:STAFFING:-", "INGREDIENT:PRICE_CHECK:12")).cards();

		assertThat(cards).extracting(c -> c.key()).containsExactly("EVENT:STOCK:-", "EVENT:PICKUP:-", "EVENT:PACKING:-",
				"INGREDIENT:ORDER_PLAN:15", "SUPPLY:RESTOCK:3", "SUPPLY:RESTOCK:5");
	}

	@Test
	void stockSupplyForSameEventReplacesEventStockCard() {
		var water = new SupplyEvidence(new SupplyTimingResponse("S1", 8, "생수", Demand.STRONG_UP, LocalDate.of(2026, 11, 20), 1,
				"공연", "E1"), "STOCK", false);

		var cards = new BriefingPlanner(ContractExample.store(), ContractExample.events(), empty(), List.of(water), List.of()).cards();

		assertThat(cards).extracting(c -> c.key()).containsExactly("EVENT:STAFFING:-", "EVENT:PICKUP:-", "EVENT:PACKING:-",
				"SUPPLY:DISPLAY:8");
	}

	/** impactStarts가 없는 날씨(예전 수집기)는 기준일과 발효 중인 특보로 본다 */
	@Test
	void weatherWarningOpensHoursAndFacility() {
		var warning = new WeatherWarning("한파", "주의보", OffsetDateTime.parse("2026-11-17T05:00:00+09:00"), null);
		var weather = new WeatherResponse("W1", "1120065000", List.of(WeatherImpact.COLD_WAVE, WeatherImpact.WARNING), List.of(warning),
				List.of(new WeatherForecast(LocalDate.of(2026, 11, 17), DayPart.AM, "맑음", -8.0, -1.0, 0)),
				OffsetDateTime.parse("2026-11-17T06:50:00+09:00"));
		var events = new EventEvidence(new EventSearch(7, ContractExample.BASE_DATE, 0, EventSource.WEATHER), List.of(), List.of(),
				weather, List.of());

		var cards = new BriefingPlanner(store(OperationType.STORE), events, empty(), List.of(), List.of()).cards();

		assertThat(cards).extracting(c -> c.taskType(), c -> c.reason(), c -> c.dueDate()).containsExactly(
				tuple(TaskType.HOURS, "한파주의보가 발효 중이에요.", ContractExample.BASE_DATE),
				tuple(TaskType.FACILITY, "한파주의보가 발효 중이에요.", ContractExample.BASE_DATE));
	}

	@Test
	void rainForecastStartsOnFirstRainyDay() {
		var weather = new WeatherResponse("W1", "1120065000", List.of(WeatherImpact.RAIN_SNOW), List.of(),
				List.of(new WeatherForecast(LocalDate.of(2026, 11, 17), DayPart.AM, "맑음", 2.0, null, 10),
						new WeatherForecast(LocalDate.of(2026, 11, 18), DayPart.PM, "구름많음", null, 9.0, 30),
						new WeatherForecast(LocalDate.of(2026, 11, 19), DayPart.AM, "흐리고 비", 4.0, null, 80)),
				OffsetDateTime.parse("2026-11-17T06:50:00+09:00"),
				List.of(new WeatherImpactStart(WeatherImpact.RAIN_SNOW, LocalDate.of(2026, 11, 19))));
		var events = new EventEvidence(new EventSearch(7, ContractExample.BASE_DATE, 0, EventSource.WEATHER), List.of(), List.of(),
				weather, List.of());

		var planner = new BriefingPlanner(ContractExample.store(), events, empty(), List.of(), List.of());

		// 음식점·BOTH라 배달 근무(STAFFING)와 매장 정비(FACILITY). 특보가 없어 영업시간 칸은 없다
		assertThat(planner.cards()).extracting(c -> c.taskType(), c -> c.reason(), c -> c.dueDate()).containsExactly(
				tuple(TaskType.STAFFING, "11.19(목) 비·눈 예보가 있어요.", LocalDate.of(2026, 11, 19)),
				tuple(TaskType.FACILITY, "11.19(목) 비·눈 예보가 있어요.", LocalDate.of(2026, 11, 19)));
		assertThat(planner.scores()).extracting(s -> s.urgency(), s -> s.impact(), s -> s.certainty()).containsExactly(tuple(2, 2, 2));
	}

	@Test
	void seasonEventCountsDaysLeft() {
		var christmas = new SeasonEventResponse("H1", "CHRISTMAS", "크리스마스", TriggerType.HOLIDAY, LocalDate.of(2026, 12, 25),
				LocalDate.of(2026, 12, 25));
		var events = new EventEvidence(new EventSearch(7, ContractExample.BASE_DATE, 0, EventSource.SEASON), List.of(),
				List.of(christmas), null, List.of());

		var cards = new BriefingPlanner(ContractExample.store(), events, empty(), List.of(), List.of()).cards();

		assertThat(cards).extracting(c -> c.category(), c -> c.taskType(), c -> c.reason()).containsExactly(
				tuple(SuggestionCategory.EVENT, TaskType.STOCK, "크리스마스까지 38일 남았어요."),
				tuple(SuggestionCategory.EVENT, TaskType.PICKUP, "크리스마스까지 38일 남았어요."));
	}

	@Test
	void briefingAlwaysShowsWeatherEvenWithSeasonEvents() {
		var calm = new WeatherResponse("W1", "1120065000", List.of(), List.of(),
				List.of(new WeatherForecast(LocalDate.of(2026, 11, 18), DayPart.AM, "맑음", 4.0, null, 10)),
				OffsetDateTime.parse("2026-11-17T06:50:00+09:00"));
		var events = new EventEvidence(new EventSearch(7, ContractExample.BASE_DATE, 0, EventSource.SEASON), List.of(),
				List.of(CHRISTMAS), calm, List.of());
		var bag = new SupplyEvidence(new SupplyTimingResponse("S1", 5, "배달 봉투", Demand.UP, LocalDate.of(2026, 11, 17), 1, "비", "W1"),
				"SUPPLY", true);

		var withTrigger = new BriefingPlanner(ContractExample.store(), events, empty(), List.of(bag), List.of()).briefing(List.of());
		var withoutTrigger = new BriefingPlanner(ContractExample.store(), events, empty(), List.of(), List.of()).briefing(List.of());

		assertThat(withTrigger.weather().evidenceId()).isEqualTo("W1");
		assertThat(withoutTrigger.weather().evidenceId()).isEqualTo("W1");
		assertThat(withoutTrigger.seasonEvents()).extracting(SeasonEventResponse::evidenceId).containsExactly("H1");
	}

	/** 실제 브리핑에서 카드가 4장뿐이던 경우: 시즌 칸 2 + 원자재 1 + 부자재 1 → 날씨 칸으로 6장을 채운다 */
	@Test
	void fewCardsAreFilledToSixByCardScore() {
		var events = new EventEvidence(new EventSearch(7, ContractExample.BASE_DATE, 0, EventSource.SEASON), List.of(),
				List.of(CHRISTMAS), rain(), List.of());
		var bag = new SupplyEvidence(new SupplyTimingResponse("S1", 5, "배달 봉투", Demand.UP, LocalDate.of(2026, 11, 17), 1, "비", "W1"),
				"SUPPLY", true);
		var ingredients = new IngredientEvidenceResult(List.of(ContractExample.R2, ContractExample.R3), List.of());

		var planner = new BriefingPlanner(ContractExample.store(), events, ingredients, List.of(bag), List.of());

		// 남은 후보 중 날씨(W1, 7점) 칸이 쌀 단가 확인(R3, 3점)보다 먼저 들어온다
		assertThat(planner.cards()).extracting(c -> c.key(), c -> c.filled()).containsExactly(
				tuple("EVENT:STOCK:-", false),
				tuple("EVENT:PICKUP:-", false),
				tuple("EVENT:STAFFING:-", true),
				tuple("EVENT:FACILITY:-", true),
				tuple("INGREDIENT:ORDER_PLAN:15", false),
				tuple("SUPPLY:RESTOCK:5", false));
		assertThat(planner.briefing(List.of()).weather().evidenceId()).isEqualTo("W1");

		// 채운 날씨 칸은 점수가 더 높아도 강추가 아니다
		List<SuggestionResponse> suggestions = planner.suggestions(written(planner, "C1", "C2", "C3", "C4", "C5", "C6"));
		assertThat(suggestions).filteredOn(SuggestionResponse::recommended).extracting(SuggestionResponse::cardKey)
				.containsExactly("EVENT:STOCK:-", "INGREDIENT:ORDER_PLAN:15", "SUPPLY:RESTOCK:5");
		assertThat(suggestions).filteredOn(SuggestionResponse::filled).extracting(SuggestionResponse::cardKey)
				.containsExactly("EVENT:STAFFING:-", "EVENT:FACILITY:-");
	}

	@Test
	void enoughCardsAreNotFilled() {
		var events = new EventEvidence(new EventSearch(7, ContractExample.BASE_DATE, 1, EventSource.CULTURAL_EVENT),
				List.of(ContractExample.E1), List.of(CHRISTMAS), rain(), List.of());

		var cards = new BriefingPlanner(ContractExample.store(), events, ContractExample.ingredients(), ContractExample.supplies(),
				List.of()).cards();

		// 계약 예시와 같은 7장. 시즌·날씨 칸과 쌀 단가 확인은 들어오지 않는다
		assertThat(cards).extracting(c -> c.key()).containsExactly("EVENT:STAFFING:-", "EVENT:STOCK:-", "EVENT:PICKUP:-",
				"INGREDIENT:PRICE_CHECK:12", "INGREDIENT:ORDER_PLAN:15", "SUPPLY:RESTOCK:3", "SUPPLY:RESTOCK:5");
	}

	@Test
	void quietIngredientFillsAfterSignalCardsWithPlainReason() {
		var events = new EventEvidence(new EventSearch(7, ContractExample.BASE_DATE, 0, EventSource.NONE), List.of(), List.of(), null,
				List.of());

		var cards = new BriefingPlanner(ContractExample.store(), events, ContractExample.ingredients(), List.of(), List.of()).cards();

		assertThat(cards).extracting(c -> c.key(), c -> c.reason(), c -> c.filled()).containsExactly(
				tuple("INGREDIENT:PRICE_CHECK:12", "배추가 4주 전보다 10.9% 올랐어요.", false),
				tuple("INGREDIENT:ORDER_PLAN:15", "돼지고기가 4주 전보다 6.3% 내렸어요.", false),
				tuple("INGREDIENT:PRICE_CHECK:7", "쌀은 4주 전과 가격이 같아요.", true));
	}

	@Test
	void noEvidenceMeansNoCards() {
		var events = new EventEvidence(new EventSearch(7, ContractExample.BASE_DATE, 0, EventSource.NONE), List.of(), List.of(), null,
				List.of());

		assertThat(new BriefingPlanner(ContractExample.store(), events, empty(), List.of(), null).cards()).isEmpty();
	}

	/** filled가 생기기 전에 저장된 final_result도 읽혀야 한다 */
	@Test
	void storedSuggestionWithoutFilledReadsAsNotFilled() {
		String legacy = """
				{"id":1,"cardKey":"EVENT:STOCK:-","category":"EVENT","taskType":"STOCK","itemId":null,"title":"제목","reason":"사유",
				"dueDate":null,"expectedEffect":null,"subTasks":[],"recommended":true,"evidenceIds":["H1"]}""";

		var suggestion = JsonMapper.builder().build().readValue(legacy, SuggestionResponse.class);

		assertThat(suggestion.filled()).isFalse();
		assertThat(suggestion.recommended()).isTrue();
	}

	@Test
	void josaFollowsFinalConsonant() {
		assertThat(BriefingPlanner.josa("배추", "이", "가")).isEqualTo("배추가");
		assertThat(BriefingPlanner.josa("공연", "이", "가")).isEqualTo("공연이");
		assertThat(BriefingPlanner.josa("쌀", "은", "는")).isEqualTo("쌀은");
	}

	private static final SeasonEventResponse CHRISTMAS = new SeasonEventResponse("H1", "CHRISTMAS", "크리스마스", TriggerType.HOLIDAY,
			LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 25));

	private static WeatherResponse rain() {
		return new WeatherResponse("W1", "1120065000", List.of(WeatherImpact.RAIN_SNOW), List.of(),
				List.of(new WeatherForecast(LocalDate.of(2026, 11, 18), DayPart.AM, "비", 4.0, null, 80)),
				OffsetDateTime.parse("2026-11-17T06:50:00+09:00"),
				List.of(new WeatherImpactStart(WeatherImpact.RAIN_SNOW, LocalDate.of(2026, 11, 18))));
	}

	private static AiAnalyzeResponse written(BriefingPlanner planner, String... cardIds) {
		return new AiAnalyzeResponse(List.of(cardIds).stream()
				.map(id -> new AiAnalyzeResponse.Suggestion(id, "제목 " + id, null, List.of("할 일 1", "할 일 2"))).toList(),
				"test-model", "2026-11-17T07:00:12+09:00");
	}

	private static IngredientEvidenceResult empty() {
		return new IngredientEvidenceResult(List.of(), List.of());
	}

	private static StoreContext store(OperationType operationType) {
		var s = ContractExample.store();
		return new StoreContext(s.analysisId(), s.baseTime(), s.baseDate(), s.latitude(), s.longitude(), s.gu(), s.adminDongCode(),
				s.industryId(), s.industryCategory(), s.industryName(), operationType, s.ingredientIds(), s.supplyIds());
	}
}
