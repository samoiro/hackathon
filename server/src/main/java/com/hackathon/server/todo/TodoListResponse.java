package com.hackathon.server.todo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** GET·POST /api/analysis/{analysisId}/todos 응답. */
@Schema(description = "내 준비할 일 목록")
public record TodoListResponse(
		@Schema(description = "할 일 (AI 강추 먼저, 그다음 추가한 순서)", requiredMode = Schema.RequiredMode.REQUIRED) List<TodoResponse> items,
		@Schema(description = "완료한 수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) int doneCount) {
}
