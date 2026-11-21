package com.hackathon.server.config;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** 외부 공공데이터 API 호출용 RestClient (응답이 늦어도 무한정 기다리지 않도록 타임아웃 설정). */
@Slf4j
@Configuration
public class PublicApiConfig {

	@Bean
	public RestClient publicApiRestClient(@Value("${seoul.api.key:}") String seoulApiKey) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(15));
		return RestClient.builder()
				.requestFactory(requestFactory)
				// 무료 인스턴스는 디스크가 작아 응답 본문은 남기지 않고 요청 URL·상태·소요시간만 남긴다
				.requestInterceptor((request, body, execution) -> {
					String uri = mask(request.getURI().toString(), seoulApiKey);
					log.info("========== [공공API 요청] {} {} ==========", request.getMethod(), uri);
					long start = System.currentTimeMillis();
					try {
						ClientHttpResponse response = execution.execute(request, body);
						log.info("========== [공공API 응답] {} {}ms {} ==========", response.getStatusCode().value(),
								System.currentTimeMillis() - start, uri);
						return response;
					}
					catch (IOException e) {
						log.warn("========== [공공API 실패] {}ms {} {} ==========", System.currentTimeMillis() - start, uri, e.toString());
						throw e;
					}
				})
				.build();
	}

	/** 인증키가 URL에 들어가는 API(data.go.kr 쿼리 serviceKey, 서울시 경로)의 키를 가린다. 헤더로 보내는 키는 로그에 안 남는다. */
	static String mask(String uri, String seoulApiKey) {
		String masked = uri.replaceAll("(?i)(serviceKey=)[^&]*", "$1****");
		return StringUtils.hasText(seoulApiKey) ? masked.replace(seoulApiKey, "****") : masked;
	}
}
