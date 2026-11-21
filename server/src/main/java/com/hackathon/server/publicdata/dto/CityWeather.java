package com.hackathon.server.publicdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** "맑음 · 8°C · 강수 확률 10%". */
@Schema(description = "지역 날씨")
public record CityWeather(
		@Schema(description = "하늘 상태 (가장 가까운 시각 FCST24HOURS의 SKY_STTS)", example = "맑음") String sky,
		@Schema(description = "기온 (TEMP, ℃)", example = "8.0") Double temperature,
		@Schema(description = "강수 확률 (가장 가까운 시각 FCST24HOURS의 RAIN_CHANCE, %)", example = "10") Integer precipitationChance) {
}
