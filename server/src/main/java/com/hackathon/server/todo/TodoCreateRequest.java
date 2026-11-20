package com.hackathon.server.todo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/analysis/{analysisId}/todos: 사장님이 직접 추가. */
@Schema(description = "할 일 직접 추가")
public record TodoCreateRequest(
		@Schema(description = "할 일 내용", example = "냅킨 주문하기", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 40) String title) {
}
