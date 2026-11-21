package com.hackathon.server.analysis.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/** 행사 트랙 조회 조건과 결과 분기. "향후 7일 · 2건 확인". */
@Schema(description = "행사 조회 조건과 건수")
public record EventSearch(
		@Schema(description = "조회 기간 (일)", example = "7", requiredMode = Schema.RequiredMode.REQUIRED) int days,
		@Schema(description = "기준일 (리포트 만들기 클릭 날짜)", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate baseDate,
		@Schema(description = "찾은 주변 행사 수", example = "2", requiredMode = Schema.RequiredMode.REQUIRED) int count,
		@Schema(description = "행사 후보를 만든 분기: 주변 행사 → 시즌 행사 → 날씨 → 없음", requiredMode = Schema.RequiredMode.REQUIRED) EventSource source) {
}
