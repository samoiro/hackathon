package com.hackathon.server.analysis;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/** 서울시 문화행사(aiViewCulture)는 날짜 단위라 시각은 문구로만 둔다. */
@Schema(description = "주변 행사")
public record EventResponse(
		@Schema(description = "근거 id (추천 할 일과 연결)", example = "E1", requiredMode = Schema.RequiredMode.REQUIRED) String evidenceId,
		@Schema(description = "행사 이름", example = "서울숲 음악회", requiredMode = Schema.RequiredMode.REQUIRED) String title,
		@Schema(description = "분류 (필터 칩)", example = "공연", requiredMode = Schema.RequiredMode.REQUIRED) String category,
		@Schema(description = "분류가 켜는 트리거 (부자재 준비표 연결)", requiredMode = Schema.RequiredMode.REQUIRED) EventTrigger trigger,
		@Schema(description = "장소", example = "서울숲 야외무대", requiredMode = Schema.RequiredMode.REQUIRED) String place,
		@Schema(description = "시작일", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate,
		@Schema(description = "종료일", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate endDate,
		@Schema(description = "시간 문구. 원본에 없으면 null", example = "16:00–19:00") String timeText,
		@Schema(description = "예정 · 진행 중", requiredMode = Schema.RequiredMode.REQUIRED) EventStatus status,
		@Schema(description = "가게까지 직선거리 (m)", example = "650", requiredMode = Schema.RequiredMode.REQUIRED) int distanceM,
		@Schema(description = "도보 시간 추정 (분)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED) int walkMinutes,
		@Schema(description = "무료 여부. 모르면 null") Boolean isFree,
		@Schema(description = "위도 (지도 표시)", requiredMode = Schema.RequiredMode.REQUIRED) double latitude,
		@Schema(description = "경도 (지도 표시)", requiredMode = Schema.RequiredMode.REQUIRED) double longitude,
		@Schema(description = "원문 링크") String sourceUrl) {
}
