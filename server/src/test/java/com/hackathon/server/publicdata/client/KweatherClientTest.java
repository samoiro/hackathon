package com.hackathon.server.publicdata.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class KweatherClientTest {

	private static final String BASE = "https://apigw.example.test";

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final KweatherClient client = new KweatherClient(builder.build(), JsonMapper.builder().build(), BASE, "test-key");

	@Test
	void sendsKeyHeaderAndReturnsDataObject() {
		server.expect(requestTo(BASE + "/kw-fct-wl1/1111051500"))
				.andExpect(header("Kweather-Api-Key", "test-key"))
				.andRespond(withSuccess("""
						{"error": "0", "message": "정상", "data": {"1111051500": {"service": {"timestamp": "202611201800"},
						"data": {"city2": "청운효자동", "taMax": [3, 3]}}}}""", MediaType.APPLICATION_JSON));

		JsonNode data = client.get("kw-fct-wl1", "1111051500");

		assertThat(data.path("1111051500").path("data").path("city2").asString()).isEqualTo("청운효자동");
		server.verify();
	}

	@Test
	void omittedCodeQueriesAll() {
		server.expect(requestTo(BASE + "/kw-warning2/"))
				.andRespond(withSuccess("{\"error\": \"0\", \"message\": \"정상\", \"data\": {}}", MediaType.APPLICATION_JSON));
		client.get("kw-warning2", null);
		server.verify();
	}

	@Test
	void noDataIsEmptyObject() {
		// 발효 중인 특보가 없는 지역은 HTTP 206 + error 001
		server.expect(requestTo(BASE + "/kw-warning1/1111051500"))
				.andRespond(withStatus(HttpStatus.PARTIAL_CONTENT).contentType(MediaType.APPLICATION_JSON)
						.body("{\"error\": \"001\", \"message\": \"데이터를 찾을 수 없음\"}"));

		assertThat(client.get("kw-warning1", "1111051500").isEmpty()).isTrue();
	}

	@Test
	void unknownCodeIsBadRequestWithReason() {
		server.expect(requestTo(BASE + "/kw-warning1/0000000000"))
				.andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON).body("""
						{"message":"{\\"message\\":\\"Request is not valid according to constraint rules\\",\\"constraints\\":[\\"없는 코드입니다. 코드표에서 확인해 주세요.\\"]}","http_status_code":400}"""));

		var error = assertThrows(ErrorResponseException.class, () -> client.get("kw-warning1", "0000000000"));

		assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(error.getBody().getDetail()).contains("없는 코드입니다");
	}

	@Test
	void missingKeyIsServiceUnavailable() {
		var noKey = new KweatherClient(builder.build(), JsonMapper.builder().build(), BASE, "");
		var error = assertThrows(ErrorResponseException.class, () -> noKey.get("kw-fct-wl1", "1111051500"));
		assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}
}
