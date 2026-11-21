package com.hackathon.server.supply.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.analysis.dto.EventResponse;
import com.hackathon.server.analysis.dto.EventStatus;
import com.hackathon.server.analysis.dto.EventTrigger;
import com.hackathon.server.analysis.dto.SeasonEventResponse;
import com.hackathon.server.analysis.dto.SupplyTimingResponse;
import com.hackathon.server.analysis.dto.WeatherImpact;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.evidence.EventCandidate;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.analysis.evidence.SupplyEvidence;
import com.hackathon.server.ingredient.service.IngredientEvidenceService;
import com.hackathon.server.supply.dto.Demand;
import com.hackathon.server.supply.dto.TriggerType;

/** V2·V24 준비표 규칙 위에서 행사·시즌·날씨 근거로 이번 주 걸린 규칙과 준비 기한을 실제 Postgres로 확인한다. */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, IngredientEvidenceService.class, SupplyEvidenceService.class })   // 원자재 테스트와 같은 context·컨테이너를 쓴다
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SupplyEvidenceServiceTest {

	private static final LocalDate BASE = LocalDate.of(2026, 11, 21);

	@Autowired
	SupplyEvidenceService service;
	@Autowired
	DataSource dataSource;

	private long id(String table, String name) {
		return new JdbcTemplate(dataSource).queryForObject("select id from " + table + " where name = ?", Long.class, name);
	}

	private List<SupplyEvidence> collect(String industry, List<String> supplies, EventEvidence events) {
		return service.collect(new StoreContext(null, OffsetDateTime.parse("2026-11-21T09:00:00+09:00"), BASE, 37.54, 127.05, "성동구",
				"1120011400", id("industry", industry), "음식점", industry, null, List.of(),
				supplies.stream().map(name -> id("supply", name)).toList()), events);
	}

	private static EventEvidence evidence(List<EventCandidate> events, List<SeasonEventResponse> seasons, WeatherResponse weather) {
		return new EventEvidence(null, events, seasons, weather, List.of());
	}

	private static EventCandidate event(String evidenceId, EventTrigger trigger, LocalDate start) {
		return new EventCandidate(new EventResponse(evidenceId, "행사", "공연", trigger, "서울숲", start, start.plusDays(1), null,
				EventStatus.UPCOMING, 650, 10, true, 37.54, 127.04, null), "UNKNOWN", null);
	}

	private static WeatherResponse weather(WeatherImpact... impacts) {
		return new WeatherResponse("W1", "1120011400", List.of(impacts), List.of(), List.of(), OffsetDateTime.parse("2026-11-21T08:00:00+09:00"));
	}

	@Test
	void weatherImpactsTurnOnForecastRules() {
		List<SupplyEvidence> result = collect("한식·국밥", List.of("국물 포장 용기", "배달 봉투"),
				evidence(List.of(), List.of(), weather(WeatherImpact.COLD_WAVE, WeatherImpact.RAIN_SNOW)));

		assertThat(result).extracting(SupplyEvidence::timing).containsExactly(
				new SupplyTimingResponse("S1", id("supply", "국물 포장 용기"), "국물 포장 용기", Demand.UP, BASE, 3, "한파", "W1"),
				new SupplyTimingResponse("S2", id("supply", "배달 봉투"), "배달 봉투", Demand.UP, BASE, 1, "비", "W1"));
		assertThat(result).extracting(SupplyEvidence::kind, SupplyEvidence::forecast)
				.containsOnly(tuple("SUPPLY", true));
	}

	@Test
	void noTriggerLeavesTimingEmpty() {
		// 강풍은 준비표 트리거가 없고, 설은 준비 마감(시작 21일 전)이 다음 주 이후다
		List<SupplyEvidence> result = collect("한식·국밥", List.of("국물 포장 용기", "명절음식 포장 용기"), evidence(List.of(),
				List.of(new SeasonEventResponse("H1", "SEOLLAL", "설", TriggerType.HOLIDAY, BASE.plusDays(28), BASE.plusDays(30))),
				weather(WeatherImpact.STRONG_WIND)));

		assertThat(result).extracting(SupplyEvidence::timing).containsExactly(
				new SupplyTimingResponse(null, id("supply", "국물 포장 용기"), "국물 포장 용기", null, null, null, null, null),
				new SupplyTimingResponse(null, id("supply", "명절음식 포장 용기"), "명절음식 포장 용기", null, null, null, null, null));
		assertThat(result).extracting(SupplyEvidence::kind).containsOnlyNulls();
	}

	@Test
	void seasonEventDeadlineInsideThisWeek() {
		List<SupplyEvidence> result = collect("한식·국밥", List.of("명절음식 포장 용기"), evidence(List.of(),
				List.of(new SeasonEventResponse("H1", "SEOLLAL", "설", TriggerType.HOLIDAY, BASE.plusDays(25), BASE.plusDays(27))), null));

		SupplyTimingResponse timing = result.getFirst().timing();
		assertThat(timing.prepareBy()).isEqualTo(BASE.plusDays(4));
		assertThat(timing.leadDays()).isEqualTo(21);
		assertThat(timing.triggerEvidenceId()).isEqualTo("H1");
		assertThat(result.getFirst().forecast()).isFalse();
	}

	@Test
	void picksClosestDeadlineAndClampsPastOnesToBaseDate() {
		// 카페 캐리어: 축제·공연 7일 전, 폭염 3일 전
		EventCandidate festival = event("E2", EventTrigger.FESTIVAL, BASE.plusDays(8));
		EventCandidate laterFestival = event("E3", EventTrigger.FESTIVAL, BASE.plusDays(12));

		SupplyTimingResponse upcoming = collect("카페", List.of("캐리어"), evidence(List.of(laterFestival, festival), List.of(), null))
				.getFirst().timing();
		assertThat(upcoming.prepareBy()).isEqualTo(BASE.plusDays(1));
		assertThat(upcoming.triggerLabel()).isEqualTo("축제");
		assertThat(upcoming.triggerEvidenceId()).isEqualTo("E2");

		// 이미 진행 중인 공연은 마감이 지나 기준일까지 준비하고, 축제보다 먼저 고른다
		SupplyTimingResponse ongoing = collect("카페", List.of("캐리어"),
				evidence(List.of(festival, event("E1", EventTrigger.PERFORMANCE, BASE.minusDays(3))), List.of(), null)).getFirst().timing();
		assertThat(ongoing.prepareBy()).isEqualTo(BASE);
		assertThat(ongoing.leadDays()).isEqualTo(7);
		assertThat(ongoing.triggerLabel()).isEqualTo("공연");
		assertThat(ongoing.triggerEvidenceId()).isEqualTo("E1");
	}
}
