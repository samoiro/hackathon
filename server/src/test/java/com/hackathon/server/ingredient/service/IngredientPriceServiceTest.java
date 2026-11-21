package com.hackathon.server.ingredient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.batch.dto.BatchCounts;
import com.hackathon.server.ingredient.entity.Ingredient;
import com.hackathon.server.ingredient.entity.IngredientPriceHistory;
import com.hackathon.server.ingredient.entity.IngredientRule;
import com.hackathon.server.ingredient.entity.PriceStage;
import com.hackathon.server.ingredient.repository.IngredientPriceHistoryRepository;
import com.hackathon.server.ingredient.repository.IngredientRepository;
import com.hackathon.server.ingredient.repository.IngredientRuleItemRepository;
import com.hackathon.server.ingredient.repository.IngredientRuleRepository;
import com.hackathon.server.publicdata.client.AgroPriceClient;
import com.hackathon.server.publicdata.dto.ListResponse;

/** 마이그레이션이 넣은 원자재·부류 경계·규칙 위에서 가격 저장과 규칙 재연결을 실제 Postgres로 확인한다. */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, IngredientPriceService.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IngredientPriceServiceTest {

	// 마이그레이션 데이터보다 늦은 날이라 가장 최근 조사일이 된다
	private static final LocalDate DAY = LocalDate.of(2099, 1, 1);

	@Autowired
	IngredientPriceService service;
	@Autowired
	IngredientRepository ingredientRepository;
	@Autowired
	IngredientRuleRepository ruleRepository;
	@Autowired
	IngredientRuleItemRepository ruleItemRepository;
	@Autowired
	IngredientPriceHistoryRepository priceHistoryRepository;
	@MockitoBean
	AgroPriceClient agroPriceClient;

	private Ingredient ingredient(String name) {
		return ingredientRepository.findAll().stream().filter(i -> i.getItemName().equals(name)).findFirst().orElseThrow();
	}

	private List<String> priceRuleCodesOf(Ingredient ingredient) {
		var priceRules = ruleRepository.findAll().stream().filter(r -> r.getTriggerCode().matches("PRICE_.*FOUR_WEEKS_.*")).toList();
		return ruleItemRepository.findAll().stream().filter(i -> i.getIngredientId().equals(ingredient.getId()))
				.flatMap(i -> priceRules.stream().filter(r -> r.getId().equals(i.getRuleId())))
				.map(IngredientRule::getTriggerCode).toList();
	}

	private IngredientPriceHistory price(Ingredient ingredient, String price, String fourWeeksBefore) {
		return new IngredientPriceHistory(ingredient.getId(), DAY, new BigDecimal(price), null, null, null,
				fourWeeksBefore == null ? null : new BigDecimal(fourWeeksBefore));
	}

	@Test
	void linksOnlyJudgedStageAndKeepsOtherRules() {
		Ingredient apple = ingredient("사과");   // 과일류 상위 5% 경계 +18.14%
		Ingredient pear = ingredient("배");
		Ingredient oyster = ingredient("굴");
		long seasonalLinksBefore = ruleItemRepository.count() - 6 * ingredientRepository.count();

		BatchCounts counts = service.saveAndRemap(List.of(
				price(apple, "130", "100"),   // +30%: 상승 심각
				price(pear, "100", "100"),    // 0%: 안정이라 연결 없음
				price(oyster, "100", null))); // 4주 전 가격 없음: 판정 제외

		assertThat(priceRuleCodesOf(apple)).containsExactly(PriceStage.SEVERE_HIGH.ruleCode);
		assertThat(priceRuleCodesOf(pear)).isEmpty();
		assertThat(priceRuleCodesOf(oyster)).isEmpty();
		assertThat(ruleItemRepository.count()).isEqualTo(seasonalLinksBefore + 1);
		assertThat(counts).isEqualTo(new BatchCounts(3, 1, ingredientRepository.count() - 2));
	}

	@Test
	void fetchesOnlyRowsWithSameRepresentativeSpec() {
		Ingredient apple = ingredient("사과");
		String row = """
				{"item_cd":"%s","se_cd":"%s","vrty_cd":"%s","grd_cd":"%s","unit":"%s","unit_sz":"%s",
				 "exmn_dd_avg_prc":"1,300","ww1_bfr_avg_prc":"-","ww2_bfr_avg_prc":"","ww3_bfr_avg_prc":"1200","ww4_bfr_avg_prc":"1000"}""";
		String matching = row.formatted(apple.getItemCode(), apple.getTradeTypeCode(), apple.getVarietyCode(), apple.getGradeCode(),
				apple.getUnit(), apple.getUnitSize().toPlainString());
		String otherGrade = row.formatted(apple.getItemCode(), apple.getTradeTypeCode(), apple.getVarietyCode(), "99",
				apple.getUnit(), apple.getUnitSize().toPlainString());
		var json = JsonMapper.builder().build();
		when(agroPriceClient.priceTrends(anyString(), anyMap(), eq(1), eq(1000))).thenAnswer(call -> "20990101".equals(call.getArgument(0))
				? new ListResponse(2, json.readTree("[" + matching + "," + otherGrade + "]"))
				: new ListResponse(0, json.createArrayNode()));

		List<IngredientPriceHistory> prices = service.fetchPrices(DAY);

		assertThat(prices).singleElement().satisfies(p -> {
			assertThat(p.getIngredientId()).isEqualTo(apple.getId());
			assertThat(p.getObservedDate()).isEqualTo(DAY);
			assertThat(p.getPrice()).isEqualByComparingTo("1300");
			assertThat(p.getOneWeekBeforePrice()).isNull();
			assertThat(p.getTwoWeeksBeforePrice()).isNull();
			assertThat(p.getFourWeeksBeforePrice()).isEqualByComparingTo("1000");
		});
	}

	@Test
	void reloadKeepsOneRowPerIngredientAndDay() {
		Ingredient apple = ingredient("사과");
		service.saveAndRemap(List.of(price(apple, "130", "100")));
		service.saveAndRemap(List.of(price(apple, "90", "100")));   // 같은 날 정정: -10%는 과일류 하락 주의(-6.39%)~경계(-10.29%) 사이

		assertThat(priceHistoryRepository.findByObservedDate(DAY)).singleElement()
				.satisfies(p -> assertThat(p.getPrice()).isEqualByComparingTo("90"));
		assertThat(priceRuleCodesOf(apple)).containsExactly(PriceStage.WATCH_LOW.ruleCode);
	}
}
