package com.hackathon.server.ingredient.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 품목별 대표 원자재. 가격은 trade_type·variety·grade·unit·unit_size 규격이 같은 API 행만 연결한다. */
@Entity
@Table(name = "ingredient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String itemCode;
	private String itemName;
	private String categoryCode;
	private String categoryName;
	private String tradeTypeCode;
	private String tradeTypeName;
	private String varietyCode;
	private String varietyName;
	private String gradeCode;
	private String gradeName;
	private String unit;
	private BigDecimal unitSize;
}
