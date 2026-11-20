package com.hackathon.server.todo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** PATCH /api/todos/{todoId}: 완료 체크. 내용 수정은 없음. */
@Schema(description = "할 일 완료 체크")
public record TodoUpdateRequest(
		@Schema(description = "완료 여부", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Boolean done) {
}
