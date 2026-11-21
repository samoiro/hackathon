package com.hackathon.server.ingredient.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부류별 4주 변동률 분위수 경계. 값 = 조사일 가격 ÷ 4주 전 평균 가격 − 1.
 * 기준: docs/database-schema.md ingredient_price_band
 */
@Entity
@Table(name = "ingredient_price_band")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientPriceBand {

	@Id
	private String categoryName;
	private Integer sampleCount;
	private BigDecimal p5;
	@Column(name = "p12_5")
	private BigDecimal p12_5;
	private BigDecimal p20;
	private BigDecimal p80;
	@Column(name = "p87_5")
	private BigDecimal p87_5;
	private BigDecimal p95;
	private String samplePeriod;

	IngredientPriceBand(String categoryName, double p5, double p12_5, double p20, double p80, double p87_5, double p95) {
		this.categoryName = categoryName;
		this.p5 = BigDecimal.valueOf(p5);
		this.p12_5 = BigDecimal.valueOf(p12_5);
		this.p20 = BigDecimal.valueOf(p20);
		this.p80 = BigDecimal.valueOf(p80);
		this.p87_5 = BigDecimal.valueOf(p87_5);
		this.p95 = BigDecimal.valueOf(p95);
	}

	/** 경계값과 같으면 그 단계에 넣는다(상위 5%는 p95 이상, 하위 5%는 p5 이하). */
	public PriceStage stage(double fourWeekChange) {
		if (fourWeekChange >= p95.doubleValue()) return PriceStage.SEVERE_HIGH;
		if (fourWeekChange >= p87_5.doubleValue()) return PriceStage.ALERT_HIGH;
		if (fourWeekChange >= p80.doubleValue()) return PriceStage.WATCH_HIGH;
		if (fourWeekChange <= p5.doubleValue()) return PriceStage.SEVERE_LOW;
		if (fourWeekChange <= p12_5.doubleValue()) return PriceStage.ALERT_LOW;
		if (fourWeekChange <= p20.doubleValue()) return PriceStage.WATCH_LOW;
		return PriceStage.STABLE;
	}
}
