package com.hackathon.server.analysis.service;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.hackathon.server.analysis.dto.AiAnalyzeRequest;
import com.hackathon.server.analysis.dto.AiAnalyzeResponse;
import com.hackathon.server.analysis.dto.BriefingResponse;
import com.hackathon.server.analysis.dto.EventResponse;
import com.hackathon.server.analysis.dto.EventSource;
import com.hackathon.server.analysis.dto.EventStatus;
import com.hackathon.server.analysis.dto.EventTrigger;
import com.hackathon.server.analysis.dto.IngredientPriceResponse;
import com.hackathon.server.analysis.dto.OperationType;
import com.hackathon.server.analysis.dto.SeasonEventResponse;
import com.hackathon.server.analysis.dto.SourceResponse;
import com.hackathon.server.analysis.dto.StoreSummary;
import com.hackathon.server.analysis.dto.SuggestionCategory;
import com.hackathon.server.analysis.dto.SuggestionResponse;
import com.hackathon.server.analysis.dto.SupplyTimingResponse;
import com.hackathon.server.analysis.dto.TaskType;
import com.hackathon.server.analysis.dto.WeatherImpact;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.dto.WeatherWarning;
import com.hackathon.server.analysis.evidence.EventCandidate;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.IngredientEvidence;
import com.hackathon.server.analysis.evidence.IngredientEvidenceResult;
import com.hackathon.server.analysis.evidence.IngredientSignal;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.analysis.evidence.SupplyEvidence;
import com.hackathon.server.ingredient.entity.PriceStage;
import com.hackathon.server.supply.dto.Demand;
import com.hackathon.server.supply.dto.TriggerType;

/**
 * 4단계(준비할 일 정리)의 Java 몫: 근거 점수 → 카드 칸·사유·준비 기한 → Python 요청, Python 응답 → 강추 → 브리핑.
 * 기준: ai-server/docs/API_CONTRACT_DRAFT.md "강추 고르기"·"준비 기한", ai-server/docs/SUGGESTION_TODOS_DRAFT.md "카드 칸 만들기"
 */
final class BriefingPlanner {

	/** briefing_analysis_job.score_version. 점수표·카드 규칙을 바꾸면 올린다. */
	static final String SCORE_VERSION = "2026-11-21";
	static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("M.d(E)", Locale.KOREAN);

	private static final String FOOD = "음식점";
	private static final String RETAIL = "소매점";
	private static final String SERVICE = "단체·개인서비스업";
	private static final int MAX_EVENT_CARDS = 3;
	private static final int MAX_INGREDIENT_CARDS = 3;
	private static final int MAX_SUPPLY_CARDS = 2;
	private static final int MAX_CARD_EVIDENCE = 3;
	private static final int MAX_RECOMMENDED = 3;
	private static final int MAX_RECOMMENDED_PER_CATEGORY = 2;
	private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})");
	private static final Pattern SEASON_MONTH = Pattern.compile("^(IN_SEASON|SEASON_START)_MONTH_(\\d{2})$");
	private static final Map<String, String> HOLIDAY_RULES = Map.of("BEFORE_KIMJANG", "KIMJANG", "BEFORE_SEOLLAL", "SEOLLAL",
			"BEFORE_CHUSEOK", "CHUSEOK");
	private static final Map<String, String> HOLIDAY_LABELS = Map.of("KIMJANG", "김장철", "SEOLLAL", "설", "CHUSEOK", "추석");
	private static final Set<WeatherImpact> DELIVERY_WEATHER = Set.of(WeatherImpact.RAIN_SNOW, WeatherImpact.HEAVY_SNOW,
			WeatherImpact.COLD_WAVE, WeatherImpact.FINE_DUST);

	/** 근거 하나의 점수 (evidence_snapshot 한 줄). deadline은 행동 마감 = 카드의 준비 기한, 원자재는 null */
	record Score(String evidenceId, LocalDate deadline, int urgency, int impact, int certainty, int total) {
	}

	/** Java가 정한 카드 칸. */
	record Card(String cardId, SuggestionCategory category, TaskType taskType, Long itemId, List<String> evidenceIds, String reason,
			LocalDate dueDate) {

		String key() {
			return category + ":" + taskType + ":" + (itemId == null ? "-" : itemId);
		}

		Card withId(String id) {
			return new Card(id, category, taskType, itemId, evidenceIds, reason, dueDate);
		}
	}

	private record Times(LocalTime start, LocalTime end) {
	}

	private final StoreContext store;
	private final EventEvidence events;
	private final IngredientEvidenceResult ingredients;
	private final List<SupplyEvidence> supplies;
	private final Set<String> excluded;
	private final Map<String, Score> scores = new LinkedHashMap<>();
	private final Map<String, EventCandidate> eventById = new HashMap<>();
	private final Map<String, SeasonEventResponse> seasonById = new HashMap<>();
	private final Map<String, IngredientEvidence> ingredientById = new HashMap<>();
	private final Map<String, SupplyEvidence> supplyById = new LinkedHashMap<>();
	private final List<Card> cards;

	BriefingPlanner(StoreContext store, EventEvidence events, IngredientEvidenceResult ingredients, List<SupplyEvidence> supplies,
			Collection<String> excludedKeys) {
		this.store = store;
		this.events = events;
		this.ingredients = ingredients;
		this.supplies = supplies;
		this.excluded = excludedKeys == null ? Set.of() : Set.copyOf(excludedKeys);
		events.events().forEach(c -> eventById.put(c.event().evidenceId(), c));
		events.seasonEvents().forEach(h -> seasonById.put(h.evidenceId(), h));
		ingredients.items().forEach(r -> ingredientById.put(r.price().evidenceId(), r));
		supplies.stream().filter(BriefingPlanner::hit).forEach(s -> supplyById.put(s.timing().evidenceId(), s));
		scoreAll();
		List<Card> supplyCards = supplyCards();
		List<Card> all = new ArrayList<>(eventCards(stockTriggers(supplyCards)));
		all.addAll(ingredientCards());
		all.addAll(supplyCards);
		this.cards = IntStream.range(0, all.size()).mapToObj(i -> all.get(i).withId("C" + (i + 1))).toList();
	}

	List<Score> scores() {
		return List.copyOf(scores.values());
	}

	List<Card> cards() {
		return cards;
	}

	// ---- 근거 점수 = 급한 정도 + 영향 크기 + 확실성 (각 0~3) ----

	private void scoreAll() {
		LocalDate base = store.baseDate();
		for (EventCandidate c : events.events()) {
			EventResponse e = c.event();
			LocalDate deadline = e.status() == EventStatus.ONGOING ? base : notBefore(e.startDate());
			put(e.evidenceId(), deadline, urgency(deadline), distanceImpact(e.distanceM()), 3);
		}
		for (SeasonEventResponse h : events.seasonEvents()) {
			LocalDate deadline = notBefore(h.startDate());
			put(h.evidenceId(), deadline, urgency(deadline), 1, 3);
		}
		WeatherResponse w = events.weather();
		if (w != null) {
			LocalDate deadline = weatherStart(w);
			put(w.evidenceId(), deadline, urgency(deadline), !w.warnings().isEmpty() ? 3 : w.impacts().isEmpty() ? 0 : 2, 2);
		}
		for (IngredientEvidence r : ingredients.items()) {
			int signal = signal(r);
			if (signal != 0) {
				// "이번 주 발주 전"으로 보고 4~7일(1점). 0일로 두면 작은 가격 변동도 늘 1등이 된다
				put(r.price().evidenceId(), null, 1, Math.min(3, Math.abs(signal)), r.price().price() != null ? 3 : 1);
			}
		}
		for (SupplyEvidence s : supplyById.values()) {
			SupplyTimingResponse t = s.timing();
			Score trigger = t.triggerEvidenceId() == null ? null : scores.get(t.triggerEvidenceId());
			LocalDate deadline = t.prepareBy() != null ? t.prepareBy() : trigger != null ? trigger.deadline() : null;
			int impact = switch (t.demand()) {
				case STRONG_UP -> 3;
				case UP -> 2;
				case DOWN -> 1;
			};
			// 확실성은 부자재를 걸리게 한 행사·날씨 근거를 따른다
			int certainty = trigger != null ? trigger.certainty() : s.forecast() ? 2 : 3;
			put(t.evidenceId(), deadline, deadline == null ? 1 : urgency(deadline), impact, certainty);
		}
	}

	private void put(String id, LocalDate deadline, int urgency, int impact, int certainty) {
		scores.put(id, new Score(id, deadline, urgency, impact, certainty, urgency + impact + certainty));
	}

	private int urgency(LocalDate deadline) {
		long days = ChronoUnit.DAYS.between(store.baseDate(), deadline);
		return days <= 1 ? 3 : days <= 3 ? 2 : days <= 7 ? 1 : 0;
	}

	private static int distanceImpact(int meters) {
		return meters <= 500 ? 3 : meters <= 1000 ? 2 : meters <= 1500 ? 1 : 0;
	}

	private LocalDate notBefore(LocalDate date) {
		return date.isBefore(store.baseDate()) ? store.baseDate() : date;
	}

	/** shortcut: WeatherResponse에 영향 시작일이 없어 특보면 기준일, 아니면 첫 예보일로 본다. 수집기가 영향일을 주면 그 값으로 바꾼다 */
	private LocalDate weatherStart(WeatherResponse w) {
		if (!w.warnings().isEmpty()) {
			return store.baseDate();
		}
		return w.forecasts().stream().map(f -> f.date()).filter(d -> !d.isBefore(store.baseDate())).min(naturalOrder())
				.orElse(store.baseDate());
	}

	/** 원자재 신호 = 걸린 규칙 demand 합: STRONG_UP +2, UP +1, DOWN −1, STRONG_DOWN −2 */
	private static int signal(IngredientEvidence r) {
		return r.signals().stream().mapToInt(s -> switch (s.demand()) {
			case "STRONG_UP" -> 2;
			case "UP" -> 1;
			case "DOWN" -> -1;
			case "STRONG_DOWN" -> -2;
			default -> 0;
		}).sum();
	}

	private static boolean hit(SupplyEvidence s) {
		return s.timing().evidenceId() != null && s.timing().demand() != null && s.kind() != null;
	}

	/** 카드 점수: 카드 근거 중 가장 높은 근거 점수 (같으면 급한 정도 높은 것) */
	private Score best(Card c) {
		return c.evidenceIds().stream().map(scores::get).filter(Objects::nonNull)
				.max(comparingInt(Score::total).thenComparingInt(Score::urgency)).orElseThrow();
	}

	// ---- 카드 칸 ----

	private List<Card> eventCards(Set<String> stockTriggers) {
		Map<TaskType, List<String>> opened = new EnumMap<>(TaskType.class);
		switch (events.search().source()) {
			case CULTURAL_EVENT -> events.events().forEach(c -> eventTypes(c).forEach(t -> open(opened, t, c.event().evidenceId())));
			case SEASON -> events.seasonEvents().forEach(h -> seasonTypes(h).forEach(t -> open(opened, t, h.evidenceId())));
			case WEATHER -> {
				WeatherResponse w = events.weather();
				if (w != null && !w.impacts().isEmpty()) {
					weatherTypes(w).forEach(t -> open(opened, t, w.evidenceId()));
				}
			}
			case NONE -> {
			}
		}
		// 부자재 묶음에 같은 근거로 걸린 판매 상품(kind=STOCK)이 있으면 재고 칸은 그쪽에서 다룬다
		List<String> stock = opened.get(TaskType.STOCK);
		if (stock != null) {
			stock.removeAll(stockTriggers);
			if (stock.isEmpty()) {
				opened.remove(TaskType.STOCK);
			}
		}
		Comparator<String> byDeadline = comparing(id -> scores.get(id).deadline(), nullsLast(naturalOrder()));
		return opened.entrySet().stream()
				.map(en -> {
					List<String> ids = en.getValue().stream().sorted(byDeadline).limit(MAX_CARD_EVIDENCE).toList();
					return new Card(null, SuggestionCategory.EVENT, en.getKey(), null, ids, eventReason(ids), scores.get(ids.get(0)).deadline());
				})
				.filter(c -> !excluded.contains(c.key()))
				// 대표 근거 마감 빠른 순 → 카드 점수 높은 순 → 유형 순서(EnumMap 순서, 정렬은 안정적)
				.sorted(comparing(Card::dueDate, nullsLast(naturalOrder())).thenComparingInt(c -> -best(c).total()))
				.limit(MAX_EVENT_CARDS)
				.toList();
	}

	private static void open(Map<TaskType, List<String>> opened, TaskType type, String evidenceId) {
		opened.computeIfAbsent(type, t -> new ArrayList<>()).add(evidenceId);
	}

	private List<TaskType> eventTypes(EventCandidate c) {
		EventResponse e = c.event();
		Times times = times(e.timeText());
		List<TaskType> types = new ArrayList<>();
		if (food() || retail()) {
			types.add(TaskType.STAFFING);
			types.add(TaskType.STOCK);
		}
		if ((times.end() != null && !times.end().isBefore(LocalTime.of(21, 0)))
				|| (times.start() != null && times.start().isBefore(LocalTime.of(11, 0)))) {
			types.add(TaskType.HOURS);
		}
		if (food() && store.operationType() != OperationType.DELIVERY) {
			types.add(TaskType.PICKUP);
		}
		if (service()) {
			types.add(TaskType.RESERVATION);
		}
		if (food() && (e.trigger() == EventTrigger.FESTIVAL || (times.end() != null && !times.end().isBefore(LocalTime.of(17, 0))))) {
			types.add(TaskType.PACKING);
		}
		return types;
	}

	private List<TaskType> seasonTypes(SeasonEventResponse h) {
		List<TaskType> types = new ArrayList<>();
		if (food() || retail()) {
			types.add(TaskType.STOCK);
		}
		if ("SEOLLAL".equals(h.code()) || "CHUSEOK".equals(h.code())) {
			types.add(TaskType.HOURS);
		}
		if ((food() || retail()) && h.type() == TriggerType.HOLIDAY && store.operationType() != OperationType.DELIVERY) {
			types.add(TaskType.PICKUP);
		}
		if (service()) {
			types.add(TaskType.RESERVATION);
		}
		return types;
	}

	private List<TaskType> weatherTypes(WeatherResponse w) {
		List<TaskType> types = new ArrayList<>();
		OperationType op = store.operationType();
		if (food() && (op == OperationType.DELIVERY || op == OperationType.BOTH)
				&& w.impacts().stream().anyMatch(DELIVERY_WEATHER::contains)) {
			types.add(TaskType.STAFFING);
		}
		if (!w.warnings().isEmpty()) {
			types.add(TaskType.HOURS);
		}
		if (service()) {
			types.add(TaskType.RESERVATION);
		}
		if (op != OperationType.DELIVERY) {
			types.add(TaskType.FACILITY);
		}
		return types;
	}

	private List<Card> ingredientCards() {
		List<Card> list = new ArrayList<>();
		for (IngredientEvidence r : ingredients.items()) {
			int signal = signal(r);
			if (signal == 0) {
				continue;
			}
			TaskType type = signal < 0 ? TaskType.PRICE_CHECK : holidayCode(r) != null ? TaskType.BUY_AHEAD : TaskType.ORDER_PLAN;
			IngredientPriceResponse p = r.price();
			list.add(new Card(null, SuggestionCategory.INGREDIENT, type, p.itemId(), List.of(p.evidenceId()), ingredientReason(r, type), null));
		}
		// 카드 점수 높은 순 → 1주 변동률 절댓값 큰 순 → 사장님이 고른 순(안정 정렬)
		return list.stream().filter(c -> !excluded.contains(c.key()))
				.sorted(comparingInt((Card c) -> -best(c).total()).thenComparing(c -> -weekChange(c)))
				.limit(MAX_INGREDIENT_CARDS)
				.toList();
	}

	private double weekChange(Card c) {
		IngredientPriceResponse p = ingredientById.get(c.evidenceIds().get(0)).price();
		return p.change1w() == null ? -1 : Math.abs(p.change1w().percent());
	}

	private static String holidayCode(IngredientEvidence r) {
		return r.signals().stream().map(s -> HOLIDAY_RULES.get(s.ruleCode())).filter(Objects::nonNull).findFirst().orElse(null);
	}

	private List<Card> supplyCards() {
		List<Card> list = new ArrayList<>();
		for (SupplyEvidence s : supplies) {
			if (!hit(s)) {
				continue;
			}
			SupplyTimingResponse t = s.timing();
			TaskType type = t.demand() == Demand.DOWN ? TaskType.ORDER_LESS : "STOCK".equals(s.kind()) ? TaskType.DISPLAY : TaskType.RESTOCK;
			List<String> ids = t.triggerEvidenceId() != null && scores.containsKey(t.triggerEvidenceId())
					? List.of(t.evidenceId(), t.triggerEvidenceId()) : List.of(t.evidenceId());
			list.add(new Card(null, SuggestionCategory.SUPPLY, type, t.itemId(), ids, supplyReason(t, type), t.prepareBy()));
		}
		// prepare_by 빠른 순 → 카드 점수 높은 순 → 사장님이 고른 순(안정 정렬)
		return list.stream().filter(c -> !excluded.contains(c.key()))
				.sorted(comparing(Card::dueDate, nullsLast(naturalOrder())).thenComparingInt(c -> -best(c).total()))
				.limit(MAX_SUPPLY_CARDS)
				.toList();
	}

	private Set<String> stockTriggers(List<Card> supplyCards) {
		Set<String> triggers = new HashSet<>();
		for (Card c : supplyCards) {
			SupplyEvidence s = supplyById.get(c.evidenceIds().get(0));
			if ("STOCK".equals(s.kind()) && s.timing().triggerEvidenceId() != null) {
				triggers.add(s.timing().triggerEvidenceId());
			}
		}
		return triggers;
	}

	private boolean food() {
		return FOOD.equals(store.industryCategory());
	}

	private boolean retail() {
		return RETAIL.equals(store.industryCategory());
	}

	private boolean service() {
		return SERVICE.equals(store.industryCategory());
	}

	/** "16:00–19:00" → 시작·끝. 시각이 하나뿐이면 시작만, 못 읽으면 둘 다 null */
	private static Times times(String text) {
		if (text == null) {
			return new Times(null, null);
		}
		Matcher m = TIME.matcher(text);
		LocalTime start = m.find() ? time(m) : null;
		LocalTime end = start != null && m.find() ? time(m) : null;
		return new Times(start, end);
	}

	private static LocalTime time(Matcher m) {
		int hour = Integer.parseInt(m.group(1));
		int minute = Integer.parseInt(m.group(2));
		return hour >= 24 ? LocalTime.MAX : LocalTime.of(hour, Math.min(minute, 59));
	}

	// ---- 사유 (reason) 템플릿 ----

	private String eventReason(List<String> ids) {
		String rep = ids.get(0);
		EventCandidate candidate = eventById.get(rep);
		if (candidate != null) {
			EventResponse e = candidate.event();
			String head = dateLabel(e) + " " + e.distanceM() + "m 거리에서 ";
			long count = ids.stream().filter(eventById::containsKey).count();
			if (count >= 2) {
				return head + e.category() + " 등 행사 " + count + "건이 있어요.";
			}
			return head + josa(e.category(), "이", "가") + (e.status() == EventStatus.ONGOING ? " 진행 중이에요." : " 열려요.");
		}
		SeasonEventResponse season = seasonById.get(rep);
		if (season != null) {
			return untilReason(season.label(), season.startDate());
		}
		WeatherResponse w = events.weather();
		if (!w.warnings().isEmpty()) {
			WeatherWarning warning = w.warnings().get(0);
			return warning.kind() + josa(warning.level(), "이", "가") + " 발효 중이에요.";
		}
		return DATE_LABEL.format(weatherStart(w)) + " " + impactLabel(w.impacts().get(0)) + " 예보가 있어요.";
	}

	private String untilReason(String label, LocalDate start) {
		long days = ChronoUnit.DAYS.between(store.baseDate(), start);
		return days <= 0 ? label + " 기간이에요." : label + "까지 " + days + "일 남았어요.";
	}

	private String ingredientReason(IngredientEvidence r, TaskType type) {
		IngredientPriceResponse p = r.price();
		String name = p.name();
		Double change4w = p.change4w() == null ? null : p.change4w().percent();
		if (type == TaskType.PRICE_CHECK) {
			return change4w != null && change4w > 0 ? josa(name, "이", "가") + " 4주 전보다 " + percent(change4w) + "% 올랐어요."
					: josa(name, "이", "가") + " 4주 전보다 올랐어요.";
		}
		if (type == TaskType.BUY_AHEAD) {
			String code = holidayCode(r);
			return events.seasonEvents().stream().filter(h -> code.equals(h.code())).findFirst()
					.map(h -> untilReason(h.label(), h.startDate()))
					.orElse(HOLIDAY_LABELS.get(code) + " 전에 미리 사 두면 좋아요.");
		}
		boolean falling = p.priceStage() == PriceStage.WATCH_LOW || p.priceStage() == PriceStage.ALERT_LOW
				|| p.priceStage() == PriceStage.SEVERE_LOW;
		if (falling && change4w != null && change4w < 0) {
			return josa(name, "이", "가") + " 4주 전보다 " + percent(change4w) + "% 내렸어요.";
		}
		for (IngredientSignal s : r.signals()) {
			Matcher m = SEASON_MONTH.matcher(s.ruleCode());
			if (m.matches()) {
				return josa(name, "은", "는") + " " + Integer.parseInt(m.group(2)) + "월 제철이에요.";
			}
			if (s.ruleCode().startsWith("BEFORE_SEASON_START_MONTH_")) {
				return josa(name, "은", "는") + " 다음 달부터 제철이에요.";
			}
		}
		return josa(name, "은", "는") + " 지금 사기 좋은 때예요.";
	}

	private static String supplyReason(SupplyTimingResponse t, TaskType type) {
		String label = t.triggerLabel();
		if (type == TaskType.ORDER_LESS) {
			return (label != null ? label + " 기간엔" : "이번 주엔") + " 덜 쓰일 수 있어요.";
		}
		if (t.prepareBy() == null) {
			return (label != null ? label + " 준비가" : "이번 주 준비가") + " 필요해요.";
		}
		String date = DATE_LABEL.format(t.prepareBy());
		return label != null ? label + " 준비는 " + date + "까지예요." : date + "까지 준비하면 좋아요.";
	}

	private static String impactLabel(WeatherImpact impact) {
		return switch (impact) {
			case RAIN_SNOW -> "비·눈";
			case HEAVY_SNOW -> "대설";
			case COLD_WAVE -> "한파";
			case HEATWAVE -> "폭염";
			case STRONG_WIND -> "강풍";
			case FINE_DUST -> "미세먼지";
			case WARNING -> "기상특보";
		};
	}

	/** 받침이 있으면 consonant(이·은), 없으면 vowel(가·는). 한글이 아닌 끝 글자는 받침 없음으로 본다 */
	static String josa(String word, String consonant, String vowel) {
		char last = word.charAt(word.length() - 1);
		boolean batchim = last >= '가' && last <= '힣' && (last - '가') % 28 != 0;
		return word + (batchim ? consonant : vowel);
	}

	private static String percent(double value) {
		return BigDecimal.valueOf(Math.abs(value)).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	private static String dateLabel(EventResponse e) {
		return e.startDate().equals(e.endDate()) ? DATE_LABEL.format(e.startDate())
				: DATE_LABEL.format(e.startDate()) + "~" + DATE_LABEL.format(e.endDate());
	}

	// ---- Python 요청 ----

	AiAnalyzeRequest aiRequest(StoreSummary summary) {
		EventSource source = events.search().source();
		Set<String> triggers = new HashSet<>();
		supplyById.values().forEach(s -> triggers.add(s.timing().triggerEvidenceId()));
		WeatherResponse w = events.weather();
		AiAnalyzeRequest.Weather weather = w != null && (source == EventSource.WEATHER || triggers.contains(w.evidenceId()))
				? new AiAnalyzeRequest.Weather(w.evidenceId(), w.impacts(),
						w.warnings().stream().map(x -> new AiAnalyzeRequest.Warning(x.kind(), x.level())).toList(),
						DATE_LABEL.format(weatherStart(w)))
				: null;
		int radius = events.events().stream().anyMatch(c -> c.event().distanceM() > 1000) ? 1500 : 1000;
		return new AiAnalyzeRequest(
				store.analysisId().toString(),
				DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(store.baseTime()),
				new AiAnalyzeRequest.Store(summary.gu(), summary.areaLabel(), store.industryCategory(), store.industryName(),
						store.operationType()),
				new AiAnalyzeRequest.Search(radius, events.search().days()),
				events.events().stream().map(BriefingPlanner::aiEvent).toList(),
				events.seasonEvents().stream().filter(h -> source == EventSource.SEASON || triggers.contains(h.evidenceId()))
						.map(h -> new AiAnalyzeRequest.Season(h.evidenceId(), h.code(), h.label(), h.type().name(),
								DATE_LABEL.format(h.startDate()), Math.max(0, ChronoUnit.DAYS.between(store.baseDate(), h.startDate()))))
						.toList(),
				weather,
				ingredients.items().stream().map(BriefingPlanner::aiRawMaterial).toList(),
				supplies.stream().filter(BriefingPlanner::hit).map(BriefingPlanner::aiSupply).toList(),
				cards.stream().map(c -> new AiAnalyzeRequest.Card(c.cardId(), c.category(), c.taskType(), c.itemId(), c.evidenceIds()))
						.toList());
	}

	private static AiAnalyzeRequest.Event aiEvent(EventCandidate c) {
		EventResponse e = c.event();
		return new AiAnalyzeRequest.Event(e.evidenceId(), e.title(), e.category(), e.place(), dateLabel(e), e.status(), e.distanceM(),
				e.walkMinutes(), e.isFree(), e.trigger(), c.timeBand() == null ? "UNKNOWN" : c.timeBand(), c.peakLabel());
	}

	private static AiAnalyzeRequest.RawMaterial aiRawMaterial(IngredientEvidence r) {
		IngredientPriceResponse p = r.price();
		return new AiAnalyzeRequest.RawMaterial(p.evidenceId(), p.itemId(), p.name(), p.unit(), p.grade(),
				p.baseDate() == null ? null : p.baseDate().toString(), p.price(),
				p.change1w() == null ? null : p.change1w().percent(), p.change4w() == null ? null : p.change4w().percent(),
				p.trend(), p.consecutiveWeeks(), p.priceStage() == null ? null : p.priceStage().name(),
				r.signals().stream().map(s -> new AiAnalyzeRequest.Signal(s.text(), s.demand())).toList());
	}

	private static AiAnalyzeRequest.Supply aiSupply(SupplyEvidence s) {
		SupplyTimingResponse t = s.timing();
		return new AiAnalyzeRequest.Supply(t.evidenceId(), t.itemId(), t.name(), s.kind(), t.demand().name(), t.triggerLabel(),
				t.triggerEvidenceId(), t.prepareBy() == null ? null : DATE_LABEL.format(t.prepareBy()));
	}

	// ---- Python 응답 → 강추 → 브리핑 ----

	/** 요청에 없는 card_id는 버리고, 응답이 빠진 칸은 내지 않는다. 순서는 요청(cards) 순서 */
	List<SuggestionResponse> suggestions(AiAnalyzeResponse response) {
		Map<String, AiAnalyzeResponse.Suggestion> written = new HashMap<>();
		response.suggestions().stream().filter(s -> s != null && s.card_id() != null && s.title() != null)
				.forEach(s -> written.putIfAbsent(s.card_id(), s));
		List<Card> kept = cards.stream().filter(c -> written.containsKey(c.cardId())).toList();
		Set<String> picks = recommended(kept);
		return IntStream.range(0, kept.size()).mapToObj(i -> {
			Card c = kept.get(i);
			AiAnalyzeResponse.Suggestion s = written.get(c.cardId());
			return new SuggestionResponse(i + 1, c.key(), c.category(), c.taskType(), c.itemId(), s.title(), c.reason(), c.dueDate(),
					s.expected_effect(), s.sub_tasks() == null ? List.of() : s.sub_tasks(), picks.contains(c.cardId()), c.evidenceIds());
		}).toList();
	}

	/** 점수 높은 순 → 급한 정도 높은 순 → cards 순서로 3개. 같은 묶음 최대 2개, 같은 대표 근거 1개 */
	private Set<String> recommended(List<Card> kept) {
		List<Card> ranked = kept.stream()
				.sorted(comparingInt((Card c) -> -best(c).total()).thenComparingInt(c -> -best(c).urgency()))
				.toList();
		Set<String> picks = new HashSet<>();
		Map<SuggestionCategory, Integer> perCategory = new EnumMap<>(SuggestionCategory.class);
		Set<String> representatives = new HashSet<>();
		for (Card c : ranked) {
			if (picks.size() == MAX_RECOMMENDED) {
				break;
			}
			if (perCategory.getOrDefault(c.category(), 0) < MAX_RECOMMENDED_PER_CATEGORY && representatives.add(c.evidenceIds().get(0))) {
				picks.add(c.cardId());
				perCategory.merge(c.category(), 1, Integer::sum);
			}
		}
		return picks;
	}

	BriefingResponse briefing(List<SuggestionResponse> suggestions) {
		List<EventResponse> nearby = events.events().stream().map(EventCandidate::event).toList();
		List<SeasonEventResponse> seasons = nearby.isEmpty() ? events.seasonEvents() : List.of();
		WeatherResponse weather = nearby.isEmpty() && seasons.isEmpty() ? events.weather() : null;
		List<SourceResponse> sources = Stream.concat(events.sources().stream(), ingredients.sources().stream()).toList();
		return new BriefingResponse(nearby, seasons, weather, ingredients.items().stream().map(IngredientEvidence::price).toList(),
				supplies.stream().map(SupplyEvidence::timing).toList(), suggestions, sources);
	}
}
