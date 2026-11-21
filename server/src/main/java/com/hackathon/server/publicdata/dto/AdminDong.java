package com.hackathon.server.publicdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 카카오 coord2regioncode의 행정동(H) 결과. code는 케이웨더 조회에 그대로 쓴다. */
@Schema(description = "좌표가 속한 행정동")
public record AdminDong(
		@Schema(description = "행정동 코드 10자리 (케이웨더 code)", example = "1120065000", requiredMode = Schema.RequiredMode.REQUIRED) String code,
		@Schema(description = "전체 이름", example = "서울특별시 성동구 성수1가1동", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "시도", example = "서울특별시", requiredMode = Schema.RequiredMode.REQUIRED) String sido,
		@Schema(description = "시군구", example = "성동구", requiredMode = Schema.RequiredMode.REQUIRED) String sigungu,
		@Schema(description = "행정동", example = "성수1가1동", requiredMode = Schema.RequiredMode.REQUIRED) String dong) {
}
