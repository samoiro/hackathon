package com.hackathon.server.analysis.evidence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.hackathon.server.analysis.dto.OperationType;

/**
 * 분석 한 건의 가게 조건. 분석 서비스가 1단계(가게 정보 확인)에서 만들어 근거 수집기들에 넘긴다.
 * baseDate는 baseTime의 한국 날짜이고, 행사·시즌·준비 기한은 모두 이 날짜부터 센다.
 */
public record StoreContext(
		UUID analysisId,
		OffsetDateTime baseTime,
		LocalDate baseDate,
		double latitude,
		double longitude,
		String gu,
		String adminDongCode,
		long industryId,
		String industryCategory,
		String industryName,
		OperationType operationType,
		List<Long> ingredientIds,
		List<Long> supplyIds) {
}
