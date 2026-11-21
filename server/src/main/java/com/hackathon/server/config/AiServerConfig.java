package com.hackathon.server.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Python ai-server 호출용 RestClient. LLM 응답이 느려서 공공데이터용보다 읽기 타임아웃이 길다. */
@Configuration
public class AiServerConfig {

	@Bean
	public RestClient aiServerRestClient(@Value("${ai.server.base-url}") String baseUrl,
			@Value("${ai.server.token}") String token, @Value("${ai.server.read-timeout}") Duration readTimeout) {
		// JDK 기본 HTTP/2는 평문 http에서 h2c 업그레이드를 보내고, uvicorn이 이를 400으로 거부한다
		HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(readTimeout);
		RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory);
		if (StringUtils.hasText(token)) {
			builder.defaultHeader("X-Internal-Token", token);
		}
		return builder.build();
	}
}
