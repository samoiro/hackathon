package com.hackathon.server.analysis;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분석 실패 정보")
public record AnalysisFailure(
		@Schema(description = "멈춘 단계", requiredMode = Schema.RequiredMode.REQUIRED) StepCode step,
		@Schema(description = "실패 코드", example = "PRICE_SOURCE_FAILED", requiredMode = Schema.RequiredMode.REQUIRED) String code,
		@Schema(description = "단계 아래 문구", example = "가격 자료를 불러오지 못했어요", requiredMode = Schema.RequiredMode.REQUIRED) String message) {
}
