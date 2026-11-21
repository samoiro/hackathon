package com.hackathon.server.publicdata.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예상 혼잡도")
public record CityPopulationForecast(
		@Schema(description = "예측 시각 (FCST_TIME)", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime time,
		@Schema(description = "예상 혼잡도 (FCST_CONGEST_LVL)", requiredMode = Schema.RequiredMode.REQUIRED) Congestion congestion,
		@Schema(description = "예상 인구 최소 (FCST_PPLTN_MIN)", example = "32000", requiredMode = Schema.RequiredMode.REQUIRED) int populationMin,
		@Schema(description = "예상 인구 최대 (FCST_PPLTN_MAX)", example = "34000", requiredMode = Schema.RequiredMode.REQUIRED) int populationMax) {
}
