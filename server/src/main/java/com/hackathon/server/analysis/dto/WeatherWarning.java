package com.hackathon.server.analysis.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/** 케이웨더 kw-warning 한 건. */
@Schema(description = "기상특보")
public record WeatherWarning(
		@Schema(description = "특보 종류", example = "한파", requiredMode = Schema.RequiredMode.REQUIRED) String kind,
		@Schema(description = "강도", example = "주의보", requiredMode = Schema.RequiredMode.REQUIRED) String level,
		@Schema(description = "발효 시각", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime startTime,
		@Schema(description = "해제 시각. 해제 전이면 null") OffsetDateTime endTime) {
}
