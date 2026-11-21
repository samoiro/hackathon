package com.hackathon.server.industry.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 업종별 추천 원자재 (업종당 최대 3개). 이름은 원본과 외래키로 동기화된다(V23). */
@Entity
@Table(name = "industry_ingredient")
@IdClass(IndustryIngredient.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndustryIngredient {

	@Id
	private Long industryId;
	@Id
	private Long ingredientId;

	private Short sortOrder;
	private String industryName;
	private String ingredientName;

	public record Key(Long industryId, Long ingredientId) implements Serializable {
	}
}
