package com.hackathon.server.ingredient.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hackathon.server.analysis.evidence.IngredientEvidenceCollector;
import com.hackathon.server.analysis.evidence.IngredientEvidenceResult;
import com.hackathon.server.analysis.evidence.StoreContext;

/** 원자재 가격 근거. shortcut: 아직 빈 근거만 돌려준다, 원자재·부자재 근거 작업에서 구현으로 바꾼다 */
@Service
public class IngredientEvidenceService implements IngredientEvidenceCollector {

	@Override
	public IngredientEvidenceResult collect(StoreContext store) {
		return new IngredientEvidenceResult(List.of(), List.of());
	}
}
