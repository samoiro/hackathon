package com.hackathon.server.analysis.evidence;

import java.util.List;

import com.hackathon.server.analysis.dto.EventSearch;
import com.hackathon.server.analysis.dto.SeasonEventResponse;
import com.hackathon.server.analysis.dto.SourceResponse;
import com.hackathon.server.analysis.dto.WeatherResponse;

/**
 * 행사 트랙 근거(2단계 주변 행사 조회). EVENT 칸은 search.source가 가리키는 것 하나만 쓴다:
 * 주변 행사(events) → 없으면 시즌 행사(seasonEvents) → 없으면 날씨(weather) → 없음.
 * seasonEvents와 weather는 부자재 준비표 트리거에도 쓰므로 events가 있어도 채운다. 날씨를 못 받으면 weather는 null.
 * 근거 id는 수집기가 붙인다: 행사 E1·E2…, 시즌 H1…, 날씨 W1.
 */
public record EventEvidence(
		EventSearch search,
		List<EventCandidate> events,
		List<SeasonEventResponse> seasonEvents,
		WeatherResponse weather,
		List<SourceResponse> sources) {
}
