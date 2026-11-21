package com.hackathon.server.supply.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.analysis.evidence.SupplyEvidence;
import com.hackathon.server.analysis.evidence.SupplyEvidenceCollector;

/** 부자재 준비 시점 근거. shortcut: 아직 빈 근거만 돌려준다, 원자재·부자재 근거 작업에서 구현으로 바꾼다 */
@Service
public class SupplyEvidenceService implements SupplyEvidenceCollector {

	@Override
	public List<SupplyEvidence> collect(StoreContext store, EventEvidence events) {
		return List.of();
	}
}
