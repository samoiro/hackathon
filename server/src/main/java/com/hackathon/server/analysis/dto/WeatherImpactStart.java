package com.hackathon.server.analysis.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/** 영향 날씨가 처음 나타나는 날. 부자재 준비 기한(시작일 − supply_rule.lead_days_max)을 이 날로 센다. */
@Schema(description = "영향 날씨 시작일")
public record WeatherImpactStart(
		@Schema(description = "영향 날씨", requiredMode = Schema.RequiredMode.REQUIRED) WeatherImpact impact,
		@Schema(description = "처음 나타나는 날 (발효 중인 특보는 기준일)", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date) {
}
