package com.hackathon.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 공공API 요청 로그에 인증키가 남지 않아야 한다. */
class PublicApiConfigTest {

	@Test
	void masksDataGoKrServiceKeyQuery() {
		String uri = "https://apis.data.go.kr/B552895/svc?pageNo=1&serviceKey=abc%2Bdef%3D&numOfRows=10";

		assertThat(PublicApiConfig.mask(uri, ""))
				.isEqualTo("https://apis.data.go.kr/B552895/svc?pageNo=1&serviceKey=****&numOfRows=10");
	}

	@Test
	void masksSeoulKeyInPath() {
		String uri = "http://openapi.seoul.go.kr:8088/seoul-secret/json/citydata/1/5/광화문";

		assertThat(PublicApiConfig.mask(uri, "seoul-secret"))
				.isEqualTo("http://openapi.seoul.go.kr:8088/****/json/citydata/1/5/광화문");
	}
}
