package com.hackathon.server.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.hackathon.server.location.dto.LocationValidateRequest;
import com.hackathon.server.location.dto.LocationValidateResponse;
import com.hackathon.server.publicdata.client.KakaoLocalClient;
import com.hackathon.server.publicdata.dto.AdminDong;

class LocationServiceTest {

	private final KakaoLocalClient kakao = mock(KakaoLocalClient.class);
	private final LocationService service = new LocationService(kakao);

	@Test
	void seoulAddressIsValid() {
		when(kakao.adminDong(37.5446, 127.0446))
				.thenReturn(new AdminDong("1120065000", "서울특별시 성동구 성수1가1동", "서울특별시", "성동구", "성수1가1동"));

		assertThat(service.validate(new LocationValidateRequest("서울 성동구 서울숲2길 32", 37.5446, 127.0446)))
				.isEqualTo(new LocationValidateResponse(true, null));
	}

	@Test
	void outsideSeoulIsInvalidWithMessage() {
		when(kakao.adminDong(37.4138, 127.5183))
				.thenReturn(new AdminDong("4161025000", "경기도 광주시 경안동", "경기도", "광주시", "경안동"));

		assertThat(service.validate(new LocationValidateRequest("경기 광주시 경안로 1", 37.4138, 127.5183)))
				.isEqualTo(new LocationValidateResponse(false, "서울 밖 주소는 아직 지원하지 않아요."));
	}

	@Test
	void unknownRegionIsInvalid() {
		// 바다 위처럼 카카오가 행정동을 못 찾는 좌표
		assertThat(service.validate(new LocationValidateRequest("어딘가", 33.0, 125.0)).valid()).isFalse();
	}
}
