package com.hackathon.server.analysis;

/** 가격 흐름: 상승 · 하락 · 변동 없음 (change1w 기준, ±1% 미만이면 FLAT. 원자재 강추는 change1w.percent 절대값이 가장 큰 품목 (순서도 13)). */
public enum Trend {
	UP, DOWN, FLAT
}
