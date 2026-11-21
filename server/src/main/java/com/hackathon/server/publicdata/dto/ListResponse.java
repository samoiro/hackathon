package com.hackathon.server.publicdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(description = "목록 응답")
public record ListResponse(
		@Schema(description = "조건에 맞는 전체 건수", requiredMode = Schema.RequiredMode.REQUIRED) long totalCount,
		@Schema(description = "현재 페이지 데이터 (필드명은 원본 API 그대로)", types = "array", implementation = Object.class, requiredMode = Schema.RequiredMode.REQUIRED) JsonNode items) {
}
