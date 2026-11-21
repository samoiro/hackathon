package com.hackathon.server.publicdata.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import com.hackathon.server.publicdata.client.SeoulOpenApiClient;

class SeoulDataControllerTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final SeoulOpenApiClient client = mock(SeoulOpenApiClient.class);
	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SeoulDataController(client)).build();

	@Test
	void cityDataKeepsOnlyScreenFields() throws Exception {
		when(client.realtime("citydata", "성수카페거리")).thenReturn(demo("citydata-seongsu-cafe-street"));

		mvc.perform(get("/api/seoul/city-data/{area}", "성수카페거리"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.areaCode").value("POI068"))
				.andExpect(jsonPath("$.congestion").exists())
				.andExpect(jsonPath("$.forecasts.length()").value(12))
				.andExpect(jsonPath("$.consumptionRatios.length()").value(6))
				.andExpect(jsonPath("$.consumptionRatios[2].ageGroup").value(30))
				.andExpect(jsonPath("$.consumptionRatios[2].percent").value(27.0))
				.andExpect(jsonPath("$.weather.sky").exists())
				.andExpect(jsonPath("$.ROAD_TRAFFIC_STTS").doesNotExist());
	}

	@Test
	void placeWithoutCommercialDataGivesEmptyRatiosAndEventPins() throws Exception {
		JsonNode root = demo("citydata-seoul-forest-park");
		((ObjectNode) root.path("CITYDATA")).set("EVENT_STTS", jsonMapper.readTree("""
				[{"EVENT_NM": "서울숲 재즈", "EVENT_X": "127.0374", "EVENT_Y": "37.5444"},
				 {"EVENT_NM": "좌표 없음", "EVENT_X": "", "EVENT_Y": ""}]"""));
		when(client.realtime("citydata", "서울숲공원")).thenReturn(root);

		mvc.perform(get("/api/seoul/city-data/{area}", "서울숲공원"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.congestion").value("RELAXED"))
				.andExpect(jsonPath("$.populationMin").value(3500))
				.andExpect(jsonPath("$.malePercent").value(52.7))
				.andExpect(jsonPath("$.femalePercent").value(47.3))
				.andExpect(jsonPath("$.weather.temperature").value(-1.5))
				.andExpect(jsonPath("$.weather.precipitationChance").value(0))
				.andExpect(jsonPath("$.consumptionRatios.length()").value(0))
				.andExpect(jsonPath("$.events.length()").value(1))
				.andExpect(jsonPath("$.events[0].latitude").value(37.5444));
	}

	private JsonNode demo(String name) throws Exception {
		try (InputStream in = getClass().getResourceAsStream("/demo/seongsu/" + name + ".json")) {
			return jsonMapper.readTree(in);
		}
	}
}
