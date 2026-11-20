package com.hackathon.server.analysis;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** 관심 품목 가격 (원자재). 가격 자료가 없으면 price부터 priceStage까지 모두 null. */
@Schema(description = "원자재 가격")
public record IngredientPriceResponse(
		@Schema(description = "근거 id", example = "R1", requiredMode = Schema.RequiredMode.REQUIRED) String evidenceId,
		@Schema(description = "원자재 id", requiredMode = Schema.RequiredMode.REQUIRED) long itemId,
		@Schema(description = "품목 이름", example = "배추", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "등급", example = "상품") String grade,
		@Schema(description = "단위", example = "1포기") String unit,
		@Schema(description = "조사 기준일") LocalDate baseDate,
		@Schema(description = "기준일 가격 (원)", example = "4380") Long price,
		@Schema(description = "4주 전 → 기준일 순서 5개. 빈 주는 null") List<Long> weeklyPrices,
		@Schema(description = "1주 전 대비. 가격 흐름(trend) 기준") PriceChange change1w,
		@Schema(description = "4주 전 대비. 가격 단계 기준이고 화면 설명에도 씀 ('4주 전보다 430원 올랐어요')") PriceChange change4w,
		@Schema(description = "가격 흐름") Trend trend,
		@Schema(description = "같은 방향으로 이어진 주 수 (4주 연속 상승). FLAT이면 0") Integer consecutiveWeeks,
		@Schema(description = "가격 단계: 4주 변동률을 부류별 분위수로 나눈 7단계 (수급조절매뉴얼 방식). 강추 점수의 원자재 가격 신호. 기준: docs/database-schema.md ingredient_price_band") PriceStage priceStage) {
}
