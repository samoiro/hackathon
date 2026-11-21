package com.hackathon.server.publicdata.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.publicdata.dto.AdminDong;

/** 카카오 로컬 API. 인증키는 "Authorization: KakaoAK {REST API 키}" 헤더로 보낸다. */
@Component
public class KakaoLocalClient {

	private final RestClient restClient;
	private final JsonMapper jsonMapper;
	private final String baseUrl;
	private final String apiKey;

	public KakaoLocalClient(RestClient publicApiRestClient, JsonMapper jsonMapper,
			@Value("${kakao.api.base-url}") String baseUrl, @Value("${kakao.api.key}") String apiKey) {
		this.restClient = publicApiRestClient;
		this.jsonMapper = jsonMapper;
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
	}

	/** 좌표 → 행정동. 바다·국외처럼 행정구역이 없는 좌표면 null. */
	public AdminDong adminDong(double latitude, double longitude) {
		if (!StringUtils.hasText(apiKey)) {
			throw new BusinessException(ErrorCode.PUBLIC_API_KEY_MISSING, "KAKAO_REST_API_KEY가 설정되지 않았습니다.");
		}
		String body;
		try {
			// 카카오는 x = 경도, y = 위도
			body = restClient.get()
					.uri(baseUrl + "/v2/local/geo/coord2regioncode.json?x={x}&y={y}", longitude, latitude)
					.header("Authorization", "KakaoAK " + apiKey)
					.retrieve()
					.body(String.class);
		}
		catch (RestClientResponseException e) {
			// 400: 좌표 형식 오류, 401·403: 키·권한 문제(서버 설정 문제라 502)
			ErrorCode errorCode = e.getStatusCode().value() == 400 ? ErrorCode.PUBLIC_API_BAD_REQUEST : ErrorCode.PUBLIC_API_ERROR;
			throw new BusinessException(errorCode, "카카오 API 오류 (HTTP " + e.getStatusCode().value() + "): "
					+ errorMessage(e.getResponseBodyAsString()));
		}
		catch (RestClientException e) {
			throw new BusinessException(ErrorCode.PUBLIC_API_CONNECTION_FAILED, "카카오 API 호출 실패: " + e.getMostSpecificCause().getMessage());
		}
		if (body == null || body.isBlank()) {
			throw new BusinessException(ErrorCode.PUBLIC_API_INVALID_RESPONSE, "카카오 API 응답이 비어 있습니다.");
		}
		for (JsonNode doc : jsonMapper.readTree(body).path("documents")) {
			if ("H".equals(doc.path("region_type").asString(""))) {
				return new AdminDong(doc.path("code").asString(), doc.path("address_name").asString(),
						doc.path("region_1depth_name").asString(), doc.path("region_2depth_name").asString(),
						doc.path("region_3depth_name").asString());
			}
		}
		return null;
	}

	private String errorMessage(String body) {
		try {
			JsonNode message = jsonMapper.readTree(body).path("message");
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
