package com.hackathon.server.supply.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.server.analysis.dto.SupplyTimingResponse;
import com.hackathon.server.analysis.dto.WeatherImpact;
import com.hackathon.server.analysis.dto.WeatherImpactStart;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.evidence.EventCandidate;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.analysis.evidence.SupplyEvidence;
import com.hackathon.server.analysis.evidence.SupplyEvidenceCollector;
import com.hackathon.server.supply.dto.Demand;
import com.hackathon.server.supply.entity.Supply;
import com.hackathon.server.supply.repository.SupplyRepository;

/**
 * 부자재 준비 시점 근거. 고른 물품마다 업종의 supply_rule 중 트리거가 행사·시즌·날씨 근거에 있고 준비 마감이 이번 주(기준일~6일 후)인
 * 규칙에서 마감이 가장 가까운 것 하나를 고른다. 마감 = 트리거 시작일 − lead_days_max, 이미 지났으면 기준일.
 */
@Service
@Transactional(readOnly = true)
public class SupplyEvidenceService implements SupplyEvidenceCollector {

	private static final int WEEK_DAYS = 7;
	/** WeatherImpact → supply_trigger.code. 강풍·기타 특보는 준비표 트리거가 없다 */
	private static final Map<WeatherImpact, String> WEATHER_TRIGGERS = Map.of(
			WeatherImpact.HEATWAVE, "HEATWAVE", WeatherImpact.COLD_WAVE, "COLD_WAVE", WeatherImpact.RAIN_SNOW, "RAIN",
			WeatherImpact.HEAVY_SNOW, "HEAVY_SNOW", WeatherImpact.FINE_DUST, "FINE_DUST");

	private final SupplyRepository supplyRepository;

	public SupplyEvidenceService(SupplyRepository supplyRepository) {
		this.supplyRepository = supplyRepository;
	}

	/** 근거에 있는 트리거. start는 트리거가 시작하는 날(진행 중이면 과거일 수 있다). */
	record Trigger(LocalDate start, String evidenceId) {
	}

	@Override
	public List<SupplyEvidence> collect(StoreContext store, EventEvidence events) {
		List<Long> ids = store.supplyIds();
		if (ids == null || ids.isEmpty()) return List.of();
		LocalDate baseDate = store.baseDate();
		Map<String, Trigger> triggers = triggers(baseDate, events);
		Map<Long, Supply> supplies = supplyRepository.findAllById(ids).stream().collect(Collectors.toMap(Supply::getId, Function.identity()));
		Map<Long, List<SupplyRepository.RuleRow>> rules = supplyRepository.findRules(store.industryId(), ids).stream()
				.collect(Collectors.groupingBy(SupplyRepository.RuleRow::getSupplyId));

		List<SupplyEvidence> result = new ArrayList<>();
		int evidenceNo = 1;
		for (Long id : ids) {
			Supply supply = supplies.get(id);
			if (supply == null) continue;   // 없는 id는 이름도 없어 근거로 못 쓴다. 요청 검증은 분석 서비스 몫
			SupplyRepository.RuleRow best = null;
			LocalDate bestDeadline = null;
			for (SupplyRepository.RuleRow rule : rules.getOrDefault(id, List.of())) {
				Trigger trigger = triggers.get(rule.getTriggerCode());
				if (trigger == null) continue;
				LocalDate deadline = trigger.start().minusDays(rule.getLeadDaysMax() == null ? 0 : rule.getLeadDaysMax());
				if (deadline.isAfter(baseDate.plusDays(WEEK_DAYS - 1))) continue;   // 준비는 다음 주 이후
				if (bestDeadline == null || deadline.isBefore(bestDeadline)) {
					best = rule;
					bestDeadline = deadline;
				}
			}
			if (best == null) {
				result.add(new SupplyEvidence(new SupplyTimingResponse(null, id, supply.getName(), null, null, null, null, null), null, false));
				continue;
			}
			LocalDate prepareBy = bestDeadline.isBefore(baseDate) ? baseDate : bestDeadline;
			result.add(new SupplyEvidence(new SupplyTimingResponse("S" + evidenceNo++, id, supply.getName(), Demand.valueOf(best.getDemand()),
					prepareBy, best.getLeadDaysMax(), best.getTriggerLabel(), triggers.get(best.getTriggerCode()).evidenceId()),
					best.getKind(), Boolean.TRUE.equals(best.getForecast())));
		}
		return result;
	}

	/** 트리거 코드 → 가장 먼저 시작하는 근거. 행사는 trigger, 시즌 행사는 code, 날씨는 impacts에서 찾는다. */
	static Map<String, Trigger> triggers(LocalDate baseDate, EventEvidence events) {
		Map<String, Trigger> triggers = new HashMap<>();
		if (events == null) return triggers;
		for (EventCandidate candidate : events.events() == null ? List.<EventCandidate>of() : events.events()) {
			if (candidate.event().trigger() == null) continue;
			triggers.merge(candidate.event().trigger().name(), new Trigger(candidate.event().startDate(), candidate.event().evidenceId()),
					SupplyEvidenceService::earlier);
		}
		if (events.seasonEvents() != null) {
			events.seasonEvents().forEach(season -> triggers.merge(season.code(), new Trigger(season.startDate(), season.evidenceId()),
					SupplyEvidenceService::earlier));
		}
		WeatherResponse weather = events.weather();
		if (weather != null && weather.impacts() != null) {
			Map<WeatherImpact, LocalDate> starts = weather.impactStarts() == null ? Map.of() : weather.impactStarts().stream()
					.collect(Collectors.toMap(WeatherImpactStart::impact, WeatherImpactStart::date, (a, b) -> a.isBefore(b) ? a : b));
			for (WeatherImpact impact : weather.impacts()) {
				String code = WEATHER_TRIGGERS.get(impact);
				// 시작일을 모르면 기준일로 본다: 준비가 늦어지는 쪽으로 틀리지 않게
				if (code != null) triggers.putIfAbsent(code, new Trigger(starts.getOrDefault(impact, baseDate), weather.evidenceId()));
			}
		}
		return triggers;
	}

	private static Trigger earlier(Trigger a, Trigger b) {
		return b.start().isBefore(a.start()) ? b : a;
	}
}
