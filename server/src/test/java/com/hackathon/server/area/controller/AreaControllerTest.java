package com.hackathon.server.area.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hackathon.server.area.dto.AreaResponse;
import com.hackathon.server.area.service.AreaService;
import com.hackathon.server.common.exception.GlobalExceptionHandler;
import com.hackathon.server.common.repository.ErrorLogRepository;

class AreaControllerTest {

	private final AreaService service = mock(AreaService.class);
	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AreaController(service))
			.setControllerAdvice(new GlobalExceptionHandler(mock(ErrorLogRepository.class))).build();

	@Test
	void sendsBoundaryAsGeoJsonObject() throws Exception {
		when(service.areas(37.5446, 127.0559)).thenReturn(List.of(new AreaResponse("T1", "테스트숲", "공원", 37.545, 127.045,
				"{\"type\":\"MultiPolygon\",\"coordinates\":[[[[127.04,37.54],[127.05,37.54],[127.05,37.55],[127.04,37.54]]]]}")));

		mvc.perform(get("/api/areas").param("latitude", "37.5446").param("longitude", "127.0559"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].boundary.type").value("MultiPolygon"))
				.andExpect(jsonPath("$[0].boundary.coordinates[0][0][0][0]").value(127.04));
	}

	@Test
	void rejectsLatitudeWithoutLongitude() throws Exception {
		mvc.perform(get("/api/areas").param("latitude", "37.5446"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(service);
	}
}
