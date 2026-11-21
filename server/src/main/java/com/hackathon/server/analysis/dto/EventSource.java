package com.hackathon.server.analysis.dto;

/** 행사 트랙이 어느 분기에서 후보를 만들었는지 (순서도 3 → 5 → 3-1). */
public enum EventSource {
	/** 3·4: 주변 문화행사가 있음 */
	CULTURAL_EVENT,
	/** 5: 행사가 없어서 시즌 행사(설·추석·크리스마스 등)로 대신함 */
	SEASON,
	/** 3-1: 시즌 행사도 없어서 영업에 영향 주는 날씨로 대신함 */
	WEATHER,
	/** 셋 다 없음: "조건에 맞는 예정 행사가 없어요" */
	NONE
}
