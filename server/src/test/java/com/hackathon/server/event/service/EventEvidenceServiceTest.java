package com.hackathon.server.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.analysis.dto.EventSource;
import com.hackathon.server.analysis.dto.EventStatus;
import com.hackathon.server.analysis.dto.EventTrigger;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.evidence.EventCandidate;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.event.dto.CulturalEventRow;
import com.hackathon.server.event.repository.CulturalEventRepository;
import com.hackathon.server.event.repository.SeasonEventRepository;

/** 행사 후보 규칙(분류·기간·반경·회차·최대 5건)을 실제 PostGIS로 확인한다. 날씨는 경계라 mock. */
@DataJpaTest
@Import(TestcontainersConfiguration.class) // 마이그레이션 테스트와 같은 context를 써서 컨테이너를 더 띄우지 않는다
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventEvidenceServiceTest {

	// 성수동 시연 가게. 위도 0.001도 ≈ 111m
	private static final double LAT = 37.5446;
	private static final double LON = 127.0446;
	// 2026-11-23(월): 시즌 행사가 없는 날(V19)이 없어서 시즌은 따로 확인한다
	private static final LocalDate BASE = LocalDate.of(2026, 11, 23);

	@Autowired
	DataSource dataSource;

	private final WeatherEvidenceService weather = mock(WeatherEvidenceService.class);
	private JdbcClient jdbc;
	private CulturalEventRepository repository;
	private EventEvidenceService service;

	@BeforeEach
	void setUp() {
		jdbc = JdbcClient.create(dataSource);
		repository = new CulturalEventRepository(jdbc, new NamedParameterJdbcTemplate(dataSource));
		service = new EventEvidenceService(repository, new SeasonEventRepository(jdbc), weather);
	}

	private int key = 1;

	private void event(String title, String category, String place, double northMeters, LocalDate start, LocalDate end, String time) {
		repository.upsertAll(List.of(new CulturalEventRow(String.valueOf(key++), title, category, "성동구", place, start, end, time,
				LAT + northMeters / 111_000, LON, null, null, null, null, Boolean.TRUE,
				"https://culture.seoul.go.kr/culture/culture/cultureEvent/view.do?cultcode=" + key, null, "{}")));
	}

	private EventEvidence collect() {
		return service.collect(new StoreContext(UUID.randomUUID(), OffsetDateTime.parse("2026-11-23T07:00:00+09:00"), BASE,
				LAT, LON, "성동구", "1120065000", 1, "음식점", "한식", null, List.of(), List.of()));
	}

	private List<String> titles(EventEvidence evidence) {
		return evidence.events().stream().map(c -> c.event().title()).toList();
	}

	@Test
	void keepsOnlyCandidateCategoriesInDateWindowWithinOneKilometer() {
		event("공연", "콘서트", "서울숲 야외무대", 300, BASE.plusDays(3), BASE.plusDays(3), "16:00–19:00");
		event("진행 중 축제", "축제-문화/예술", "서울숲", 500, BASE.minusDays(2), BASE.plusDays(1), null);
		event("전시", "전시/미술", "갤러리", 100, BASE, BASE.plusDays(3), null);           // 후보 분류 아님
		event("다음 주 공연", "연극", "소극장", 200, BASE.plusDays(8), BASE.plusDays(8), null); // 7일 넘게 남음
		event("끝난 공연", "연극", "소극장", 200, BASE.minusDays(5), BASE.minusDays(1), null);
		event("먼 공연", "연극", "소극장", 1200, BASE.plusDays(1), BASE.plusDays(1), null);     // 1km 안에 다른 행사가 있으면 안 넓힘
		event("긴 축제 둘째 주", "축제-기타", "공원", 400, BASE.minusDays(7), BASE.plusDays(20), null); // 15일 이상, 시작 후 7일 지남
		event("긴 축제 첫 주", "축제-기타", "광장", 600, BASE.minusDays(6), BASE.plusDays(20), null);

		EventEvidence evidence = collect();

		assertThat(titles(evidence)).containsExactly("공연", "진행 중 축제", "긴 축제 첫 주");
		assertThat(evidence.search().source()).isEqualTo(EventSource.CULTURAL_EVENT);
		assertThat(evidence.search().count()).isEqualTo(3);
		EventCandidate first = evidence.events().getFirst();
		assertThat(first.event().evidenceId()).isEqualTo("E1");
		assertThat(first.event().trigger()).isEqualTo(EventTrigger.PERFORMANCE);
		assertThat(first.event().status()).isEqualTo(EventStatus.UPCOMING);
		assertThat(first.event().distanceM()).isBetween(295, 305);
		assertThat(first.event().walkMinutes()).isEqualTo(5);
		assertThat(first.timeBand()).isEqualTo("AFTERNOON");
		assertThat(first.peakLabel()).isEqualTo("15~20시");
		EventCandidate festival = evidence.events().get(1);
		assertThat(festival.event().trigger()).isEqualTo(EventTrigger.FESTIVAL);
		assertThat(festival.event().status()).isEqualTo(EventStatus.ONGOING);
		assertThat(festival.timeBand()).isEqualTo("UNKNOWN");
		assertThat(festival.peakLabel()).isNull();
	}

	@Test
	void widensToOneAndHalfKilometersOnlyWhenNothingWithinOne() {
		event("1.2km 공연", "국악", "국악당", 1200, BASE.plusDays(1), BASE.plusDays(1), null);
		event("2km 공연", "국악", "국악당2", 2000, BASE.plusDays(1), BASE.plusDays(1), null);

		assertThat(titles(collect())).containsExactly("1.2km 공연");
	}

	@Test
	void mergesSessionsOfSameTitleAndPlaceAndKeepsFiveNearest() {
		event("오페레타", "뮤지컬/오페라", "소월아트홀", 300, BASE.plusDays(4), BASE.plusDays(4), "11.27.(금) 19:30");
		event("오페레타", "뮤지컬/오페라", "소월아트홀", 300, BASE.plusDays(5), BASE.plusDays(5), "11.28.(토) 16:00");
		for (int i = 1; i <= 5; i++) {
			event("공연 " + i, "클래식", "홀 " + i, 300 + i * 100, BASE.plusDays(1), BASE.plusDays(1), null);
		}

		EventEvidence evidence = collect();

		assertThat(titles(evidence)).containsExactly("오페레타", "공연 1", "공연 2", "공연 3", "공연 4");
		assertThat(evidence.events().getFirst().event().startDate()).isEqualTo(BASE.plusDays(4));
		assertThat(evidence.events().getFirst().event().endDate()).isEqualTo(BASE.plusDays(5));
		assertThat(evidence.events()).extracting(c -> c.event().evidenceId()).containsExactly("E1", "E2", "E3", "E4", "E5");
	}

	@Test
	void fillsSeasonEventsAndWeatherEvenWhenEventsExist() {
		event("공연", "콘서트", "무대", 300, BASE.plusDays(1), BASE.plusDays(1), null);
		WeatherResponse w1 = new WeatherResponse("W1", "1120065000", List.of(), List.of(), List.of(), OffsetDateTime.parse("2026-11-23T07:00:00+09:00"));
		when(weather.collect(any())).thenReturn(w1);

		EventEvidence evidence = collect();

		// V19: 11-23 기준 진행 중인 김장철, 21일 안에 시작하는 겨울(12-01)
		assertThat(evidence.seasonEvents()).extracting(s -> s.code()).contains("KIMJANG", "WINTER").doesNotContain("CHRISTMAS");
		assertThat(evidence.seasonEvents().getFirst().evidenceId()).isEqualTo("H1");
		assertThat(evidence.weather()).isEqualTo(w1);
		assertThat(evidence.sources()).extracting(s -> s.provider()).contains("서울 열린데이터광장", "케이웨더");
	}

	@Test
	void fallsBackToSeasonThenNoneWhenNoEventAndNoWeather() {
		EventEvidence evidence = collect();

		assertThat(evidence.events()).isEmpty();
		assertThat(evidence.weather()).isNull();
		assertThat(evidence.search().source()).isEqualTo(EventSource.SEASON);
		jdbc.sql("delete from supply_trigger_date").update();
		assertThat(collect().search().source()).isEqualTo(EventSource.NONE);
	}

	@Test
	void upsertKeepsIdAndDeactivatesMissingEvents() {
		event("공연", "콘서트", "무대", 300, BASE.plusDays(1), BASE.plusDays(1), null);
		event("사라질 공연", "콘서트", "무대2", 300, BASE.plusDays(1), BASE.plusDays(1), null);
		long id = jdbc.sql("select id from cultural_event where source_key = '1'").query(Long.class).single();

		repository.upsertAll(List.of(new CulturalEventRow("1", "제목 바뀐 공연", "콘서트", "성동구", "무대", BASE.plusDays(1), BASE.plusDays(1),
				null, LAT, LON, null, null, null, null, null, "https://example.test", null, "{}")));
		repository.deactivateExcept(List.of("1"));

		assertThat(jdbc.sql("select id from cultural_event where title = '제목 바뀐 공연'").query(Long.class).single()).isEqualTo(id);
		assertThat(jdbc.sql("select is_active from cultural_event where source_key = '2'").query(Boolean.class).single()).isFalse();
		assertThat(titles(collect())).containsExactly("제목 바뀐 공연");
	}
}
