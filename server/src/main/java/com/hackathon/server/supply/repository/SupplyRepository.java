package com.hackathon.server.supply.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hackathon.server.supply.entity.Supply;

public interface SupplyRepository extends JpaRepository<Supply, Long> {

	/** 업종 규칙 중 고른 물품이 든 것. 물품 하나가 규칙 여러 개에 들면 여러 줄이다. */
	@Query(nativeQuery = true, value = """
			select i.supply_id as "supplyId", i.kind as "kind", r.trigger_code as "triggerCode", t.label as "triggerLabel",
			    r.forecast as "forecast", r.lead_days_max as "leadDaysMax", r.demand as "demand"
			from supply_rule r
			join supply_rule_item i on i.rule_id = r.id
			join supply_trigger t on t.code = r.trigger_code
			where r.industry_id = :industryId and i.supply_id in :supplyIds""")
	List<RuleRow> findRules(long industryId, Collection<Long> supplyIds);

	interface RuleRow {
		Long getSupplyId();

		String getKind();

		String getTriggerCode();

		String getTriggerLabel();

		Boolean getForecast();

		Integer getLeadDaysMax();

		String getDemand();
	}
}
