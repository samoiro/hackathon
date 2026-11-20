package com.hackathon.server.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 경계값은 V26 채소류 · 축산물 행. */
class PriceBandTest {

	private final PriceBand vegetables = new PriceBand(-0.2676, -0.1735, -0.1181, 0.1308, 0.2119, 0.3842);
	private final PriceBand livestock = new PriceBand(-0.0820, -0.0488, -0.0274, 0.0343, 0.0541, 0.0865);

	@Test
	void fourWeekChangeFollowsSevenStages() {
		assertThat(vegetables.stageOf(0.40)).isEqualTo(PriceStage.SEVERE_HIGH);
		assertThat(vegetables.stageOf(0.2119)).isEqualTo(PriceStage.ALERT_HIGH);
		assertThat(vegetables.stageOf(0.15)).isEqualTo(PriceStage.WATCH_HIGH);
		assertThat(vegetables.stageOf(0.0)).isEqualTo(PriceStage.STABLE);
		assertThat(vegetables.stageOf(-0.12)).isEqualTo(PriceStage.WATCH_LOW);
		assertThat(vegetables.stageOf(-0.20)).isEqualTo(PriceStage.ALERT_LOW);
		assertThat(vegetables.stageOf(-0.30)).isEqualTo(PriceStage.SEVERE_LOW);
	}

	@Test
	void sameChangeMeansDifferentStageByCategory() {
		// 4주 10% 상승은 채소엔 평소 수준이지만 축산물엔 상승 심각
		assertThat(vegetables.stageOf(0.10)).isEqualTo(PriceStage.STABLE);
		assertThat(livestock.stageOf(0.10)).isEqualTo(PriceStage.SEVERE_HIGH);
	}

	@Test
	void ruleCodeMatchesV26TriggerCodes() {
		assertThat(PriceStage.SEVERE_HIGH.ruleCode()).isEqualTo("PRICE_RISE_FOUR_WEEKS_SEVERE");
		assertThat(PriceStage.WATCH_LOW.ruleCode()).isEqualTo("PRICE_DROP_FOUR_WEEKS_WATCH");
		assertThat(PriceStage.STABLE.ruleCode()).isNull();
	}
}
