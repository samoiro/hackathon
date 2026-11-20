package com.hackathon.server.analysis;

/**
 * 가격 단계. 정부 농산물 수급조절매뉴얼의 7단계(상승 심각·경계·주의 / 안정 / 하락 주의·경계·심각)를 따른다.
 * 경계값은 ingredient_price_band의 부류별 분위수. 기준: docs/database-schema.md ingredient_price_band
 */
public enum PriceStage {
	/** 상승 심각: 상위 5% */
	SEVERE_HIGH,
	/** 상승 경계: 상위 12.5% */
	ALERT_HIGH,
	/** 상승 주의: 상위 20% */
	WATCH_HIGH,
	STABLE,
	/** 하락 주의: 하위 20% */
	WATCH_LOW,
	/** 하락 경계: 하위 12.5% */
	ALERT_LOW,
	/** 하락 심각: 하위 5%, 평년 비율이면 평년의 80% 이하도 */
	SEVERE_LOW;

	/** 이 단계가 거는 ingredient_rule.trigger_code. 안정이면 null. 1주 변동률은 심각을 경계와 같은 규칙으로 본다. */
	public String ruleCode(PriceBandBasis basis) {
		if (this == STABLE) {
			return null;
		}
		boolean high = ordinal() < STABLE.ordinal();
		if (basis == PriceBandBasis.NORMAL_RATIO) {
			String level = switch (this) {
				case SEVERE_HIGH, SEVERE_LOW -> "SEVERE";
				case ALERT_HIGH, ALERT_LOW -> "ALERT";
				default -> "WATCH";
			};
			return (high ? "PRICE_ABOVE_NORMAL_" : "PRICE_BELOW_NORMAL_") + level;
		}
		String level = (this == WATCH_HIGH || this == WATCH_LOW) ? "WATCH" : "ALERT";
		return (high ? "PRICE_RISE_ONE_WEEK_" : "PRICE_DROP_ONE_WEEK_") + level;
	}
}
