package com.hackathon.server.ingredient.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class IngredientPriceBandTest {

	// V27 축산물 경계값
	private final IngredientPriceBand livestock = new IngredientPriceBand("축산물", -0.0820, -0.0488, -0.0274, 0.0343, 0.0541, 0.0865);

	@ParameterizedTest
	@CsvSource({
			"0.10, SEVERE_HIGH",
			"0.0865, SEVERE_HIGH",
			"0.0864, ALERT_HIGH",
			"0.0541, ALERT_HIGH",
			"0.0343, WATCH_HIGH",
			"0.0342, STABLE",
			"0, STABLE",
			"-0.0273, STABLE",
			"-0.0274, WATCH_LOW",
			"-0.0488, ALERT_LOW",
			"-0.0819, ALERT_LOW",
			"-0.0820, SEVERE_LOW",
			"-0.50, SEVERE_LOW" })
	void judgesStageByCategoryQuantiles(double fourWeekChange, PriceStage expected) {
		assertThat(livestock.stage(fourWeekChange)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "0.10, STABLE", "0.40, SEVERE_HIGH" })
	void sameChangeIsDifferentStageByCategory(double fourWeekChange, PriceStage expected) {
		// V27 채소류: 10% 상승은 상위 20% 경계(+13.08%) 아래라 안정
		var vegetables = new IngredientPriceBand("채소류", -0.2676, -0.1735, -0.1181, 0.1308, 0.2119, 0.3842);
		assertThat(vegetables.stage(fourWeekChange)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "SEVERE_HIGH, PRICE_RISE_FOUR_WEEKS_SEVERE", "WATCH_LOW, PRICE_DROP_FOUR_WEEKS_WATCH" })
	void stageMapsToRuleCode(PriceStage stage, String ruleCode) {
		assertThat(stage.ruleCode).isEqualTo(ruleCode);
	}
}
