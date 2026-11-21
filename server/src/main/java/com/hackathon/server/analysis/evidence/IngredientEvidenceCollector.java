package com.hackathon.server.analysis.evidence;

/** 고른 원자재의 최신 가격·4주 단계·걸린 규칙을 모은다. 기준: docs/database-schema.md ingredient_rule · ingredient_price_band */
public interface IngredientEvidenceCollector {

	IngredientEvidenceResult collect(StoreContext store);
}
