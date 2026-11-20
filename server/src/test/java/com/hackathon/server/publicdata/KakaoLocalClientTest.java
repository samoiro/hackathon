package com.hackathon.server.publicdata;

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
import tools.jackson.databind.json.JsonMapper;

class KakaoLocalClientTest {

	private static final String BASE = "https://dapi.example.test";
	private static final String URL = BASE + "/v2/local/geo/coord2regioncode.json?x=127.0446&y=37.5446";

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final KakaoLocalClient client = new KakaoLocalClient(builder.build(), JsonMapper.builder().build(), BASE, "test-key");

	@Test
	void sendsLongitudeAsXAndPicksAdminDong() {
		// 카카오는 법정동(B)과 행정동(H)을 함께 준다. 케이웨더는 행정동 코드를 쓴다
		server.expect(requestTo(URL))
				.andExpect(header("Authorization", "KakaoAK test-key"))
				.andRespond(withSuccess("""
						{"meta": {"total_count": 2}, "documents": [
						{"region_type": "B", "code": "1120011400", "address_name": "서울특별시 성동구 성수동1가",
						 "region_1depth_name": "서울특별시", "region_2depth_name": "성동구", "region_3depth_name": "성수동1가"},
						{"region_type": "H", "code": "1120065000", "address_name": "서울특별시 성동구 성수1가1동",
						 "region_1depth_name": "서울특별시", "region_2depth_name": "성동구", "region_3depth_name": "성수1가1동"}]}""",
						MediaType.APPLICATION_JSON));

		AdminDong dong = client.adminDong(37.5446, 127.0446);

		assertThat(dong).isEqualTo(new AdminDong("1120065000", "서울특별시 성동구 성수1가1동", "서울특별시", "성동구", "성수1가1동"));
		server.verify();
	}

	@Test
	void noRegionIsNull() {
		// 바다·국외 좌표
		server.expect(requestTo(URL))
				.andRespond(withSuccess("{\"meta\": {\"total_count\": 0}, \"documents\": []}", MediaType.APPLICATION_JSON));

		assertThat(client.adminDong(37.5446, 127.0446)).isNull();
	}

	@Test
	void badCoordinateIsBadRequestWithReason() {
		server.expect(requestTo(URL))
				.andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON)
						.body("{\"errorType\": \"InvalidArgument\", \"message\": \"x is out of range\"}"));

		var error = assertThrows(ErrorResponseException.class, () -> client.adminDong(37.5446, 127.0446));

		assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(error.getBody().getDetail()).contains("x is out of range");
	}

	@Test
	void wrongKeyIsBadGateway() {
		server.expect(requestTo(URL))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
						.body("{\"errorType\": \"AccessDeniedError\", \"message\": \"cannot find appKey\"}"));

		var error = assertThrows(ErrorResponseException.class, () -> client.adminDong(37.5446, 127.0446));

		assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(error.getBody().getDetail()).contains("cannot find appKey");
	}

	@Test
	void missingKeyIsServiceUnavailable() {
		var noKey = new KakaoLocalClient(builder.build(), JsonMapper.builder().build(), BASE, "");
		var error = assertThrows(ErrorResponseException.class, () -> noKey.adminDong(37.5446, 127.0446));
		assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}
}
