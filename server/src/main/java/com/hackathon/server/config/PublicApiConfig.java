package com.hackathon.server.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 외부 공공데이터 API 호출용 RestClient (응답이 늦어도 무한정 기다리지 않도록 타임아웃 설정). */
@Configuration
public class PublicApiConfig {

	@Bean
	public RestClient publicApiRestClient() {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(15));
		return RestClient.builder().requestFactory(requestFactory).build();
	}
}
