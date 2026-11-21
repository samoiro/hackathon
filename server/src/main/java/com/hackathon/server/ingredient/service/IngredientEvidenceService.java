package com.hackathon.server.ingredient.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.server.analysis.dto.IngredientPriceResponse;
import com.hackathon.server.analysis.dto.PriceChange;
import com.hackathon.server.analysis.dto.SourceResponse;
import com.hackathon.server.analysis.dto.Trend;
import com.hackathon.server.analysis.evidence.IngredientEvidence;
import com.hackathon.server.analysis.evidence.IngredientEvidenceCollector;
import com.hackathon.server.analysis.evidence.IngredientEvidenceResult;
import com.hackathon.server.analysis.evidence.IngredientSignal;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.ingredient.entity.Ingredient;
import com.hackathon.server.ingredient.entity.IngredientPriceBand;
import com.hackathon.server.ingredient.entity.IngredientPriceHistory;
import com.hackathon.server.ingredient.entity.IngredientRule;
import com.hackathon.server.ingredient.entity.IngredientRuleItem;
import com.hackathon.server.ingredient.entity.PriceStage;
import com.hackathon.server.ingredient.repository.IngredientPriceBandRepository;
import com.hackathon.server.ingredient.repository.IngredientPriceHistoryRepository;
import com.hackathon.server.ingredient.repository.IngredientRepository;
import com.hackathon.server.ingredient.repository.IngredientRuleItemRepository;
import com.hackathon.server.ingredient.repository.IngredientRuleRepository;
import com.hackathon.server.supply.entity.SupplyTriggerDate;
import com.hackathon.server.supply.repository.SupplyTriggerDateRepository;

/**
 * 3단계(품목 가격 조회) 원자재 근거. 기준일까지 가장 최근 조사일 가격으로 1·4주 변동, 흐름, 4주 단계를 계산하고
 * 이번 주 걸린 ingredient_rule(가격 단계, 제철, 제철 한 달 전, 김장·설·추석 전)을 붙인다.
 * 기준: docs/database-schema.md ingredient_rule · ingredient_price_band
 */
@Service
@Transactional(readOnly = true)
public class IngredientEvidenceService implements IngredientEvidenceCollector {

	private static final String IN_SEASON = "IN_SEASON_MONTH_%02d";
	private static final String BEFORE_SEASON = "BEFORE_SEASON_START_MONTH_%02d";
	/** 규칙 코드 BEFORE_{행사} → supply_trigger_date 코드 */
	private static final Map<String, String> BEFORE_HOLIDAY = Map.of(
			"BEFORE_KIMJANG", "KIMJANG", "BEFORE_SEOLLAL", "SEOLLAL", "BEFORE_CHUSEOK", "CHUSEOK");
	private static final List<String> PRICE_RULE_CODES = Arrays.stream(PriceStage.values())
			.map(stage -> stage.ruleCode).filter(Objects::nonNull).toList();
	/** 1주 변동률이 이보다 작으면 FLAT (%) */
	private static final double FLAT_PERCENT = 1.0;

	private final IngredientRepository ingredientRepository;
	private final IngredientPriceHistoryRepository priceHistoryRepository;
	private final IngredientPriceBandRepository priceBandRepository;
	private final IngredientRuleRepository ruleRepository;
	private final IngredientRuleItemRepository ruleItemRepository;
	private final SupplyTriggerDateRepository triggerDateRepository;

	public IngredientEvidenceService(IngredientRepository ingredientRepository, IngredientPriceHistoryRepository priceHistoryRepository,
			IngredientPriceBandRepository priceBandRepository, IngredientRuleRepository ruleRepository,
			IngredientRuleItemRepository ruleItemRepository, SupplyTriggerDateRepository triggerDateRepository) {
		this.ingredientRepository = ingredientRepository;
		this.priceHistoryRepository = priceHistoryRepository;
		this.priceBandRepository = priceBandRepository;
		this.ruleRepository = ruleRepository;
		this.ruleItemRepository = ruleItemRepository;
		this.triggerDateRepository = triggerDateRepository;
	}

	@Override
	public IngredientEvidenceResult collect(StoreContext store) {
		List<Long> ids = store.ingredientIds();
		if (ids == null || ids.isEmpty()) return new IngredientEvidenceResult(List.of(), List.of());
		LocalDate baseDate = store.baseDate();

		Map<Long, Ingredient> ingredients = ingredientRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(Ingredient::getId, Function.identity()));
		Map<Long, IngredientPriceHistory> prices = priceHistoryRepository.findLatestByIngredientIds(ids, baseDate).stream()
				.collect(Collectors.toMap(IngredientPriceHistory::getIngredientId, Function.identity()));
		Map<String, IngredientPriceBand> bands = priceBandRepository.findAll().stream()
				.collect(Collectors.toMap(IngredientPriceBand::getCategoryName, Function.identity()));
		Map<String, IngredientRule> priceRules = ruleRepository.findByTriggerCodeIn(PRICE_RULE_CODES).stream()
				.collect(Collectors.toMap(IngredientRule::getTriggerCode, Function.identity()));
		Map<Long, List<Long>> linkedRuleIds = ruleItemRepository.findByIngredientIdIn(ids).stream()
				.collect(Collectors.groupingBy(IngredientRuleItem::getIngredientId,
						Collectors.mapping(IngredientRuleItem::getRuleId, Collectors.toList())));
		Map<Long, IngredientRule> rules = ruleRepository.findAllById(linkedRuleIds.values().stream().flatMap(List::stream).distinct().toList())
				.stream().collect(Collectors.toMap(IngredientRule::getId, Function.identity()));
		Set<String> weekRules = weekRuleCodes(baseDate, triggerDateRepository.findByTriggerCodeIn(BEFORE_HOLIDAY.values()), rules.values());

		List<IngredientEvidence> items = new ArrayList<>();
		for (Long id : ids) {
			Ingredient ingredient = ingredients.get(id);
			if (ingredient == null) continue;   // 없는 id는 이름도 없어 근거로 못 쓴다. 요청 검증은 분석 서비스 몫
			IngredientPriceResponse price = price("R" + (items.size() + 1), ingredient, prices.get(id), bands.get(ingredient.getCategoryName()));

			List<IngredientSignal> signals = new ArrayList<>();
			// 가격 단계 규칙은 연결(일배치가 최근 조사일로 다시 만든다) 대신 위에서 계산한 단계로 고른다. 화면의 priceStage와 어긋나지 않게
			IngredientRule priceRule = price.priceStage() == null || price.priceStage().ruleCode == null ? null
					: priceRules.get(price.priceStage().ruleCode);
			if (priceRule != null) signals.add(signal(priceRule));
			linkedRuleIds.getOrDefault(id, List.of()).stream().map(rules::get)
					.filter(rule -> weekRules.contains(rule.getTriggerCode()))
					.sorted(Comparator.comparing(IngredientRule::getTriggerCode))
					.forEach(rule -> signals.add(signal(rule)));
			items.add(new IngredientEvidence(price, signals));
		}
		if (items.isEmpty()) return new IngredientEvidenceResult(List.of(), List.of());

		LocalDate priceDate = items.stream().map(item -> item.price().baseDate()).filter(Objects::nonNull)
				.max(Comparator.naturalOrder()).orElse(null);
		SourceResponse source = new SourceResponse("농산물 가격 추이", "한국농수산식품유통공사", "매일 갱신", priceDate, store.baseTime(),
				"https://www.data.go.kr/data/15156069/openapi.do");
		return new IngredientEvidenceResult(items, List.of(source));
	}

	/** 기준일에 걸리는 가격 외 규칙 코드: 이번 달 제철, 다음 달 제철 시작 한 달 전, 행사 시작 lead_days_max~min일 전 안이면 명절 전. */
	static Set<String> weekRuleCodes(LocalDate baseDate, List<SupplyTriggerDate> holidayDates, Iterable<IngredientRule> rules) {
		Set<String> codes = new HashSet<>();
		codes.add(IN_SEASON.formatted(baseDate.getMonthValue()));
		codes.add(BEFORE_SEASON.formatted(baseDate.plusMonths(1).getMonthValue()));
		for (IngredientRule rule : rules) {
			String holiday = BEFORE_HOLIDAY.get(rule.getTriggerCode());
			if (holiday == null || rule.getLeadDaysMin() == null || rule.getLeadDaysMax() == null) continue;
			boolean hit = holidayDates.stream().filter(d -> d.getTriggerCode().equals(holiday)).anyMatch(d ->
					!baseDate.isBefore(d.getStartDate().minusDays(rule.getLeadDaysMax()))
							&& !baseDate.isAfter(d.getStartDate().minusDays(rule.getLeadDaysMin())));
			if (hit) codes.add(rule.getTriggerCode());
		}
		return codes;
	}

	/** 가격 자료가 없으면 가격 필드는 모두 null. 1·4주 전 가격이 없으면 그 비교값과 그에 기대는 흐름·단계만 null. */
	static IngredientPriceResponse price(String evidenceId, Ingredient ingredient, IngredientPriceHistory history, IngredientPriceBand band) {
		String unit = ingredient.getUnitSize() == null ? ingredient.getUnit()
				: ingredient.getUnitSize().stripTrailingZeros().toPlainString() + ingredient.getUnit();
		if (history == null || history.getPrice() == null) {
			return new IngredientPriceResponse(evidenceId, ingredient.getId(), ingredient.getItemName(), ingredient.getGradeName(), unit,
					null, null, null, null, null, null, null, null);
		}
		BigDecimal price = history.getPrice();
		List<BigDecimal> weeks = Arrays.asList(history.getFourWeeksBeforePrice(), history.getThreeWeeksBeforePrice(),
				history.getTwoWeeksBeforePrice(), history.getOneWeekBeforePrice(), price);
		Trend trend = trend(history.getOneWeekBeforePrice(), price);
		Integer consecutiveWeeks = null;
		if (trend != null) {
			int count = 0;
			// 기준일부터 거꾸로, 같은 흐름인 주를 센다. FLAT이면 0
			for (int i = weeks.size() - 1; trend != Trend.FLAT && i > 0 && trend(weeks.get(i - 1), weeks.get(i)) == trend; i--) count++;
			consecutiveWeeks = count;
		}
		Double fourWeekChange = history.fourWeekChange();
		PriceStage stage = fourWeekChange == null || band == null ? null : band.stage(fourWeekChange);
		return new IngredientPriceResponse(evidenceId, ingredient.getId(), ingredient.getItemName(), ingredient.getGradeName(), unit,
				history.getObservedDate(), won(price), weeks.stream().map(IngredientEvidenceService::won).toList(),
				change(history.getOneWeekBeforePrice(), price), change(history.getFourWeeksBeforePrice(), price), trend, consecutiveWeeks, stage);
	}

	private static PriceChange change(BigDecimal before, BigDecimal now) {
		Double percent = percent(before, now);
		return percent == null ? null : new PriceChange(won(now.subtract(before)), percent);
	}

	/** 화면에 보이는 소수 첫째 자리 변동률로 정한다("−1.0%"인데 FLAT이 되지 않게). ±1% 미만이면 FLAT, 비교할 가격이 없으면 null. */
	private static Trend trend(BigDecimal before, BigDecimal now) {
		Double percent = percent(before, now);
		if (percent == null) return null;
		if (Math.abs(percent) < FLAT_PERCENT) return Trend.FLAT;
		return percent > 0 ? Trend.UP : Trend.DOWN;
	}

	/** 변동률(%), 소수 첫째 자리 */
	private static Double percent(BigDecimal before, BigDecimal now) {
		if (before == null || now == null || before.signum() <= 0) return null;
		return Math.round((now.doubleValue() / before.doubleValue() - 1) * 1000) / 10.0;
	}

	private static Long won(BigDecimal value) {
		return value == null ? null : value.setScale(0, RoundingMode.HALF_UP).longValueExact();
	}

	private static IngredientSignal signal(IngredientRule rule) {
		return new IngredientSignal(rule.getTriggerCode(), rule.getTriggerText(), rule.getDemand());
	}
}
