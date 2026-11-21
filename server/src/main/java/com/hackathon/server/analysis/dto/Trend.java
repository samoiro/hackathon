package com.hackathon.server.analysis.dto;

/** 가격 흐름: 상승 · 하락 · 변동 없음 (change1w 기준, ±1% 미만이면 FLAT. 강추 점수가 같으면 change1w.percent 절대값이 큰 품목 먼저). */
public enum Trend {
	UP, DOWN, FLAT
}
