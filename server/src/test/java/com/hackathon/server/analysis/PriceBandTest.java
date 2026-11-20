package com.hackathon.server.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 경계값은 V26 채소류 행. */
class PriceBandTest {

	private final PriceBand normal = new PriceBand(PriceBandBasis.NORMAL_RATIO, 0.8958, 0.9523, 0.9768, 1.2863, 1.4018, 1.6763);
	private final PriceBand weekly = new PriceBand(PriceBandBasis.WEEKLY_CHANGE, -0.1213, -0.0722, -0.0461, 0.0428, 0.0728, 0.1375);

	@Test
	void normalRatioFollowsSevenStages() {
		assertThat(normal.stageOf(1.70)).isEqualTo(PriceStage.SEVERE_HIGH);
		assertThat(normal.stageOf(1.4018)).isEqualTo(PriceStage.ALERT_HIGH);
		assertThat(normal.stageOf(1.30)).isEqualTo(PriceStage.WATCH_HIGH);
		assertThat(normal.stageOf(1.10)).isEqualTo(PriceStage.STABLE);
		assertThat(normal.stageOf(0.97)).isEqualTo(PriceStage.WATCH_LOW);
		assertThat(normal.stageOf(0.93)).isEqualTo(PriceStage.ALERT_LOW);
		assertThat(normal.stageOf(0.89)).isEqualTo(PriceStage.SEVERE_LOW);
	}

	@Test
	void eightyPercentOfNormalIsAlwaysSevereLow() {
		var wideBand = new PriceBand(PriceBandBasis.NORMAL_RATIO, 0.70, 0.75, 0.85, 1.2, 1.3, 1.4);
		assertThat(wideBand.stageOf(0.79)).isEqualTo(PriceStage.SEVERE_LOW);
		assertThat(wideBand.stageOf(0.82)).isEqualTo(PriceStage.WATCH_LOW);
	}

	@Test
	void weeklyChangeUnderOnePercentIsStable() {
		var grainBand = new PriceBand(PriceBandBasis.WEEKLY_CHANGE, -0.0383, -0.0158, -0.0072, 0.0086, 0.0167, 0.0411);
		assertThat(grainBand.stageOf(0.009)).isEqualTo(PriceStage.STABLE);
		assertThat(grainBand.stageOf(0.011)).isEqualTo(PriceStage.WATCH_HIGH);
	}

	@Test
	void ruleCodeMatchesV26TriggerCodes() {
		assertThat(normal.stageOf(1.70).ruleCode(PriceBandBasis.NORMAL_RATIO)).isEqualTo("PRICE_ABOVE_NORMAL_SEVERE");
		assertThat(normal.stageOf(0.97).ruleCode(PriceBandBasis.NORMAL_RATIO)).isEqualTo("PRICE_BELOW_NORMAL_WATCH");
		assertThat(weekly.stageOf(0.20).ruleCode(PriceBandBasis.WEEKLY_CHANGE)).isEqualTo("PRICE_RISE_ONE_WEEK_ALERT");
		assertThat(weekly.stageOf(-0.05).ruleCode(PriceBandBasis.WEEKLY_CHANGE)).isEqualTo("PRICE_DROP_ONE_WEEK_WATCH");
		assertThat(PriceStage.STABLE.ruleCode(PriceBandBasis.NORMAL_RATIO)).isNull();
	}
}
