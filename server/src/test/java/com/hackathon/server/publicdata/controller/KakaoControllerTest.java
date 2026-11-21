package com.hackathon.server.publicdata.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hackathon.server.publicdata.client.KakaoLocalClient;
import com.hackathon.server.publicdata.dto.AdminDong;

class KakaoControllerTest {

	private final KakaoLocalClient client = mock(KakaoLocalClient.class);
	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new KakaoController(client)).build();

	@Test
	void returnsAdminDong() throws Exception {
		when(client.adminDong(37.5446, 127.0446))
				.thenReturn(new AdminDong("1120065000", "서울특별시 성동구 성수1가1동", "서울특별시", "성동구", "성수1가1동"));

		mvc.perform(get("/api/kakao/admin-dong").param("latitude", "37.5446").param("longitude", "127.0446"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("1120065000"));
	}

	@Test
	void noRegionIsNotFound() throws Exception {
		mvc.perform(get("/api/kakao/admin-dong").param("latitude", "0").param("longitude", "0"))
				.andExpect(status().isNotFound());
	}
}
