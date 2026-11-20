package com.hackathon.server.analysis;

/** ingredient_price_band 한 행: 부류별 4주 변동률 분위수 경계. */
public record PriceBand(double p5, double p12_5, double p20, double p80, double p87_5, double p95) {

	/** fourWeekChange = 조사일 가격 ÷ 4주 전 평균 가격 − 1 (change4w.percent / 100) */
	public PriceStage stageOf(double fourWeekChange) {
		if (fourWeekChange >= p95) {
			return PriceStage.SEVERE_HIGH;
		}
		if (fourWeekChange >= p87_5) {
			return PriceStage.ALERT_HIGH;
		}
		if (fourWeekChange >= p80) {
			return PriceStage.WATCH_HIGH;
		}
		if (fourWeekChange <= p5) {
			return PriceStage.SEVERE_LOW;
		}
		if (fourWeekChange <= p12_5) {
			return PriceStage.ALERT_LOW;
		}
		if (fourWeekChange <= p20) {
			return PriceStage.WATCH_LOW;
		}
		return PriceStage.STABLE;
	}
}
