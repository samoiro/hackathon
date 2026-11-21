package com.hackathon.server.ingredient.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 규칙과 원자재의 다대다 연결. */
@Entity
@Table(name = "ingredient_rule_item")
@IdClass(IngredientRuleItem.Key.class)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientRuleItem {

	@Id
	private Long ruleId;
	@Id
	private Long ingredientId;

	public record Key(Long ruleId, Long ingredientId) implements Serializable {
	}
}
