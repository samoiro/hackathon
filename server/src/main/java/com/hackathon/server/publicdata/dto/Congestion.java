package com.hackathon.server.publicdata.dto;

import java.util.Arrays;

import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;

/** 서울시 혼잡도 (AREA_CONGEST_LVL, FCST_CONGEST_LVL): 여유 · 보통 · 약간 붐빔 · 붐빔. */
public enum Congestion {
	RELAXED("여유"), NORMAL("보통"), SLIGHTLY_CROWDED("약간 붐빔"), CROWDED("붐빔");

	private final String label;

	Congestion(String label) {
		this.label = label;
	}

	public static Congestion fromLabel(String label) {
		return Arrays.stream(values()).filter(c -> c.label.equals(label)).findFirst()
				.orElseThrow(() -> new BusinessException(ErrorCode.PUBLIC_API_INVALID_RESPONSE, "알 수 없는 혼잡도: " + label));
	}
}
