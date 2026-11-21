package com.hackathon.server.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
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
import com.hackathon.server.publicdata.client.KweatherClient;

/** 성수동 시연 원본 응답(demo/seongsu, 2026-11-21 받음)으로 읽는다. */
class WeatherEvidenceServiceTest {

	private static final String DONG = "1120065000";
	private static final LocalDate BASE = LocalDate.of(2026, 11, 21);
	private static final JsonMapper JSON = JsonMapper.builder().build();
	private final KweatherClient kweather = mock(KweatherClient.class);
	private final WeatherEvidenceService service = new WeatherEvidenceService(kweather);

	private static JsonNode demo(String file) throws IOException {
		try (InputStream in = WeatherEvidenceServiceTest.class.getResourceAsStream("/demo/seongsu/" + file)) {
			return JSON.readTree(in).path("data");
		}
	}

	private static StoreContext store(String dong) {
		return new StoreContext(UUID.randomUUID(), OffsetDateTime.parse("2026-11-21T10:00:00+09:00"), LocalDate.of(2026, 11, 21),
				37.5446, 127.0446, "성동구", dong, 1, "음식점", "한식", null, List.of(), List.of());
	}

	@Test
	void readsDemoForecastsForSevenDays() throws IOException {
		when(kweather.get("kw-fct-dl1", DONG)).thenReturn(demo("kweather-short-forecast.json"));
		when(kweather.get("kw-fct-wl1", DONG)).thenReturn(demo("kweather-weekly-forecast.json"));

		WeatherResponse weather = service.collect(store(DONG));

		assertThat(weather.evidenceId()).isEqualTo("W1");
		assertThat(weather.warnings()).isEmpty();
		// 단기 6칸(11-21 오전 ~ 11-23 오후) + 주간 11-24~11-27 오전·오후 8칸
		assertThat(weather.forecasts()).hasSize(14);
		assertThat(weather.forecasts().getFirst()).satisfies(f -> {
			assertThat(f.date()).isEqualTo(LocalDate.of(2026, 11, 21));
			assertThat(f.dayPart()).isEqualTo(DayPart.AM);
			assertThat(f.minTemperature()).isEqualTo(-5.0);
		});
		assertThat(weather.forecasts().get(5)).satisfies(f -> {
			assertThat(f.date()).isEqualTo(LocalDate.of(2026, 11, 23));
			assertThat(f.sky()).isEqualTo("흐리고 비");
		});
		assertThat(weather.forecasts().getLast().date()).isEqualTo(LocalDate.of(2026, 11, 27));
		assertThat(weather.impacts()).containsExactly(WeatherImpact.RAIN_SNOW);
		// 단기예보 5번째 칸(23일 오전) "흐리고 비"(60%)가 첫 비 예보
		assertThat(weather.impactStarts()).containsExactly(new WeatherImpactStart(WeatherImpact.RAIN_SNOW, LocalDate.of(2026, 11, 23)));
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
	void keepsShortForecastWhenWeeklyFails() throws IOException {
		when(kweather.get("kw-fct-dl1", DONG)).thenReturn(demo("kweather-short-forecast.json"));
		when(kweather.get("kw-fct-wl1", DONG)).thenThrow(new BusinessException(ErrorCode.PUBLIC_API_ERROR, "케이웨더 API 오류 (HTTP 429)"));

		WeatherResponse weather = service.collect(store(DONG));

		// 단기예보 6칸(11-21 오전 ~ 11-23 오후)만 남는다
		assertThat(weather.forecasts()).hasSize(6);
		assertThat(weather.warnings()).isEmpty();
	}

	@Test
	void returnsNullWhenKweatherFails() {
		when(kweather.get(anyString(), eq(DONG))).thenThrow(new BusinessException(ErrorCode.PUBLIC_API_CONNECTION_FAILED, "케이웨더 API 호출 실패"));

		assertThat(service.collect(store(DONG))).isNull();
		assertThat(service.collect(store(null))).isNull();
	}
}
