package com.hackathon.server.todo;

import java.time.OffsetDateTime;

import com.hackathon.server.analysis.SuggestionCategory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 준비할 일")
public record TodoResponse(
		@Schema(description = "할 일 id", requiredMode = Schema.RequiredMode.REQUIRED) long id,
		@Schema(description = "할 일 내용", example = "배추 거래처 2곳에 단가 묻기", requiredMode = Schema.RequiredMode.REQUIRED) String title,
		@Schema(description = "AI 강추 · 직접 추가", requiredMode = Schema.RequiredMode.REQUIRED) TodoSource source,
		@Schema(description = "어느 추천 카드에서 왔는지. 직접 추가면 null") Long suggestionId,
		@Schema(description = "유형 라벨. 직접 추가면 null") SuggestionCategory category,
		@Schema(description = "완료 여부", requiredMode = Schema.RequiredMode.REQUIRED) boolean done,
		@Schema(description = "삭제 가능 여부 (USER만 true)", requiredMode = Schema.RequiredMode.REQUIRED) boolean deletable,
		@Schema(description = "추가된 시각", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt) {
}
