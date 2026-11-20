package com.hackathon.server.analysis;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/analysis 요청. 가게 정보 선택 화면의 입력 전부.
 * 좌표는 /api/locations/validate 결과를 그대로 보내지만, 서버는 서울 여부를 다시 확인한다.
 */
@Schema(description = "분석 요청 (가게 정보 · 선택 품목)")
public record AnalysisRequest(
		@Schema(description = "가게 이름 (필수)", example = "성수동 한식당", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 30) String storeName,
		@Schema(description = "도로명 주소 (필수)", example = "서울 성동구 서울숲2길 32", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String roadAddress,
		@Schema(description = "위도", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Double latitude,
		@Schema(description = "경도", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Double longitude,
		@Schema(description = "업종 id (필수)", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long industryId,
		@Schema(description = "운영 형태 (선택). null = 선택 안 함") OperationType operationType,
		@Schema(description = "원자재 id (선택, 최대 3개, 중복 불가)") @Size(max = 3) List<@NotNull Long> ingredientIds,
		@Schema(description = "부자재 id (선택, 최대 2개, 중복 불가)") @Size(max = 2) List<@NotNull Long> supplyIds,
		@Schema(description = "제외 목록: 사장님이 '해당 없어요'로 표시한 할 일 제목. 같거나 비슷한 후보를 뺀다 (순서도 11). 로그인이 없어 브라우저가 보관해 보낸다") @Size(max = 50) List<@NotBlank @Size(max = 40) String> excludedTitles) {
}
