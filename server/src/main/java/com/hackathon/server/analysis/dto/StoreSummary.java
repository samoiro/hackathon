package com.hackathon.server.analysis.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.hackathon.server.industry.dto.IndustryResponse;
import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.supply.dto.SupplyResponse;

@Schema(description = "분석에 쓴 가게 정보")
public record StoreSummary(
		@Schema(description = "가게 이름", example = "성수동 한식당", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "도로명 주소", requiredMode = Schema.RequiredMode.REQUIRED) String roadAddress,
		@Schema(description = "자치구", example = "성동구", requiredMode = Schema.RequiredMode.REQUIRED) String gu,
		@Schema(description = "생활권 표시 이름", example = "성수동 · 서울숲 인근", requiredMode = Schema.RequiredMode.REQUIRED) String areaLabel,
		@Schema(description = "업종", requiredMode = Schema.RequiredMode.REQUIRED) IndustryResponse industry,
		@Schema(description = "운영 형태. null = 선택 안 함") OperationType operationType,
		@Schema(description = "고른 원자재", requiredMode = Schema.RequiredMode.REQUIRED) List<IngredientResponse> ingredients,
		@Schema(description = "고른 부자재", requiredMode = Schema.RequiredMode.REQUIRED) List<SupplyResponse> supplies) {
}
