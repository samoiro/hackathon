package com.hackathon.server.location;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST /api/locations/validate 응답 (항상 200). 서울 밖 등 "쓸 수 없는 주소"는 요청 오류가 아니라 확인 결과라서 valid=false로 돌려준다.
 * 빈 값처럼 요청 자체가 잘못되면 400 ProblemDetail.
 */
@Schema(description = "주소 확인 결과")
public record LocationValidateResponse(
		@Schema(description = "분석할 수 있는 주소인지", requiredMode = Schema.RequiredMode.REQUIRED) boolean valid,
		@Schema(description = "valid=false일 때 화면에 보여줄 이유. true면 null", example = "서울 밖 주소는 아직 지원하지 않아요.") String message) {
}
