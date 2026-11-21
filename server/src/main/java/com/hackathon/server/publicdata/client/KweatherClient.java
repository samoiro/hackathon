package com.hackathon.server.publicdata.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.common.exception.PublicApiErrors;

/**
 * 케이웨더 API 게이트웨이 호출. URL 형식: {base}/{api}/{행정구역코드 10자리, 생략하면 전체}
 * 응답: {"error": "0", "message": "정상", "data": {코드: {service, data}}}. 인증키는 Kweather-Api-Key 헤더로 보냅니다.
 */
@Component
public class KweatherClient {

	private final RestClient restClient;
	private final JsonMapper jsonMapper;
	private final String baseUrl;
	private final String apiKey;

	public KweatherClient(RestClient publicApiRestClient, JsonMapper jsonMapper,
			@Value("${kweather.api.base-url}") String baseUrl, @Value("${kweather.api.key}") String apiKey) {
		this.restClient = publicApiRestClient;
		this.jsonMapper = jsonMapper;
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
	}

	/** api: 게이트웨이 경로 (예: kw-warning1, kw-fct-wl2). 결과의 data 객체(코드 → 지역 데이터)를 돌려줍니다. */
	public JsonNode get(String api, String code) {
		if (!StringUtils.hasText(apiKey)) {
			throw PublicApiErrors.of(HttpStatus.SERVICE_UNAVAILABLE, "KWEATHER_API_KEY가 설정되지 않았습니다.");
		}
		String body;
		try {
			body = restClient.get()
					.uri(baseUrl + "/{api}/{code}", api, code == null ? "" : code)
					.header("Kweather-Api-Key", apiKey)
					.retrieve()
					.body(String.class);
		}
		catch (RestClientResponseException e) {
			// 400: 없는 행정구역코드 (constraints에 사유), 401: 인증키 오류
			HttpStatus status = e.getStatusCode().value() == 400 ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
			throw PublicApiErrors.of(status, "케이웨더 API 오류 (HTTP " + e.getStatusCode().value() + "): "
					+ gatewayMessage(e.getResponseBodyAsString()));
		}
		catch (RestClientException e) {
			throw PublicApiErrors.of(HttpStatus.BAD_GATEWAY, "케이웨더 API 호출 실패: " + e.getMostSpecificCause().getMessage());
		}
		if (body == null || body.isBlank()) {
			throw PublicApiErrors.of(HttpStatus.BAD_GATEWAY, "케이웨더 API 응답이 비어 있습니다.");
		}
		JsonNode root = jsonMapper.readTree(body);
		String error = root.path("error").asString("");
		if (error.equals("001")) {
			// 데이터를 찾을 수 없음 (HTTP 206): 그 지역에 발효 중인 특보가 없을 때도 이렇게 옵니다
			return jsonMapper.createObjectNode();
		}
		if (!error.equals("0")) {
			throw PublicApiErrors.of(HttpStatus.BAD_GATEWAY,
					"케이웨더 API 오류 [" + error + "] " + root.path("message").asString(""));
		}
		return root.path("data");
	}

	private String gatewayMessage(String body) {
		try {
			JsonNode root = jsonMapper.readTree(body);
			JsonNode message = root.path("message");
			// 400 응답은 message 안에 JSON 문자열이 한 번 더 들어 있음
			if (message.isString() && message.asString().startsWith("{")) {
				JsonNode inner = jsonMapper.readTree(message.asString());
				JsonNode constraints = inner.path("constraints");
				return constraints.isArray() && !constraints.isEmpty() ? constraints.get(0).asString()
						: inner.path("message").asString();
			}
			if (message.isString()) {
				return message.asString();
			}
		}
		catch (RuntimeException ignored) {
			// JSON이 아니면 아래에서 원문 일부를 돌려줌
		}
		return body.length() > 200 ? body.substring(0, 200) : body;
	}
}
