package com.hackathon.server.event.dto;

import java.time.LocalDate;

/** cultural_event에 upsert할 한 줄. aiViewCulture 원본 한 건을 검증·변환한 값이다. rawPayload는 원본 JSON 문자열. */
public record CulturalEventRow(
		String sourceKey,
		String title,
		String category,
		String districtName,
		String place,
		LocalDate startDate,
		LocalDate endDate,
		String timeText,
		double latitude,
		double longitude,
		String targetAudience,
		String program,
		String notes,
		String feeText,
		Boolean isFree,
		String sourceUrl,
		String imageUrl,
		String rawPayload) {
}
