package com.hackathon.server.supply;

import io.swagger.v3.oas.annotations.media.Schema;

/** supply_rule_item 한 줄 + supply.name. */
@Schema(description = "준비 규칙에 걸린 품목")
public record SupplyRuleItemResponse(
		@Schema(description = "부자재 id", example = "3", requiredMode = Schema.RequiredMode.REQUIRED) long supplyId,
		@Schema(description = "품목 이름", example = "포장 용기", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "SUPPLY: 부자재, STOCK: 손님에게 파는 상품", requiredMode = Schema.RequiredMode.REQUIRED) SupplyKind kind,
		@Schema(description = "준비표에 적힌 순서 (0부터)", example = "0", requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder) {
}
