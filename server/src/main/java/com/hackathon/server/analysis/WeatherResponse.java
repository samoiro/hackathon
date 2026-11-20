package com.hackathon.server.analysis;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** 순서도 3-1: 매장 주소(행정동) 기준 향후 7일 날씨. 케이웨더 기상특보 + 단기예보 + 주간예보. */
@Schema(description = "날씨")
public record WeatherResponse(
		@Schema(description = "근거 id", example = "W1", requiredMode = Schema.RequiredMode.REQUIRED) String evidenceId,
		@Schema(description = "행정동 코드 (10자리)", example = "1120011400", requiredMode = Schema.RequiredMode.REQUIRED) String regionCode,
		@Schema(description = "영업에 영향 주는 날씨. 비어 있으면 날씨 후보 없음", requiredMode = Schema.RequiredMode.REQUIRED) List<WeatherImpact> impacts,
		@Schema(description = "발효 중인 기상특보", requiredMode = Schema.RequiredMode.REQUIRED) List<WeatherWarning> warnings,
		@Schema(description = "향후 7일 예보, 시각 순 (0~2일 후는 단기예보 kw-fct-dl, 3일 후부터는 주간예보 kw-fct-wl)", requiredMode = Schema.RequiredMode.REQUIRED) List<WeatherForecast> forecasts,
		@Schema(description = "케이웨더 확인 시각", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime checkedAt) {
}
