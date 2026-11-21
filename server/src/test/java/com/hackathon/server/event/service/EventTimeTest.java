package com.hackathon.server.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EventTimeTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', nullValues = "null", value = {
			"16:00–19:00                              | AFTERNOON | 15~20시",
			"10:00~17:00, 매일 ※매주 월요일 휴관        | MORNING   | 9~18시",
			"11.27.(금) 19:30 / 11.28.(토) 16:00       | AFTERNOON | 15~23시", // 가장 이른 16시 ~ 늦은 회차 19:30 + 2시간
			"[6~7세] 10:30, [8~9세] 11:30              | MORNING   | 9~15시",
			"12:00                                    | LUNCH     | 11~15시",
			"화-토 11:00~19:00 (일, 월 휴무)            | LUNCH     | 10~20시",
			"19:30 ~ 21:00                            | EVENING   | 18~22시",
			"각 공연 / 축제별 상이                       | UNKNOWN   | null",
			"null                                     | UNKNOWN   | null" })
	void readsBandAndPeak(String text, String band, String peak) {
		assertThat(EventTime.parse(text)).isEqualTo(new EventTime(band, peak));
	}
}
