package com.hackathon.server.analysis.evidence;

import com.hackathon.server.analysis.dto.SupplyTimingResponse;

/**
 * 고른 부자재 한 개의 이번 주 준비 시점. 규칙이 안 걸렸으면 timing의 demand·prepareBy 등이 null이다.
 * 근거 id는 수집기가 S1·S2…로 붙이고, timing.triggerEvidenceId는 EventEvidence의 E·H·W id를 가리킨다.
 *
 * @param kind SUPPLY(부자재) · STOCK(손님에게 파는 상품). 규칙이 안 걸렸으면 null
 * @param forecast 날씨 예보 기준 규칙이면 true (강추 확실성 2점, 날짜 기준이면 3점)
 */
public record SupplyEvidence(SupplyTimingResponse timing, String kind, boolean forecast) {
}
