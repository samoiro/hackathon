package com.hackathon.server.event.dto;

import java.time.LocalDate;

/** 가게 반경 안의 행사 후보 한 줄 (cultural_event 조회 결과). distanceM은 가게까지 직선거리(미터). */
public record NearbyEvent(
		String title,
		String category,
		String place,
		LocalDate startDate,
		LocalDate endDate,
		String timeText,
		Boolean isFree,
		String sourceUrl,
		double latitude,
		double longitude,
		double distanceM) {
}
