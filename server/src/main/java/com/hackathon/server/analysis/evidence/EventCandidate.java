package com.hackathon.server.analysis.evidence;

import com.hackathon.server.analysis.dto.EventResponse;

/**
 * 행사 후보 한 건. 화면용 EventResponse에 AI 요청에만 쓰는 값을 더한다.
 *
 * @param timeBand 시간 문구(PRO_TIME)에서 읽은 시간대 MORNING·LUNCH·AFTERNOON·EVENING, 못 읽으면 UNKNOWN
 * @param peakLabel 붐비는 시간(시작 1시간 전 ~ 끝 1시간 후) 화면 문구 "15~20시". 못 읽으면 null
 */
public record EventCandidate(EventResponse event, String timeBand, String peakLabel) {
}
