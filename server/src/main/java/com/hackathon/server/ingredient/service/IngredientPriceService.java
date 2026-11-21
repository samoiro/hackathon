package com.hackathon.server.ingredient.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import com.hackathon.server.batch.dto.BatchCounts;
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
import com.hackathon.server.publicdata.client.AgroPriceClient;
import com.hackathon.server.publicdata.dto.ListResponse;

/**
 * aT 가격 추이 API로 원자재 가격을 받아 저장하고, 가장 최근 조사일의 4주 변동률을 부류별 분위수로 판정해
 * 4주 가격 규칙 연결(ingredient_rule_item)을 다시 만든다. 기준: docs/database-schema.md 가격 일배치
 */
@Service
public class IngredientPriceService {

	private static final DateTimeFormatter API_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int PAGE_SIZE = 1000;
	// shortcut: 마지막 조사일 다음 날부터 오늘까지 받되 최대 7일. 오래 멈췄다면 그 이전은 수동 적재
	static final int MAX_BACKFILL_DAYS = 7;
	private static final List<String> PRICE_RULE_CODES = Arrays.stream(PriceStage.values())
			.map(stage -> stage.ruleCode).filter(Objects::nonNull).toList();

	private final AgroPriceClient agroPriceClient;
	private final IngredientRepository ingredientRepository;
	private final IngredientPriceHistoryRepository priceHistoryRepository;
	private final IngredientPriceBandRepository priceBandRepository;
	private final IngredientRuleRepository ruleRepository;
	private final IngredientRuleItemRepository ruleItemRepository;

	public IngredientPriceService(AgroPriceClient agroPriceClient, IngredientRepository ingredientRepository,
			IngredientPriceHistoryRepository priceHistoryRepository, IngredientPriceBandRepository priceBandRepository,
			IngredientRuleRepository ruleRepository, IngredientRuleItemRepository ruleItemRepository) {
		this.agroPriceClient = agroPriceClient;
		this.ingredientRepository = ingredientRepository;
		this.priceHistoryRepository = priceHistoryRepository;
		this.priceBandRepository = priceBandRepository;
		this.ruleRepository = ruleRepository;
		this.ruleItemRepository = ruleItemRepository;
	}

	/** 마지막 조사일 다음 날부터 today까지 API 가격 중 대표 규격이 같은 행만 돌려준다. API 호출이라 트랜잭션 밖에서 부른다. */
	public List<IngredientPriceHistory> fetchPrices(LocalDate today) {
		Map<String, Long> ingredientIds = ingredientRepository.findAll().stream().collect(Collectors.toMap(
				i -> specKey(i.getItemCode(), i.getTradeTypeCode(), i.getVarietyCode(), i.getGradeCode(), i.getUnit(), i.getUnitSize()),
				Ingredient::getId));
		LocalDate earliest = today.minusDays(MAX_BACKFILL_DAYS);
		LocalDate from = priceHistoryRepository.findLatestObservedDate()
				.map(last -> last.plusDays(1)).filter(d -> d.isAfter(earliest)).orElse(earliest);

		List<IngredientPriceHistory> prices = new ArrayList<>();
		for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
			for (int page = 1; ; page++) {
				ListResponse response = agroPriceClient.priceTrends(date.format(API_DATE), Map.of(), page, PAGE_SIZE);
				for (JsonNode row : response.items()) {
					Long ingredientId = ingredientIds.get(specKey(text(row, "item_cd"), text(row, "se_cd"), text(row, "vrty_cd"),
							text(row, "grd_cd"), text(row, "unit"), number(row, "unit_sz")));
					if (ingredientId != null) {
						prices.add(new IngredientPriceHistory(ingredientId, date, number(row, "exmn_dd_avg_prc"),
								number(row, "ww1_bfr_avg_prc"), number(row, "ww2_bfr_avg_prc"), number(row, "ww3_bfr_avg_prc"),
								number(row, "ww4_bfr_avg_prc")));
					}
				}
				if ((long) page * PAGE_SIZE >= response.totalCount()) break;
			}
		}
		return prices;
	}

	/** 가격을 upsert하고 직전 관측 가격을 다시 계산한 뒤 규칙 연결을 다시 만든다. read = 저장한 가격, write = 만든 연결, skip = 판정 못 한 원자재. */
	@Transactional
	public BatchCounts saveAndRemap(List<IngredientPriceHistory> prices) {
		priceHistoryRepository.saveAll(prices);
		priceHistoryRepository.flush();
		priceHistoryRepository.refreshPreviousPrices();
		long ingredients = ingredientRepository.count();
		List<IngredientRuleItem> links = remapPriceRules();
		long judged = priceHistoryRepository.findLatestObservedDate().map(priceHistoryRepository::findByObservedDate)
				.orElse(List.of()).stream().filter(h -> h.fourWeekChange() != null).count();
		return new BatchCounts(prices.size(), links.size(), ingredients - judged);
	}

	/** 가장 최근 조사일에 가격과 4주 전 가격이 모두 있는 원자재만 판정한다. 안정·미조사 원자재는 연결하지 않는다. */
	private List<IngredientRuleItem> remapPriceRules() {
		Map<String, Long> ruleIds = ruleRepository.findByTriggerCodeIn(PRICE_RULE_CODES).stream()
				.collect(Collectors.toMap(IngredientRule::getTriggerCode, IngredientRule::getId));
		if (ruleIds.size() != PRICE_RULE_CODES.size()) {
			throw new IllegalStateException("4주 가격 규칙이 ingredient_rule에 모두 있어야 합니다: " + PRICE_RULE_CODES);
		}
		Map<Long, String> categories = ingredientRepository.findAll().stream()
				.collect(Collectors.toMap(Ingredient::getId, Ingredient::getCategoryName));
		Map<String, IngredientPriceBand> bands = priceBandRepository.findAll().stream()
				.collect(Collectors.toMap(IngredientPriceBand::getCategoryName, Function.identity()));

		List<IngredientRuleItem> links = new ArrayList<>();
		LocalDate latest = priceHistoryRepository.findLatestObservedDate().orElse(null);
		for (IngredientPriceHistory history : latest == null ? List.<IngredientPriceHistory>of() : priceHistoryRepository.findByObservedDate(latest)) {
			Double change = history.fourWeekChange();
			IngredientPriceBand band = bands.get(categories.get(history.getIngredientId()));
			if (change == null || band == null) continue;
			String ruleCode = band.stage(change).ruleCode;
			if (ruleCode != null) links.add(new IngredientRuleItem(ruleIds.get(ruleCode), history.getIngredientId()));
		}
		ruleItemRepository.deleteByRuleIds(ruleIds.values());
		return ruleItemRepository.saveAll(links);
	}

	static String specKey(String item, String tradeType, String variety, String grade, String unit, BigDecimal unitSize) {
		return String.join("|", item, tradeType, variety, grade, unit, unitSize == null ? "" : unitSize.stripTrailingZeros().toPlainString());
	}

	private static String text(JsonNode row, String field) {
		return row.path(field).asString("").trim();
	}

	/** API 숫자는 문자열이다. 빈 값·비숫자·음수는 미제공(null)으로 둔다. */
	private static BigDecimal number(JsonNode row, String field) {
		try {
			BigDecimal value = new BigDecimal(text(row, field).replace(",", ""));
			return value.signum() >= 0 ? value : null;
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
}
