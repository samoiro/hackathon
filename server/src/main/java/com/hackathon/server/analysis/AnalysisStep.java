package com.hackathon.server.analysis;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분석 단계 상태")
public record AnalysisStep(
		@Schema(description = "단계", requiredMode = Schema.RequiredMode.REQUIRED) StepCode code,
		@Schema(description = "상태", requiredMode = Schema.RequiredMode.REQUIRED) StepStatus status) {
}
