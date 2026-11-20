package com.hackathon.server.publicdata;

/** 서울시 혼잡도 (AREA_CONGEST_LVL, FCST_CONGEST_LVL): 여유 · 보통 · 약간 붐빔 · 붐빔. */
public enum Congestion {
	RELAXED, NORMAL, SLIGHTLY_CROWDED, CROWDED
}
