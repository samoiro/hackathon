package com.hackathon.server.publicdata.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.publicdata.dto.ListResponse;

class DataGoKrClientTest {

	private static final String BASE = "https://apis.example.test";
	private static final String PATH = "/1421000/bizinfo/pblancBsnsService";

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final DataGoKrClient client = new DataGoKrClient(builder.build(), JsonMapper.builder().build(), BASE, "key+with/special=");

	@Test
	void encodesKeyAndValuesAndSkipsBlankParams() {
		server.expect(requestTo(containsString("serviceKey=key%2Bwith%2Fspecial%3D")))
				.andExpect(requestTo(containsString("hashtags=%EC%84%9C%EC%9A%B8%2C%EC%86%8C%EC%83%81%EA%B3%B5%EC%9D%B8")))
				.andExpect(requestTo(not(containsString("pblancId"))))
				.andRespond(withSuccess("""
						{"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},"body":{"items":{"item":[
						{"pblancNm":"2026년 소상공인 경영개선 지원사업","pblancId":"PBLN_1","reqstBeginEndDe":"2026-11-18 ~ 2026-11-25"},
						{"pblancNm":"[서울] 소상공인 판로 지원","pblancId":"PBLN_2","reqstBeginEndDe":"예산 소진시까지"}]},
						"numOfRows":2,"pageNo":1,"totalCount":207}}}""", MediaType.APPLICATION_JSON));
		Map<String, String> query = new LinkedHashMap<>();
		query.put("hashtags", "서울,소상공인");
		query.put("pblancId", null);

		ListResponse response = client.list(PATH, query);

		assertThat(response.totalCount()).isEqualTo(207);
		assertThat(response.items()).hasSize(2);
		assertThat(response.items().get(1).path("pblancId").asString()).isEqualTo("PBLN_2");
		server.verify();
	}

	@Test
	void singleItemObjectBecomesArray() {
		server.expect(requestTo(containsString(PATH))).andRespond(withSuccess("""
				{"response":{"body":{"items":{"item":{"pblancId":"PBLN_1"}},"totalCount":1}}}""", MediaType.APPLICATION_JSON));

		assertThat(client.list(PATH, Map.of()).items()).hasSize(1);
	}

	@Test
	void noDataIsEmptyList() {
		// 조건에 맞는 공고가 없으면 resultCode 03 + items null
		server.expect(requestTo(containsString(PATH))).andRespond(withSuccess("""
				{"response":{"header":{"resultCode":"03","resultMsg":"NODATA_ERROR"},"body":{"items":null,"totalCount":0}}}""",
				MediaType.APPLICATION_JSON));

		ListResponse response = client.list(PATH, Map.of());

		assertThat(response.totalCount()).isZero();
		assertThat(response.items()).isEmpty();
	}

	@Test
	void gatewayErrorIsBadGatewayWithReason() {
		server.expect(requestTo(containsString(PATH))).andRespond(withSuccess("""
				{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"NO_OPENAPI_SERVICE_ERROR","returnAuthMsg":"해당 오픈API 서비스가 없거나 폐기됨","returnReasonCode":"12"}}}""",
				MediaType.APPLICATION_JSON));

		var error = assertThrows(ErrorResponseException.class, () -> client.list(PATH, Map.of()));

		assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(error.getBody().getDetail()).contains("NO_OPENAPI_SERVICE_ERROR");
	}

	@Test
	void httpErrorIsBadGateway() {
		server.expect(requestTo(containsString(PATH))).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
		var error = assertThrows(ErrorResponseException.class, () -> client.list(PATH, Map.of()));
		assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
	}

	@Test
	void missingKeyIsServiceUnavailable() {
		var noKey = new DataGoKrClient(builder.build(), JsonMapper.builder().build(), BASE, "");
		var error = assertThrows(ErrorResponseException.class, () -> noKey.list(PATH, Map.of()));
		assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}
}
