package com.hackathon.server.analysis.evidence;

/** 행사·시즌·날씨 근거를 모은다. 기준: ai-server/docs/API_CONTRACT_DRAFT.md "행사 후보 (Java)" */
public interface EventEvidenceCollector {

	EventEvidence collect(StoreContext store);
}
