package com.hackathon.server.event.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hackathon.server.analysis.dto.EventSearch;
import com.hackathon.server.analysis.dto.EventSource;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.EventEvidenceCollector;
import com.hackathon.server.analysis.evidence.StoreContext;

/** 행사·시즌·날씨 근거. shortcut: 아직 빈 근거만 돌려준다, 행사 근거 작업에서 구현으로 바꾼다 */
@Service
public class EventEvidenceService implements EventEvidenceCollector {

	@Override
	public EventEvidence collect(StoreContext store) {
		return new EventEvidence(new EventSearch(7, store.baseDate(), 0, EventSource.NONE), List.of(), List.of(), null, List.of());
	}
}
