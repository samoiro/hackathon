package com.hackathon.server.publicdata;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** GET /api/seoul/city-data/{area}를 화면용으로 줄인 응답 ("지금 이 지역은 어떤가요?"). 원본 CITYDATA는 응답이 커서 그대로 보내지 않는다. */
@Schema(description = "지역 현황")
public record CityDataResponse(
		@Schema(description = "장소 코드 (AREA_CD)", example = "POI072", requiredMode = Schema.RequiredMode.REQUIRED) String areaCode,
		@Schema(description = "장소 이름 (AREA_NM)", example = "서울숲공원", requiredMode = Schema.RequiredMode.REQUIRED) String areaName,
		@Schema(description = "인구 집계 시각 (PPLTN_TIME). '11.17 07:00 기준'", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime observedAt,
		@Schema(description = "현재 혼잡도 (AREA_CONGEST_LVL)", requiredMode = Schema.RequiredMode.REQUIRED) Congestion congestion,
		@Schema(description = "현재 인구 최소 (AREA_PPLTN_MIN)", example = "28000", requiredMode = Schema.RequiredMode.REQUIRED) int populationMin,
		@Schema(description = "현재 인구 최대 (AREA_PPLTN_MAX)", example = "30000", requiredMode = Schema.RequiredMode.REQUIRED) int populationMax,
		@Schema(description = "이후 예상 혼잡도 (FCST_PPLTN, 12시간 뒤까지 시각 순)", requiredMode = Schema.RequiredMode.REQUIRED) List<CityPopulationForecast> forecasts,
		@Schema(description = "연령대별 인구 비율 (PPLTN_RATE_0~70)", requiredMode = Schema.RequiredMode.REQUIRED) List<CityAgeRatio> ageRatios,
		@Schema(description = "날씨 (WEATHER_STTS). 자료가 없으면 null") CityWeather weather) {
}
