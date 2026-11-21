package com.hackathon.server.industry.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 업종. 데이터는 V2__supply_seed.sql */
@Entity
@Table(name = "industry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Industry {

	@Id
	private Long id;

	private String category;
	private String name;
}
