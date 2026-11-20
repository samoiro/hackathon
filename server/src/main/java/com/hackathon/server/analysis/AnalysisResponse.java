package com.hackathon.server.analysis;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** GET /api/analysis/{id}. 분석 중에는 steps만, 끝나면 briefing 또는 failure가 채워진다. */
@Schema(description = "분석 상태와 결과")
public record AnalysisResponse(
		@Schema(description = "분석 id", requiredMode = Schema.RequiredMode.REQUIRED) UUID analysisId,
		@Schema(description = "전체 상태", requiredMode = Schema.RequiredMode.REQUIRED) AnalysisStatus status,
		@Schema(description = "브리핑 기준 시각 = 리포트 만들기를 누른 시각. 행사·날씨의 향후 7일은 이 시각부터 센다", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime baseTime,
		@Schema(description = "입력한 가게 정보. 칩·대시보드 머리말·'입력한 정보는 그대로예요'에 쓴다", requiredMode = Schema.RequiredMode.REQUIRED) StoreSummary store,
		@Schema(description = "4단계 상태 (StepCode 순서로 항상 4개)", requiredMode = Schema.RequiredMode.REQUIRED) List<AnalysisStep> steps,
		@Schema(description = "행사 조회 조건과 건수. EVENT_SEARCH가 DONE이면 채움") EventSearch eventSearch,
		@Schema(description = "실패 정보. FAILED일 때만") AnalysisFailure failure,
		@Schema(description = "브리핑. SUCCEEDED일 때만") BriefingResponse briefing) {
}
