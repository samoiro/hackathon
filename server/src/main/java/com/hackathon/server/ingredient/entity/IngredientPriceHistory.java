package com.hackathon.server.ingredient.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 원자재 조사일별 가격. 1~4주 전 가격은 조사일 API의 ww1~4_bfr_avg_prc 그대로다.
 * previous_day_price는 DB에서 LAG로 다시 계산하고, 변동률·흐름·created_datetime은 DB가 채워서 매핑하지 않는다.
 */
@Entity
@Table(name = "ingredient_price_history")
@IdClass(IngredientPriceHistory.Key.class)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientPriceHistory {

	@Id
	private Long ingredientId;
	@Id
	private LocalDate observedDate;
	private BigDecimal price;
	private BigDecimal oneWeekBeforePrice;
	private BigDecimal twoWeeksBeforePrice;
	private BigDecimal threeWeeksBeforePrice;
	private BigDecimal fourWeeksBeforePrice;

	/** 4주 변동률 = 가격 ÷ 4주 전 평균 가격 − 1. 둘 중 하나가 없거나 4주 전 가격이 0이면 null. */
	public Double fourWeekChange() {
		if (price == null || fourWeeksBeforePrice == null || fourWeeksBeforePrice.signum() <= 0) return null;
		return price.doubleValue() / fourWeeksBeforePrice.doubleValue() - 1;
	}

	public record Key(Long ingredientId, LocalDate observedDate) implements Serializable {
	}
}
