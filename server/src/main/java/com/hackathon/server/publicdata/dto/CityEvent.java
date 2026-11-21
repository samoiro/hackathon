package com.hackathon.server.publicdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 지도 핀 "축제 870m". 거리는 가게 좌표를 아는 프론트가 계산한다. */
@Schema(description = "장소 주변 문화행사")
public record CityEvent(
		@Schema(description = "행사 이름 (EVENT_NM)", example = "서울숲 재즈 페스티벌", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "위도 (EVENT_Y)", example = "37.5444", requiredMode = Schema.RequiredMode.REQUIRED) double latitude,
		@Schema(description = "경도 (EVENT_X)", example = "127.0374", requiredMode = Schema.RequiredMode.REQUIRED) double longitude) {
}
