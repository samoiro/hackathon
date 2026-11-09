package com.hackathon.server.publicdata;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 서울 열린데이터광장 Open API 호출.
 * URL 형식: {base}/{key}/json/{서비스}/{시작}/{끝}/{선택인자...} (한 번에 최대 1000건)
 * 서비스마다 결과코드 위치가 달라서(RESULT.CODE / "RESULT.CODE" / resultCode, 가끔 XML) 여기서 한 번에 처리합니다.
 */
@Component
public class SeoulOpenApiClient {

	private static final Pattern XML_CODE = Pattern.compile("<CODE>(.*?)</CODE>.*?<MESSAGE><!\\[CDATA\\[(.*?)]]>", Pattern.DOTALL);

	private final RestClient restClient;
	private final JsonMapper jsonMapper;
	private final String baseUrl;
	private final String apiKey;

	public SeoulOpenApiClient(RestClient publicApiRestClient, JsonMapper jsonMapper,
			@Value("${seoul.api.base-url}") String baseUrl, @Value("${seoul.api.key}") String apiKey) {
		this.restClient = publicApiRestClient;
		this.jsonMapper = jsonMapper;
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
	}

	/** 목록형 API. params는 서비스의 선택 인자 순서대로 넘기고, 안 쓰는 값은 null. */
	public ListResponse list(String service, int page, int size, String... params) {
		int start = (page - 1) * size + 1;
		JsonNode body = call(service, start, start + size - 1, params).path(service);
		JsonNode rows = body.has("row") ? body.get("row") : jsonMapper.createArrayNode(); // INFO-200(데이터 없음)
		return new ListResponse(body.path("list_total_count").asLong(0), rows);
	}

	/** 실시간 도시데이터 계열 API (장소명 또는 장소코드 하나). */
	public JsonNode realtime(String service, String area) {
		return call(service, 1, 5, area);
	}

	private JsonNode call(String service, int start, int end, String... params) {
		if (!StringUtils.hasText(apiKey)) {
			throw PublicApiErrors.of(HttpStatus.SERVICE_UNAVAILABLE, "SEOUL_API_KEY가 설정되지 않았습니다.");
		}
		String body;
		try {
			body = restClient.get().uri(buildUri(service, start, end, params)).retrieve().body(String.class);
		}
		catch (RestClientException e) {
			// 예외 메시지에 인증키가 든 URL이 포함되므로 원인만 노출
			throw PublicApiErrors.of(HttpStatus.BAD_GATEWAY, "서울시 API 호출 실패: " + e.getMostSpecificCause().getMessage());
		}
		if (body == null || body.isBlank()) {
			throw PublicApiErrors.of(HttpStatus.BAD_GATEWAY, "서울시 API 응답이 비어 있습니다.");
		}
		if (body.startsWith("<")) {
			Matcher m = XML_CODE.matcher(body);
			throw m.find() ? error(m.group(1), m.group(2).trim())
					: PublicApiErrors.of(HttpStatus.BAD_GATEWAY, "서울시 API가 알 수 없는 형식으로 응답했습니다.");
		}
		JsonNode root = jsonMapper.readTree(body);
		JsonNode result = root.path(service).path("RESULT");
		if (result.isMissingNode()) {
			result = root.has("RESULT") ? root.get("RESULT") : root;
		}
		String code = firstText(result, "CODE", "RESULT.CODE", "resultCode");
		if (code != null && !code.equals("INFO-000") && !code.equals("INFO-200")) {
			throw error(code, firstText(result, "MESSAGE", "RESULT.MESSAGE", "resultMsg"));
		}
		return root;
	}

	private URI buildUri(String service, int start, int end, String... params) {
		StringBuilder path = new StringBuilder(baseUrl).append('/').append(apiKey)
				.append("/json/").append(service).append('/').append(start).append('/').append(end);
		int last = params.length;
		while (last > 0 && !StringUtils.hasText(params[last - 1])) {
			last--;
		}
		for (int i = 0; i < last; i++) {
			// 중간 인자를 비울 땐 공백(%20)을 넣어야 뒤 인자의 위치가 유지됨
			String value = StringUtils.hasText(params[i]) ? params[i] : " ";
			path.append('/').append(UriUtils.encodePathSegment(value, StandardCharsets.UTF_8));
		}
		return URI.create(path.toString());
	}

	private static ErrorResponseException error(String code, String message) {
		// ERROR-3xx는 요청 인자 문제, 나머지(인증키·서버 오류 등)는 서울시 쪽 문제
		HttpStatus status = code.startsWith("ERROR-3") ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
		return PublicApiErrors.of(status, "서울시 API 오류 [" + code + "] " + message);
	}

	private static String firstText(JsonNode node, String... fields) {
		for (String field : fields) {
			if (node.hasNonNull(field)) {
				return node.get(field).asString();
			}
		}
		return null;
	}
}
