package com.hackathon.server.analysis.evidence;

/**
 * 이번 주 걸린 ingredient_rule 한 개 (가격 단계, 제철, 제철 한 달 전, 김장·설·추석 전).
 *
 * @param demand UP · STRONG_UP · DOWN · STRONG_DOWN
 */
public record IngredientSignal(String ruleCode, String text, String demand) {
}
