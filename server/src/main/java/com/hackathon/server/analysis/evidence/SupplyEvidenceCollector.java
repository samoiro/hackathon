package com.hackathon.server.analysis.evidence;

import java.util.List;

/** 고른 부자재마다 업종의 supply_rule 중 이번 주 걸린 것을 고른다. 트리거는 행사·시즌·날씨 근거에서 찾는다. */
public interface SupplyEvidenceCollector {

	List<SupplyEvidence> collect(StoreContext store, EventEvidence events);
}
