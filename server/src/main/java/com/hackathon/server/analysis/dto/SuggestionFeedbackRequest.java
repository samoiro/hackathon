package com.hackathon.server.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * POST /api/analysis/{id}/feedback 요청 → 204. 분석 1건의 카드 1장에 1표, 다시 보내면 표를 바꾼다.
 * 감사 토스트는 프론트가 띄운다. 해당 없어요는 강추에도 쓰며, 프론트가 그 카드 키를 excludedKeys에 더하고 할 일에서 뺀다.
 */
@Schema(description = "추천 카드 피드백")
public record SuggestionFeedbackRequest(
		@Schema(description = "카드 키 {category}:{taskType}:{itemId 또는 -}", example = "INGREDIENT:PRICE_CHECK:12", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Pattern(regexp = CARD_KEY) String cardKey,
		@Schema(description = "도움돼요 · 해당 없어요", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull FeedbackVote vote) {

	/** 카드 키 형식. suggestion_feedback.card_key 제약과 같다. */
	public static final String CARD_KEY = "^(EVENT|INGREDIENT|SUPPLY):[A-Z_]+:([0-9]+|-)$";
}
