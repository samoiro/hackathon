package com.hackathon.server.analysis.evidence;

import java.util.List;

import com.hackathon.server.analysis.dto.SourceResponse;

/** 3단계(품목 가격 조회)의 원자재 근거. 고른 원자재 전부를 가격 자료가 없어도 넣는다. */
public record IngredientEvidenceResult(List<IngredientEvidence> items, List<SourceResponse> sources) {
}
