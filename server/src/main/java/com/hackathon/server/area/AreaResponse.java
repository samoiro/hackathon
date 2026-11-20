package com.hackathon.server.area;

import io.swagger.v3.oas.annotations.media.Schema;

/** GET /api/areas 한 건. city_data_area 테이블 (서울시 주요 장소, 데이터셋 OA-21285). */
@Schema(description = "지원 지역")
public record AreaResponse(
		@Schema(description = "장소 코드", example = "POI009", requiredMode = Schema.RequiredMode.REQUIRED) String code,
		@Schema(description = "장소 이름. /api/seoul/city-data/{area}에 그대로 넘긴다", example = "광화문·덕수궁", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "분류", example = "고궁·문화유산", requiredMode = Schema.RequiredMode.REQUIRED) String category,
		@Schema(description = "영역 중심 위도 (boundary의 ST_Centroid)", requiredMode = Schema.RequiredMode.REQUIRED) double latitude,
		@Schema(description = "영역 중심 경도 (boundary의 ST_Centroid)", requiredMode = Schema.RequiredMode.REQUIRED) double longitude) {
}
