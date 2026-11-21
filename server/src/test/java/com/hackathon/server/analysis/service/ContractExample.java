package com.hackathon.server.analysis.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.hackathon.server.analysis.dto.EventResponse;
import com.hackathon.server.analysis.dto.EventSearch;
import com.hackathon.server.analysis.dto.EventSource;
import com.hackathon.server.analysis.dto.EventStatus;
import com.hackathon.server.analysis.dto.EventTrigger;
import com.hackathon.server.analysis.dto.IngredientPriceResponse;
import com.hackathon.server.analysis.dto.OperationType;
import com.hackathon.server.analysis.dto.PriceChange;
import com.hackathon.server.analysis.dto.SupplyTimingResponse;
import com.hackathon.server.analysis.dto.Trend;
import com.hackathon.server.analysis.evidence.EventCandidate;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.IngredientEvidence;
import com.hackathon.server.analysis.evidence.IngredientEvidenceResult;
import com.hackathon.server.analysis.evidence.IngredientSignal;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.analysis.evidence.SupplyEvidence;
import com.hackathon.server.ingredient.entity.PriceStage;
import com.hackathon.server.supply.dto.Demand;

/** 계약 예시: 성수동 한식당(BOTH), 기준 2026-11-17(화) 07:00. 기준: ai-server/docs/SUGGESTION_TODOS_DRAFT.md "예시" */
final class ContractExample {

	static final UUID ANALYSIS_ID = UUID.fromString("6f1c2a4e-0d0b-4e7b-9a51-2b1f0c3d9e10");
	static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-11-17T07:00:00+09:00");
	static final LocalDate BASE_DATE = LocalDate.of(2026, 11, 17);

	static final EventCandidate E1 = new EventCandidate(new EventResponse("E1", "서울숲 음악회", "공연", EventTrigger.PERFORMANCE,
			"서울숲 야외무대", LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 21), "16:00–19:00", EventStatus.UPCOMING, 650, 10, true,
			37.5443, 127.0374, null), "AFTERNOON", "15~20시");

	static final IngredientEvidence R1 = new IngredientEvidence(new IngredientPriceResponse("R1", 12, "배추", "상품", "1포기",
			LocalDate.of(2026, 11, 16), 4380L, null, new PriceChange(172, 4.1), new PriceChange(430, 10.9), Trend.UP, 4,
			PriceStage.ALERT_HIGH),
			List.of(new IngredientSignal("PRICE_RISE_FOUR_WEEKS_ALERT", "4주 전보다 많이 오름 (상승 경계: 부류 상위 12.5%)", "STRONG_DOWN")));
	static final IngredientEvidence R2 = new IngredientEvidence(new IngredientPriceResponse("R2", 15, "돼지고기", "상품", "100g",
			LocalDate.of(2026, 11, 16), 2650L, null, new PriceChange(-32, -1.2), new PriceChange(-178, -6.3), Trend.DOWN, 2,
			PriceStage.WATCH_LOW),
			List.of(new IngredientSignal("PRICE_DROP_FOUR_WEEKS_WATCH", "4주 전보다 내림 (하락 주의: 부류 하위 20%)", "UP")));
	static final IngredientEvidence R3 = new IngredientEvidence(new IngredientPriceResponse("R3", 7, "쌀", "상품", "20kg",
			LocalDate.of(2026, 11, 16), 54200L, null, new PriceChange(0, 0.0), new PriceChange(0, 0.0), Trend.FLAT, 0,
			PriceStage.STABLE), List.of());

	static final SupplyEvidence S1 = new SupplyEvidence(new SupplyTimingResponse("S1", 3, "포장 용기", Demand.UP,
			LocalDate.of(2026, 11, 18), 3, "공연", "E1"), "SUPPLY", false);
	static final SupplyEvidence S2 = new SupplyEvidence(new SupplyTimingResponse("S2", 5, "일회용 수저", Demand.UP,
			LocalDate.of(2026, 11, 19), 2, "공연", "E1"), "SUPPLY", false);

	static StoreContext store() {
		return new StoreContext(ANALYSIS_ID, BASE_TIME, BASE_DATE, 37.5446, 127.0446, "성동구", "1120065000", 9, "음식점", "한식·국밥",
				OperationType.BOTH, List.of(12L, 15L, 7L), List.of(3L, 5L));
	}

	static EventEvidence events() {
		return new EventEvidence(new EventSearch(7, BASE_DATE, 1, EventSource.CULTURAL_EVENT), List.of(E1), List.of(), null, List.of());
	}

	static IngredientEvidenceResult ingredients() {
		return new IngredientEvidenceResult(List.of(R1, R2, R3), List.of());
	}

	static List<SupplyEvidence> supplies() {
		return List.of(S1, S2);
	}

	static BriefingPlanner planner(List<String> excludedKeys) {
		return new BriefingPlanner(store(), events(), ingredients(), supplies(), excludedKeys);
	}

	private ContractExample() {
	}
}
