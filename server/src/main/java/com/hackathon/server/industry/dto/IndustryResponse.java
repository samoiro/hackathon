package com.hackathon.server.industry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** industry 테이블 한 줄. */
@Schema(description = "지원 업종")
public record IndustryResponse(
		@Schema(description = "업종 id", example = "9", requiredMode = Schema.RequiredMode.REQUIRED) long id,
		@Schema(description = "업종 분류 (음식점 · 소매점 · 단체·개인서비스업)", example = "음식점", requiredMode = Schema.RequiredMode.REQUIRED) String category,
		@Schema(description = "업종 이름", example = "한식·국밥", requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
