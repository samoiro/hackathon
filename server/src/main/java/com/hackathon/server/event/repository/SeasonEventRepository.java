package com.hackathon.server.event.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.hackathon.server.supply.dto.TriggerType;

/** 날짜로 정해지는 시즌 행사(supply_trigger의 HOLIDAY·SCHOOL·SEASON + supply_trigger_date, V19) 조회. 읽기만 한다. */
@Repository
public class SeasonEventRepository {

	public record SeasonEvent(String code, String label, TriggerType type, LocalDate startDate, LocalDate endDate) {
	}

	private final JdbcClient jdbc;

	public SeasonEventRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** 기준일에 진행 중이거나 days일 안에 시작하는 기간. 가까운 시작일 순. */
	public List<SeasonEvent> findActiveOrStartingWithin(LocalDate baseDate, int days) {
		return jdbc.sql("""
				select t.code, t.label, t.type, d.start_date, d.end_date
				from supply_trigger_date d
				join supply_trigger t on t.code = d.trigger_code
				where t.type in ('HOLIDAY', 'SCHOOL', 'SEASON')
				  and d.end_date >= :baseDate
				  and d.start_date <= cast(:baseDate as date) + :days
				order by d.start_date, t.code""")
				.param("baseDate", baseDate).param("days", days)
				.query(SeasonEvent.class).list();
	}
}
