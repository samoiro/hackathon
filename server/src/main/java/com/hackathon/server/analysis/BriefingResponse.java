package com.hackathon.server.analysis;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** 통합 대시보드의 브리핑 영역 전부. 지역 현황은 /api/seoul/city-data/{area}로 따로 받는다. */
@Schema(description = "브리핑")
public record BriefingResponse(
		@Schema(description = "주변 행사 (순서도 3·4). 0건이면 시즌 행사·날씨로 넘어감", requiredMode = Schema.RequiredMode.REQUIRED) List<EventResponse> events,
		@Schema(description = "시즌 행사 (순서도 5). 주변 행사가 없을 때만 채움", requiredMode = Schema.RequiredMode.REQUIRED) List<SeasonEventResponse> seasonEvents,
		@Schema(description = "날씨 (순서도 3-1). 주변 행사·시즌 행사가 모두 없을 때만 채움") WeatherResponse weather,
		@Schema(description = "고른 원자재 전부의 가격 (가격 자료 없는 품목 포함, 순서도 7)", requiredMode = Schema.RequiredMode.REQUIRED) List<IngredientPriceResponse> ingredients,
		@Schema(description = "고른 부자재 전부의 준비 시점 (부자재 정보 없음 포함, 순서도 9)", requiredMode = Schema.RequiredMode.REQUIRED) List<SupplyTimingResponse> supplies,
		@Schema(description = "추천 카드 (순서도 17~19). 강추가 하나도 없으면 빈 상태 문구", requiredMode = Schema.RequiredMode.REQUIRED) List<SuggestionResponse> suggestions,
		@Schema(description = "근거 (하단 출처·확인 일시)", requiredMode = Schema.RequiredMode.REQUIRED) List<SourceResponse> sources) {
}
