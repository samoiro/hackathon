package com.hackathon.server.analysis;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** POST /api/analysis 응답 (202 Accepted). 이 id로 GET /api/analysis/{id}를 폴링한다. */
@Schema(description = "분석 시작 결과")
public record AnalysisCreatedResponse(
		@Schema(description = "분석 id", requiredMode = Schema.RequiredMode.REQUIRED) UUID analysisId,
		@Schema(description = "처음엔 PENDING", requiredMode = Schema.RequiredMode.REQUIRED) AnalysisStatus status) {
}
