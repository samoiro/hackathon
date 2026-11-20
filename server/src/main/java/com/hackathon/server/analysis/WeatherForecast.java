package com.hackathon.server.analysis;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/** 케이웨더 단기예보(kw-fct-dl)·주간예보(kw-fct-wl) 한 시점. */
@Schema(description = "날씨 예보")
public record WeatherForecast(
		@Schema(description = "날짜", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
		@Schema(description = "오전 · 오후 · 하루", requiredMode = Schema.RequiredMode.REQUIRED) DayPart dayPart,
		@Schema(description = "날씨 문구", example = "흐리고 비", requiredMode = Schema.RequiredMode.REQUIRED) String sky,
		@Schema(description = "최저 기온 (℃)", example = "-3.0") Double minTemperature,
		@Schema(description = "최고 기온 (℃)", example = "5.0") Double maxTemperature,
		@Schema(description = "강수 확률 (%)", example = "60") Integer precipitationChance) {
}
