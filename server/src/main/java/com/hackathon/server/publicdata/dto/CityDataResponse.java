package com.hackathon.server.publicdata.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/** GET /api/seoul/city-data/{area}를 화면용으로 줄인 응답 ("지금 이 지역은 어떤가요?"). 원본 CITYDATA는 응답이 커서 그대로 보내지 않는다. */
@Schema(description = "지역 현황")
public record CityDataResponse(
		@Schema(description = "장소 코드 (AREA_CD)", example = "POI072", requiredMode = Schema.RequiredMode.REQUIRED) String areaCode,
		@Schema(description = "장소 이름 (AREA_NM)", example = "서울숲공원", requiredMode = Schema.RequiredMode.REQUIRED) String areaName,
		@Schema(description = "인구 집계 시각 (PPLTN_TIME). '11.17 07:00 기준'", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime observedAt,
		@Schema(description = "현재 혼잡도 (AREA_CONGEST_LVL)", requiredMode = Schema.RequiredMode.REQUIRED) Congestion congestion,
		@Schema(description = "현재 인구 최소 (AREA_PPLTN_MIN)", example = "28000", requiredMode = Schema.RequiredMode.REQUIRED) int populationMin,
		@Schema(description = "현재 인구 최대 (AREA_PPLTN_MAX)", example = "30000", requiredMode = Schema.RequiredMode.REQUIRED) int populationMax,
		@Schema(description = "남성 인구 비율 (MALE_PPLTN_RATE, %)", example = "52.7") Double malePercent,
		@Schema(description = "여성 인구 비율 (FEMALE_PPLTN_RATE, %)", example = "47.3") Double femalePercent,
		@Schema(description = "이후 예상 혼잡도 (FCST_PPLTN, 12시간 뒤까지 시각 순)", requiredMode = Schema.RequiredMode.REQUIRED) List<CityPopulationForecast> forecasts,
		@Schema(description = "연령대별 소비 비율 (LIVE_CMRCL_STTS의 CMRCL_10~60_RATE). 상권 자료가 없는 장소면 빈 목록", requiredMode = Schema.RequiredMode.REQUIRED) List<CityAgeRatio> consumptionRatios,
		@Schema(description = "주변 문화행사 (EVENT_STTS, 좌표 있는 것만). 지도 핀", requiredMode = Schema.RequiredMode.REQUIRED) List<CityEvent> events,
		@Schema(description = "날씨 (WEATHER_STTS). 자료가 없으면 null") CityWeather weather) {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/** 원본 CITYDATA 객체에서 화면에 쓰는 값만 뽑는다. 서울시는 숫자도 문자열로 준다. */
	public static CityDataResponse from(JsonNode city) {
		JsonNode ppltn = city.path("LIVE_PPLTN_STTS").path(0);
		List<CityPopulationForecast> forecasts = new ArrayList<>();
		for (JsonNode f : ppltn.path("FCST_PPLTN")) {
			forecasts.add(new CityPopulationForecast(time(f.path("FCST_TIME")), Congestion.fromLabel(f.path("FCST_CONGEST_LVL").asString()),
					f.path("FCST_PPLTN_MIN").asInt(), f.path("FCST_PPLTN_MAX").asInt()));
		}
		JsonNode cmrcl = city.path("LIVE_CMRCL_STTS");
		List<CityAgeRatio> ratios = new ArrayList<>();
		for (int age = 10; age <= 60; age += 10) {
			Double rate = number(cmrcl.path("CMRCL_" + age + "_RATE"));
			if (rate != null) ratios.add(new CityAgeRatio(age, rate));
		}
		List<CityEvent> events = new ArrayList<>();
		for (JsonNode e : city.path("EVENT_STTS")) {
			Double lat = number(e.path("EVENT_Y"));
			Double lng = number(e.path("EVENT_X"));
			if (lat != null && lng != null) events.add(new CityEvent(e.path("EVENT_NM").asString(), lat, lng));
		}
		return new CityDataResponse(city.path("AREA_CD").asString(), city.path("AREA_NM").asString(), time(ppltn.path("PPLTN_TIME")),
				Congestion.fromLabel(ppltn.path("AREA_CONGEST_LVL").asString()),
				ppltn.path("AREA_PPLTN_MIN").asInt(), ppltn.path("AREA_PPLTN_MAX").asInt(),
				number(ppltn.path("MALE_PPLTN_RATE")), number(ppltn.path("FEMALE_PPLTN_RATE")), forecasts, ratios, events,
				weather(city.path("WEATHER_STTS").path(0)));
	}

	private static CityWeather weather(JsonNode w) {
		if (w.isMissingNode() || w.isNull()) return null;
		JsonNode next = w.path("FCST24HOURS").path(0);
		Double rain = number(next.path("RAIN_CHANCE"));
		return new CityWeather(next.path("SKY_STTS").asString(null), number(w.path("TEMP")), rain == null ? null : rain.intValue());
	}

	private static OffsetDateTime time(JsonNode node) {
		return LocalDateTime.parse(node.asString(), TIME).atZone(SEOUL).toOffsetDateTime();
	}

	/** 빈 문자열·"-"·없음은 null. */
	private static Double number(JsonNode node) {
		String text = node.isValueNode() ? node.asString().trim() : "";
		try {
			return text.isEmpty() ? null : Double.valueOf(text);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
}
