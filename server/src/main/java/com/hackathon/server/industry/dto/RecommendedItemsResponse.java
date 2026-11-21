package com.hackathon.server.industry.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.supply.dto.SupplyResponse;

/** GET /api/industries/{industryId}/recommended-items. "많이 고르는 품목 · 한 번에 담기"에 쓴다. */
@Schema(description = "업종 추천 품목")
public record RecommendedItemsResponse(
		@Schema(description = "업종 id", requiredMode = Schema.RequiredMode.REQUIRED) long industryId,
		@Schema(description = "추천 원자재 (최대 3개, 원자재 선택 상한과 같음)", requiredMode = Schema.RequiredMode.REQUIRED) List<IngredientResponse> ingredients,
		@Schema(description = "추천 부자재 (최대 2개, 부자재 선택 상한과 같음)", requiredMode = Schema.RequiredMode.REQUIRED) List<SupplyResponse> supplies) {
}
