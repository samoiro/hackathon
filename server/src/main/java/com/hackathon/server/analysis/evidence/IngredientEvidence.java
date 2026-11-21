package com.hackathon.server.analysis.evidence;

import java.util.List;

import com.hackathon.server.analysis.dto.IngredientPriceResponse;

/** 사장님이 고른 원자재 한 개의 가격과 걸린 규칙. 근거 id는 수집기가 R1·R2…로 붙인다(price.evidenceId). */
public record IngredientEvidence(IngredientPriceResponse price, List<IngredientSignal> signals) {
}
