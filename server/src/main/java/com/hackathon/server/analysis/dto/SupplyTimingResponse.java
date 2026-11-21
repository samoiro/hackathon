package com.hackathon.server.analysis.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.hackathon.server.supply.dto.Demand;

/** 부자재 준비 시점 (supply_rule 기준). 이번 주 걸린 규칙이 없으면(순서도 "부자재 정보 없음") itemId·name만 채운다. */
@Schema(description = "부자재 준비 시점")
public record SupplyTimingResponse(
		@Schema(description = "근거 id. 해당 없으면 null", example = "S1") String evidenceId,
		@Schema(description = "부자재 id", requiredMode = Schema.RequiredMode.REQUIRED) long itemId,
		@Schema(description = "품목 이름", example = "포장 용기", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "수요 변화 (수요 높음 배지)") Demand demand,
		@Schema(description = "이날까지 준비 (11.18(화))") LocalDate prepareBy,
		@Schema(description = "며칠 전 준비 (supply_rule.lead_days_max)", example = "3") Integer leadDays,
		@Schema(description = "준비 이유", example = "공연") String triggerLabel,
		@Schema(description = "연결된 행사·시즌·날씨 근거 id. 없으면 null", example = "E1") String triggerEvidenceId) {
}
