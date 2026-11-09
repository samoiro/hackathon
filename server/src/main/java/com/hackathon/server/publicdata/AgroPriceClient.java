package com.hackathon.server.publicdata;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 공공데이터포털(data.go.kr) 한국농수산식품유통공사 가격 추이 정보 (B552845/priceSequel). */
@Component
public class AgroPriceClient {

	private final RestClient restClient;
	private final JsonMapper jsonMapper;
	private final String baseUrl;
	private final String apiKey;

	public AgroPriceClient(RestClient publicApiRestClient, JsonMapper jsonMapper,
			@Value("${data-go-kr.api.base-url}") String baseUrl, @Value("${data-go-kr.api.key}") String apiKey) {
		this.restClient = publicApiRestClient;
		this.jsonMapper = jsonMapper;
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
	}

	/** conditions: 컬럼명(se_cd, ctgry_cd, item_cd, vrty_cd, grd_cd) → 값. null 값은 조건에서 제외. */
	public ListResponse priceTrends(String date, Map<String, String> conditions, int page, int size) {
		if (!StringUtils.hasText(apiKey)) {
			throw PublicApiErrors.of(HttpStatus.SERVICE_UNAVAILABLE, "DATA_GO_KR_API_KEY가 설정되지 않았습니다.");
		}
		Map<String, String> conds = new LinkedHashMap<>();
		conds.put("exmn_ymd", date);
		conditions.forEach((k, v) -> {
			if (StringUtils.hasText(v)) {
				conds.put(k, v);
			}
		});
		// 인증키에 +, /, = 가 들어 있어 값은 템플릿 변수로 넘겨 엄격하게 인코딩
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + "/B552845/priceSequel/info")
				.queryParam("serviceKey", "{serviceKey}")
				.queryParam("pageNo", page)
				.queryParam("numOfRows", size)
				.queryParam("returnType", "JSON");
		Map<String, Object> vars = new HashMap<>();
		vars.put("serviceKey", apiKey);
		conds.forEach((k, v) -> {
			builder.queryParam("cond[" + k + "::EQ]", "{" + k + "}");
			vars.put(k, v);
		});
		URI uri = builder.encode().buildAndExpand(vars).toUri();

		JsonNode root;
		try {
			root = jsonMapper.readTree(restClient.get().uri(uri).retrieve().body(String.class));
		}
		catch (RestClientResponseException e) {
			throw PublicApiErrors.of(HttpStatus.BAD_GATEWAY, "공공데이터포털 API 오류: " + gatewayMessage(e.getResponseBodyAsString()));
		}
		catch (RestClientException e) {
			throw PublicApiErrors.of(HttpStatus.BAD_GATEWAY, "공공데이터포털 API 호출 실패: " + e.getMostSpecificCause().getMessage());
		}

		JsonNode response = root.has("response") ? root.get("response") : root;
		JsonNode body = response.path("body");
		if (body.isMissingNode()) {
			throw PublicApiErrors.of(HttpStatus.BAD_GATEWAY, "공공데이터포털 API 오류: " + gatewayMessage(root.toString()));
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
