package com.hackathon.server.analysis;

/** ingredient_price_band 한 행: 부류별 분위수 경계. 부류 행이 없으면 '전체' 행을 쓴다. */
public record PriceBand(PriceBandBasis basis, double p5, double p12_5, double p20, double p80, double p87_5, double p95) {

	/** 농산물가격안정제 기준가격: 평년 도매가격의 80% (2026.9 농식품부). 이 아래면 분위수와 관계없이 하락 심각 */
	static final double STABILIZATION_FLOOR = 0.8;

	/** 1주 변동이 이보다 작으면 안정 (Trend FLAT과 같은 ±1%) */
	static final double MIN_WEEKLY_CHANGE = 0.01;

	public PriceStage stageOf(double value) {
		if (basis == PriceBandBasis.WEEKLY_CHANGE && Math.abs(value) < MIN_WEEKLY_CHANGE) {
			return PriceStage.STABLE;
		}
		if (value >= p95) {
			return PriceStage.SEVERE_HIGH;
		}
		if (value >= p87_5) {
			return PriceStage.ALERT_HIGH;
		}
		if (value >= p80) {
			return PriceStage.WATCH_HIGH;
		}
		if (value <= p5 || (basis == PriceBandBasis.NORMAL_RATIO && value <= STABILIZATION_FLOOR)) {
			return PriceStage.SEVERE_LOW;
		}
		if (value <= p12_5) {
			return PriceStage.ALERT_LOW;
		}
		if (value <= p20) {
			return PriceStage.WATCH_LOW;
		}
		return PriceStage.STABLE;
	}
}
