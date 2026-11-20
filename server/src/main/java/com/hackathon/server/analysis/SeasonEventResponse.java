package com.hackathon.server.analysis;

import java.time.LocalDate;

import com.hackathon.server.supply.TriggerType;

import io.swagger.v3.oas.annotations.media.Schema;

/** 순서도 5: 주변 행사가 없을 때 쓰는 시즌 행사. supply_trigger + supply_trigger_date. */
@Schema(description = "시즌 행사 (설·추석·크리스마스·빼빼로데이 등)")
public record SeasonEventResponse(
		@Schema(description = "근거 id", example = "H1", requiredMode = Schema.RequiredMode.REQUIRED) String evidenceId,
		@Schema(description = "트리거 코드", example = "CHRISTMAS", requiredMode = Schema.RequiredMode.REQUIRED) String code,
		@Schema(description = "이름", example = "크리스마스", requiredMode = Schema.RequiredMode.REQUIRED) String label,
		@Schema(description = "종류 (HOLIDAY · SCHOOL · SEASON)", requiredMode = Schema.RequiredMode.REQUIRED) TriggerType type,
		@Schema(description = "시작일", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate,
		@Schema(description = "종료일 (하루짜리면 시작일과 같음)", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate endDate) {
}
