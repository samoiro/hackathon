package com.hackathon.server.event.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 행사 시간 문구(PRO_TIME)에서 읽은 시간대와 붐비는 시간. 붐비는 시간은 시작 1시간 전 ~ 끝 1시간 후를 정시로 넓힌 "15~20시".
 * 여러 회차("11.27 19:30 / 11.28 16:00")는 가장 이른 시작 ~ 가장 늦은 끝으로 본다.
 *
 * @param timeBand MORNING(11시 전 시작) · LUNCH(11~14시) · AFTERNOON(14~17시) · EVENING(17시부터) · UNKNOWN
 * @param peakLabel 못 읽으면 null
 */
public record EventTime(String timeBand, String peakLabel) {

	static final EventTime UNKNOWN = new EventTime("UNKNOWN", null);
	private static final Pattern TIME = Pattern.compile("(?<!\\d)(\\d{1,2}):(\\d{2})");
	private static final Pattern RANGE = Pattern.compile("(?<!\\d)(\\d{1,2}):(\\d{2})\\s*[~\\-–—〜∼]\\s*(\\d{1,2}):(\\d{2})");
	// shortcut: 시작 시각만 적힌 공연은 2시간짜리로 본다, 끝 시각을 적는 문구가 많아지면 분류별로 나눈다
	private static final int DEFAULT_MINUTES = 120;

	// shortcut: "19:30"처럼 시:분만 읽는다("오후 7시"는 UNKNOWN), 그런 문구가 많으면 "N시"도 읽는다
	public static EventTime parse(String text) {
		if (text == null) return UNKNOWN;
		int start = Integer.MAX_VALUE;
		int end = -1;
		Matcher range = RANGE.matcher(text);
		while (range.find()) {
			int s = minutes(range.group(1), range.group(2));
			int e = minutes(range.group(3), range.group(4));
			if (s < 0 || e < s) continue;
			start = Math.min(start, s);
			end = Math.max(end, e);
		}
		if (end < 0) { // 범위가 없으면 시작 시각들만 있는 회차 공연
			Matcher time = TIME.matcher(text);
			while (time.find()) {
				int s = minutes(time.group(1), time.group(2));
				if (s < 0) continue;
				start = Math.min(start, s);
				end = Math.max(end, s + DEFAULT_MINUTES);
			}
		}
		if (end < 0) return UNKNOWN;
		int peakStart = Math.max(0, (start - 60) / 60);
		int peakEnd = Math.min(24, (end + 60 + 59) / 60);
		return new EventTime(band(start), peakStart + "~" + peakEnd + "시");
	}

	private static String band(int startMinutes) {
		int hour = startMinutes / 60;
		return hour < 11 ? "MORNING" : hour < 14 ? "LUNCH" : hour < 17 ? "AFTERNOON" : "EVENING";
	}

	/** 하루 안의 분. 시각이 아니면 -1 */
	private static int minutes(String hour, String minute) {
		int h = Integer.parseInt(hour);
		int m = Integer.parseInt(minute);
		return h > 24 || m > 59 || (h == 24 && m > 0) ? -1 : h * 60 + m;
	}
}
