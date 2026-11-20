package com.hackathon.server.ingredient;

import io.swagger.v3.oas.annotations.media.Schema;

/** GET /api/ingredients 한 건 (ingredient 테이블). */
@Schema(description = "원자재")
public record IngredientResponse(
		@Schema(description = "원자재 번호 (ingredient.id)", example = "9", requiredMode = Schema.RequiredMode.REQUIRED) long id,
		@Schema(description = "품목 이름 (ingredient.item_name)", example = "배추", requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
