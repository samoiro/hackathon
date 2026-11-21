package com.hackathon.server.ingredient.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 원자재 추천 규칙. 이 배치는 4주 가격 규칙(PRICE_%FOUR_WEEKS_%)만 읽는다. */
@Entity
@Table(name = "ingredient_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String triggerCode;
	private String triggerText;
	private String demand;
	private Integer leadDaysMin;
	private Integer leadDaysMax;
}
