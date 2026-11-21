package com.hackathon.server.ingredient.entity;

/**
 * 가격 단계. 정부 농산물 수급조절매뉴얼의 7단계(상승 심각·경계·주의 / 안정 / 하락 주의·경계·심각)를 따른다.
 * 경계값은 ingredient_price_band의 부류별 4주 변동률 분위수. 기준: docs/database-schema.md ingredient_price_band
 */
public enum PriceStage {
	/** 상승 심각: 상위 5%, 규칙 PRICE_RISE_FOUR_WEEKS_SEVERE */
	SEVERE_HIGH("PRICE_RISE_FOUR_WEEKS_SEVERE"),
	/** 상승 경계: 상위 12.5%, 규칙 PRICE_RISE_FOUR_WEEKS_ALERT */
	ALERT_HIGH("PRICE_RISE_FOUR_WEEKS_ALERT"),
	/** 상승 주의: 상위 20%, 규칙 PRICE_RISE_FOUR_WEEKS_WATCH */
	WATCH_HIGH("PRICE_RISE_FOUR_WEEKS_WATCH"),
	/** 안정: 가격 규칙 없음 */
	STABLE(null),
	/** 하락 주의: 하위 20%, 규칙 PRICE_DROP_FOUR_WEEKS_WATCH */
	WATCH_LOW("PRICE_DROP_FOUR_WEEKS_WATCH"),
	/** 하락 경계: 하위 12.5%, 규칙 PRICE_DROP_FOUR_WEEKS_ALERT */
	ALERT_LOW("PRICE_DROP_FOUR_WEEKS_ALERT"),
	/** 하락 심각: 하위 5%, 규칙 PRICE_DROP_FOUR_WEEKS_SEVERE */
	SEVERE_LOW("PRICE_DROP_FOUR_WEEKS_SEVERE");

	/** ingredient_rule.trigger_code. 안정은 null */
	public final String ruleCode;

	PriceStage(String ruleCode) {
		this.ruleCode = ruleCode;
	}
}
