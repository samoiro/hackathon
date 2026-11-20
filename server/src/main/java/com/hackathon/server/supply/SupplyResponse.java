package com.hackathon.server.supply;

import io.swagger.v3.oas.annotations.media.Schema;

/** supply 테이블 한 줄. */
@Schema(description = "부자재")
public record SupplyResponse(
		@Schema(description = "부자재 id", example = "3", requiredMode = Schema.RequiredMode.REQUIRED) long id,
		@Schema(description = "부자재 이름", example = "포장 용기", requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
