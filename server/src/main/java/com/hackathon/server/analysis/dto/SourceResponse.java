package com.hackathon.server.analysis.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "근거 자료")
public record SourceResponse(
		@Schema(description = "자료 이름", example = "서울시 문화행사 정보", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "제공 기관", example = "서울 열린데이터광장", requiredMode = Schema.RequiredMode.REQUIRED) String provider,
		@Schema(description = "갱신 주기", example = "매일 갱신", requiredMode = Schema.RequiredMode.REQUIRED) String updateCycle,
		@Schema(description = "자료 기준일 (가격처럼 기준일이 있는 자료만)") LocalDate baseDate,
		@Schema(description = "확인 시각", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime checkedAt,
		@Schema(description = "원문 링크") String url) {
}
