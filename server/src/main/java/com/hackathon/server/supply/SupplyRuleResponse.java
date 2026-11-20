package com.hackathon.server.supply;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** supply_rule 한 줄 + 트리거 + 품목. */
@Schema(description = "업종별 부자재 준비 규칙")
public record SupplyRuleResponse(
		@Schema(description = "규칙 id", requiredMode = Schema.RequiredMode.REQUIRED) long id,
		@Schema(description = "업종 id", requiredMode = Schema.RequiredMode.REQUIRED) long industryId,
		@Schema(description = "트리거", requiredMode = Schema.RequiredMode.REQUIRED) SupplyTriggerResponse trigger,
		@Schema(description = "노션 원문", example = "축제·공연·혼잡도 급증", requiredMode = Schema.RequiredMode.REQUIRED) String triggerText,
		@Schema(description = "true: 날씨 예보 기준, false: 날짜 기준", requiredMode = Schema.RequiredMode.REQUIRED) boolean forecast,
		@Schema(description = "며칠 전부터 준비하는지 (최소). 수요가 줄면 null", example = "7") Integer leadDaysMin,
		@Schema(description = "며칠 전부터 준비하는지 (최대). 수요가 줄면 null", example = "7") Integer leadDaysMax,
		@Schema(description = "수요 변화", requiredMode = Schema.RequiredMode.REQUIRED) Demand demand,
		@Schema(description = "품목 (준비표 순서)", requiredMode = Schema.RequiredMode.REQUIRED) List<SupplyRuleItemResponse> items) {
}
