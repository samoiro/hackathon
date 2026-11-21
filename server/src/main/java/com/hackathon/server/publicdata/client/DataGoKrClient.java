package com.hackathon.server.publicdata.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.publicdata.dto.ListResponse;

/** 공공데이터포털(apis.data.go.kr) 목록 API 공통 호출. 응답의 response.body.items.item을 목록으로 돌려준다. */
@Component
public class DataGoKrClient {

	private final RestClient restClient;
	private final JsonMapper jsonMapper;
	private final String baseUrl;
	private final String apiKey;

	public DataGoKrClient(RestClient publicApiRestClient, JsonMapper jsonMapper,
			@Value("${data-go-kr.api.base-url}") String baseUrl, @Value("${data-go-kr.api.key}") String apiKey) {
		this.restClient = publicApiRestClient;
		this.jsonMapper = jsonMapper;
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
	}

	/** query: 파라미터 이름 → 값 (serviceKey 제외). 비어 있는 값은 보내지 않는다. */
	public ListResponse list(String path, Map<String, String> query) {
		if (!StringUtils.hasText(apiKey)) {
			throw new BusinessException(ErrorCode.PUBLIC_API_KEY_MISSING, "DATA_GO_KR_API_KEY가 설정되지 않았습니다.");
		}
		// 인증키에 +, /, = 가 들어 있어 값은 템플릿 변수로 넘겨 엄격하게 인코딩
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path)
				.queryParam("serviceKey", "{serviceKey}");
		Map<String, Object> vars = new HashMap<>();
		vars.put("serviceKey", apiKey);
		int i = 0;
		for (Map.Entry<String, String> e : query.entrySet()) {
			if (StringUtils.hasText(e.getValue())) {
				builder.queryParam(e.getKey(), "{v" + i + "}");
				vars.put("v" + i++, e.getValue());
			}
		}
		URI uri = builder.encode().buildAndExpand(vars).toUri();

		JsonNode root;
		try {
			root = jsonMapper.readTree(restClient.get().uri(uri).retrieve().body(String.class));
		}
		catch (RestClientResponseException e) {
			throw new BusinessException(ErrorCode.PUBLIC_API_ERROR, "공공데이터포털 API 오류: " + gatewayMessage(e.getResponseBodyAsString()));
		}
		catch (RestClientException e) {
			throw new BusinessException(ErrorCode.PUBLIC_API_CONNECTION_FAILED, "공공데이터포털 API 호출 실패: " + e.getMostSpecificCause().getMessage());
		}

		JsonNode response = root.has("response") ? root.get("response") : root;
		JsonNode body = response.path("body");
		if (body.isMissingNode()) {
			throw new BusinessException(ErrorCode.PUBLIC_API_ERROR, "공공데이터포털 API 오류: " + gatewayMessage(root.toString()));
		}
		JsonNode item = body.path("items").path("item");
		// 결과가 1건이면 배열이 아니라 객체로 오는 경우가 있음
		JsonNode items = item.isArray() ? item
				: item.isObject() ? jsonMapper.createArrayNode().add(item) : jsonMapper.createArrayNode();
		return new ListResponse(body.path("totalCount").asLong(0), items);
	}

	private String gatewayMessage(String body) {
		try {
			JsonNode root = jsonMapper.readTree(body);
			JsonNode header = root.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");
			if (header.has("returnAuthMsg")) {
				return header.get("returnAuthMsg").asString() + " (" + header.path("errMsg").asString() + ")";
			}
			JsonNode h = (root.has("response") ? root.get("response") : root).path("header");
			if (h.has("resultMsg")) {
				return h.get("resultMsg").asString() + " (" + h.path("resultCode").asString() + ")";
			}
		}
		catch (RuntimeException ignored) {
			// JSON이 아니면 아래에서 원문 일부를 돌려줌
		}
		return body.length() > 200 ? body.substring(0, 200) : body;
	}
}
