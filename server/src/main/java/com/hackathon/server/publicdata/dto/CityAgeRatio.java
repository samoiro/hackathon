package com.hackathon.server.publicdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "연령대별 소비 비율")
public record CityAgeRatio(
		@Schema(description = "연령대 (10 = 10대 … 60 = 60대 이상)", example = "20", requiredMode = Schema.RequiredMode.REQUIRED) int ageGroup,
		@Schema(description = "비율 (%)", example = "31.0", requiredMode = Schema.RequiredMode.REQUIRED) double percent) {
}
