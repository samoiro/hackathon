package com.hackathon.server.analysis;

/** 영업에 영향 주는 날씨 (순서도 3-1 판정 기준). HEAVY_SNOW는 대설 특보, FINE_DUST는 도시데이터 미세먼지 "나쁨" 이상. */
public enum WeatherImpact {
	RAIN_SNOW, HEAVY_SNOW, COLD_WAVE, HEATWAVE, STRONG_WIND, FINE_DUST, WARNING
}
