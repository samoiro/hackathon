package com.hackathon.server.supply.entity;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기념일·학사·시즌 트리거의 연도별 기간. 기준: docs/database-schema.md supply_trigger_date (V19) */
@Entity
@Table(name = "supply_trigger_date")
@IdClass(SupplyTriggerDate.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplyTriggerDate {

	@Id
	private String triggerCode;
	@Id
	private LocalDate startDate;
	private LocalDate endDate;

	public record Key(String triggerCode, LocalDate startDate) implements Serializable {
	}
}
