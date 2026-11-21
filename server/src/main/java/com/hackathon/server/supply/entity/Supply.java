package com.hackathon.server.supply.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 부자재·소모품과 판매 상품. 둘의 구분은 규칙 연결(supply_rule_item.kind)에 있다. */
@Entity
@Table(name = "supply")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Supply {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
}
