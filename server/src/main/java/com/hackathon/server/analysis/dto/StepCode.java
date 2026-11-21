package com.hackathon.server.analysis.dto;

/**
 * 분석 중 화면 4단계: 가게 정보 확인 · 주변 행사 조회 · 품목 가격 조회 · 준비할 일 정리.
 * EVENT_SEARCH는 순서도의 행사 트랙 전체(3 행사 → 5 시즌 행사 → 3-1 날씨)를 맡고, 어디서 멈췄는지는 EventSearch.source로 알린다.
 */
public enum StepCode {
	STORE_CHECK, EVENT_SEARCH, PRICE_LOOKUP, TASK_GENERATION
}
