package com.hackathon.server.supply;

import io.swagger.v3.oas.annotations.media.Schema;

/** supply_trigger 테이블 한 줄. */
@Schema(description = "준비 시점을 정하는 사건 (날씨·행사·기념일 등)")
public record SupplyTriggerResponse(
		@Schema(description = "트리거 코드", example = "PERFORMANCE", requiredMode = Schema.RequiredMode.REQUIRED) String code,
		@Schema(description = "화면 표시 이름", example = "공연", requiredMode = Schema.RequiredMode.REQUIRED) String label,
		@Schema(description = "종류", requiredMode = Schema.RequiredMode.REQUIRED) TriggerType type) {
}
