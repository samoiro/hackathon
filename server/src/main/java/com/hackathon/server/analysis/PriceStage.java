package com.hackathon.server.analysis;

/**
 * 가격 단계. 정부 농산물 수급조절매뉴얼의 7단계(상승 심각·경계·주의 / 안정 / 하락 주의·경계·심각)를 따른다.
 * 경계값은 ingredient_price_band의 부류별 4주 변동률 분위수. 기준: docs/database-schema.md ingredient_price_band
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
	/** 하락 심각: 하위 5% */
	SEVERE_LOW;

	/** 이 단계가 거는 ingredient_rule.trigger_code. 안정이면 null. */
	public String ruleCode() {
		return switch (this) {
			case SEVERE_HIGH -> "PRICE_RISE_FOUR_WEEKS_SEVERE";
			case ALERT_HIGH -> "PRICE_RISE_FOUR_WEEKS_ALERT";
			case WATCH_HIGH -> "PRICE_RISE_FOUR_WEEKS_WATCH";
			case STABLE -> null;
			case WATCH_LOW -> "PRICE_DROP_FOUR_WEEKS_WATCH";
			case ALERT_LOW -> "PRICE_DROP_FOUR_WEEKS_ALERT";
			case SEVERE_LOW -> "PRICE_DROP_FOUR_WEEKS_SEVERE";
		};
	}
}
