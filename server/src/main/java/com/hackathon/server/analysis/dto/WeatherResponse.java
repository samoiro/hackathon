package com.hackathon.server.analysis.dto;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** 순서도 3-1: 매장 좌표 기준 날씨. 기상청 단기예보(약 3~4일). */
@Schema(description = "날씨")
public record WeatherResponse(
		@Schema(description = "근거 id", example = "W1", requiredMode = Schema.RequiredMode.REQUIRED) String evidenceId,
		@Schema(description = "행정동 코드 (10자리)", example = "1120011400", requiredMode = Schema.RequiredMode.REQUIRED) String regionCode,
		@Schema(description = "영업에 영향 주는 날씨. 비어 있으면 날씨 후보 없음", requiredMode = Schema.RequiredMode.REQUIRED) List<WeatherImpact> impacts,
		@Schema(description = "발효 중인 기상특보. 지금은 받지 않아 늘 비어 있다", requiredMode = Schema.RequiredMode.REQUIRED) List<WeatherWarning> warnings,
		@Schema(description = "기상청 단기예보를 오전·오후로 묶은 예보, 시각 순 (발표 후 약 3~4일)", requiredMode = Schema.RequiredMode.REQUIRED) List<WeatherForecast> forecasts,
		@Schema(description = "날씨 확인 시각", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime checkedAt,
		@Schema(description = "impacts마다 처음 나타나는 날. 비어 있으면 시작일을 모름", requiredMode = Schema.RequiredMode.REQUIRED) List<WeatherImpactStart> impactStarts) {

	/** 시작일 없이 만든다. shortcut: 기존 호출처를 깨지 않으려고 둔다, 모든 호출처가 impactStarts를 넘기면 지운다 */
	public WeatherResponse(String evidenceId, String regionCode, List<WeatherImpact> impacts, List<WeatherWarning> warnings,
			List<WeatherForecast> forecasts, OffsetDateTime checkedAt) {
		this(evidenceId, regionCode, impacts, warnings, forecasts, checkedAt, List.of());
	}
}
