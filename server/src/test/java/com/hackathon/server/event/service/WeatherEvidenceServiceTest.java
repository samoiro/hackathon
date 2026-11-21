package com.hackathon.server.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.analysis.dto.DayPart;
import com.hackathon.server.analysis.dto.WeatherForecast;
import com.hackathon.server.analysis.dto.WeatherImpact;
import com.hackathon.server.analysis.dto.WeatherImpactStart;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.publicdata.client.DataGoKrClient;
import com.hackathon.server.publicdata.dto.ListResponse;

/** 기상청 단기예보 응답(response.body.items.item, 시간별 category 한 줄씩)을 오전·오후 칸으로 묶는다. */
class WeatherEvidenceServiceTest {

	private static final String PATH = "/1360000/VilageFcstInfoService_2.0/getVilageFcst";
	private static final JsonMapper JSON = JsonMapper.builder().build();
	private final DataGoKrClient dataGoKr = mock(DataGoKrClient.class);
	private final WeatherEvidenceService service = new WeatherEvidenceService(dataGoKr);

	/** 성수동, 11-21 10:00 기준이면 08시 발표를 쓴다 */
	private static StoreContext store() {
		return new StoreContext(UUID.randomUUID(), OffsetDateTime.parse("2026-11-21T10:00:00+09:00"), LocalDate.of(2026, 11, 21),
				37.5446, 127.0446, "성동구", "1120065000", 1, "음식점", "한식", null, List.of(), List.of());
	}

	private static String item(String date, String time, String category, String value) {
		return "{\"baseDate\":\"20261121\",\"baseTime\":\"0800\",\"category\":\"%s\",\"fcstDate\":\"%s\",\"fcstTime\":\"%s\",\"fcstValue\":\"%s\",\"nx\":61,\"ny\":126}"
				.formatted(category, date, time, value);
	}

	/** 21일: 오전 맑음(최저는 TMP로), 오후 흐리고 비 70%·최고 2℃. 22일: 오전 최저 −13℃(한파), 오후 맑음. 28일은 7일 밖 */
	private static JsonNode items() {
		return JSON.readTree("[" + String.join(",",
				item("20261121", "0900", "SKY", "1"), item("20261121", "0900", "PTY", "0"), item("20261121", "0900", "POP", "0"),
				item("20261121", "0900", "TMP", "-3"), item("20261121", "1000", "TMP", "-2"),
				item("20261121", "1500", "SKY", "4"), item("20261121", "1500", "PTY", "1"), item("20261121", "1500", "POP", "70"),
				item("20261121", "1500", "TMP", "1"), item("20261121", "1500", "TMX", "2.0"),
				item("20261122", "0600", "SKY", "3"), item("20261122", "0600", "PTY", "0"), item("20261122", "0600", "POP", "20"),
				item("20261122", "0600", "TMN", "-13.0"), item("20261122", "0600", "TMP", "-13"),
				item("20261122", "1500", "SKY", "1"), item("20261122", "1500", "PTY", "0"), item("20261122", "1500", "TMX", "3.0"),
				item("20261128", "0900", "SKY", "4")) + "]");
	}

	@Test
	void groupsHourlyItemsIntoMorningAndAfternoon() {
		when(dataGoKr.list(eq(PATH), argThat(q -> "20261121".equals(q.get("base_date")) && "0800".equals(q.get("base_time"))
				&& "61".equals(q.get("nx")) && "126".equals(q.get("ny"))))).thenReturn(new ListResponse(19, items()));

		WeatherResponse weather = service.collect(store());

		assertThat(weather.evidenceId()).isEqualTo("W1");
		assertThat(weather.forecasts()).containsExactly(
				new WeatherForecast(LocalDate.of(2026, 11, 21), DayPart.AM, "맑음", -3.0, null, 0),
				new WeatherForecast(LocalDate.of(2026, 11, 21), DayPart.PM, "흐리고 비", null, 2.0, 70),
				new WeatherForecast(LocalDate.of(2026, 11, 22), DayPart.AM, "구름많음", -13.0, null, 20),
				new WeatherForecast(LocalDate.of(2026, 11, 22), DayPart.PM, "맑음", null, 3.0, null));
		assertThat(weather.impactStarts()).containsExactly(
				new WeatherImpactStart(WeatherImpact.RAIN_SNOW, LocalDate.of(2026, 11, 21)),
				new WeatherImpactStart(WeatherImpact.COLD_WAVE, LocalDate.of(2026, 11, 22)));
	}

	@Test
	void recordsFirstDayOfEachImpact() {
		List<WeatherForecast> forecasts = List.of(
				new WeatherForecast(LocalDate.of(2026, 11, 22), DayPart.AM, "맑음", -13.0, null, 0),
				new WeatherForecast(LocalDate.of(2026, 11, 24), DayPart.PM, "흐리고 눈", null, 1.0, 40),
				new WeatherForecast(LocalDate.of(2026, 11, 26), DayPart.PM, "흐리고 비", null, 5.0, 70));

		assertThat(WeatherEvidenceService.impactStarts(forecasts)).containsExactly(
				entry(WeatherImpact.RAIN_SNOW, LocalDate.of(2026, 11, 24)),
				entry(WeatherImpact.COLD_WAVE, LocalDate.of(2026, 11, 22)));
	}

	@Test
	void usesLatestIssuedForecastTenMinutesAfterRelease() {
		assertThat(WeatherEvidenceService.baseTime(LocalDateTime.of(2026, 11, 21, 10, 0))).isEqualTo(LocalDateTime.of(2026, 11, 21, 8, 0));
		assertThat(WeatherEvidenceService.baseTime(LocalDateTime.of(2026, 11, 21, 11, 9))).isEqualTo(LocalDateTime.of(2026, 11, 21, 8, 0));
		assertThat(WeatherEvidenceService.baseTime(LocalDateTime.of(2026, 11, 21, 11, 10))).isEqualTo(LocalDateTime.of(2026, 11, 21, 11, 0));
		assertThat(WeatherEvidenceService.baseTime(LocalDateTime.of(2026, 11, 21, 2, 5))).isEqualTo(LocalDateTime.of(2026, 11, 20, 23, 0));
	}

	@Test
	void convertsCoordinatesToForecastGrid() {
		// 기상청 격자 예시값: 서울시청 (60, 127), 부산시청 (98, 76)
		assertThat(WeatherEvidenceService.grid(37.5665, 126.9780)).containsExactly(60, 127);
		assertThat(WeatherEvidenceService.grid(35.1796, 129.0756)).containsExactly(98, 76);
	}

	@Test
	void returnsNullWhenForecastFailsOrIsEmpty() {
		when(dataGoKr.list(anyString(), anyMap())).thenThrow(new BusinessException(ErrorCode.PUBLIC_API_ERROR, "등록되지 않은 서비스키"));
		assertThat(service.collect(store())).isNull();

		DataGoKrClient empty = mock(DataGoKrClient.class);
		when(empty.list(anyString(), anyMap())).thenReturn(new ListResponse(0, JSON.createArrayNode()));
		assertThat(new WeatherEvidenceService(empty).collect(store())).isNull();
	}
}
