package com.hackathon.server.analysis;

/** 분석 전체 상태. SUCCEEDED·FAILED가 되면 폴링을 멈춘다. */
public enum AnalysisStatus {
	PENDING, RUNNING, SUCCEEDED, FAILED
}
