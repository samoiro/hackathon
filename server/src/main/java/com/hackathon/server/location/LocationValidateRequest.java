package com.hackathon.server.location;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** POST /api/locations/validate 요청. 주소 검색은 프론트(카카오 지도 JS)가 하고, 고른 주소만 서버가 확인한다. */
@Schema(description = "주소 확인 요청")
public record LocationValidateRequest(
		@Schema(description = "도로명 주소", example = "서울 성동구 서울숲2길 32", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String roadAddress,
		@Schema(description = "위도", example = "37.5446", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Double latitude,
		@Schema(description = "경도", example = "127.0446", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Double longitude) {
}
