package com.hackathon.server.publicdata;

import io.swagger.v3.oas.annotations.media.Schema;

/** "맑음 · 8°C · 강수 확률 10% · 미세먼지 보통". */
@Schema(description = "지역 날씨")
public record CityWeather(
		@Schema(description = "하늘 상태 (SKY_STTS)", example = "맑음") String sky,
		@Schema(description = "기온 (TEMP, ℃)", example = "8.0") Double temperature,
		@Schema(description = "강수 확률 (가장 가까운 시각의 RAIN_CHANCE, %)", example = "10") Integer precipitationChance,
		@Schema(description = "미세먼지 등급 (PM10_INDEX). FINE_DUST 판정에 씀", example = "보통") String pm10Index,
		@Schema(description = "초미세먼지 등급 (PM25_INDEX)", example = "좋음") String pm25Index) {
}
