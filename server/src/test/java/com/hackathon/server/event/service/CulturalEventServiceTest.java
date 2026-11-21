package com.hackathon.server.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.event.dto.CulturalEventRow;
import com.hackathon.server.event.repository.CulturalEventRepository;
import com.hackathon.server.publicdata.client.SeoulOpenApiClient;
import com.hackathon.server.publicdata.dto.ListResponse;

/** 성수동 시연 원본 15건(demo/seongsu/culture-events.json)으로 적재 규칙을 확인한다. */
class CulturalEventServiceTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private final SeoulOpenApiClient seoul = mock(SeoulOpenApiClient.class);
	private final CulturalEventService service = new CulturalEventService(seoul, mock(CulturalEventRepository.class));

	private static ArrayNode demoRows() throws IOException {
		try (InputStream in = CulturalEventServiceTest.class.getResourceAsStream("/demo/seongsu/culture-events.json")) {
			return (ArrayNode) JSON.readTree(in).path("aiViewCulture").path("row");
		}
	}

	@Test
	void convertsDemoRowAndSwapsReversedCoordinates() throws IOException {
		ObjectNode node = (ObjectNode) demoRows().get(0);
		node.put("LAT", "127.036006376334").put("LOT", "37.5593072327953");

		CulturalEventRow row = CulturalEventService.toRow("159872", node);

		assertThat(row.title()).isEqualTo("[성동문화재단] 오페레타 [유쾌한 미망인]");
		assertThat(row.startDate()).isEqualTo(LocalDate.of(2026, 11, 27));
		assertThat(row.endDate()).isEqualTo(LocalDate.of(2026, 11, 28));
		assertThat(row.latitude()).isEqualTo(37.5593072327953);
		assertThat(row.longitude()).isEqualTo(127.036006376334);
		assertThat(row.isFree()).isFalse();
		assertThat(row.program()).isNull(); // 빈 문자열은 null
		assertThat(JSON.readTree(row.rawPayload()).path("RGSTDATE").asString()).isEqualTo("2026-10-16");
	}

	@Test
	void skipsOnlineBadCoordinateAndBadScheduleRows() throws IOException {
		JsonNode base = demoRows().get(0);

		assertThat(CulturalEventService.toRow("1", ((ObjectNode) base.deepCopy()).put("TITLE", "[온라인] 랜선 음악회"))).isNull();
		assertThat(CulturalEventService.toRow("1", ((ObjectNode) base.deepCopy()).put("LAT", "0"))).isNull();
		assertThat(CulturalEventService.toRow("1", ((ObjectNode) base.deepCopy()).put("LOT", "abc"))).isNull();
		assertThat(CulturalEventService.toRow("1", ((ObjectNode) base.deepCopy()).put("END_DATE", "2026-11-01 00:00:00.0"))).isNull();
	}

	@Test
	void fetchesAllRowsWhenCountMatches() throws IOException {
		ArrayNode rows = demoRows();
		when(seoul.list(eq(CulturalEventService.SERVICE), eq(1), anyInt())).thenReturn(new ListResponse(rows.size(), rows));

		CulturalEventService.Fetched fetched = service.fetchAll();

		assertThat(fetched.total()).isEqualTo(15);
		assertThat(fetched.rows()).hasSize(15);
		assertThat(fetched.rows()).extracting(CulturalEventRow::sourceKey).contains("159872");
	}

	@Test
	void refusesPartialOrDuplicatedResponse() throws IOException {
		ArrayNode rows = demoRows();
		when(seoul.list(eq(CulturalEventService.SERVICE), anyInt(), anyInt())).thenReturn(new ListResponse(19_782, rows)); // 페이지마다 15건뿐
		assertThatThrownBy(service::fetchAll).isInstanceOf(BusinessException.class).hasMessageContaining("건수");

		rows.add(rows.get(0).deepCopy());
		when(seoul.list(eq(CulturalEventService.SERVICE), eq(1), anyInt())).thenReturn(new ListResponse(rows.size(), rows));
		assertThatThrownBy(service::fetchAll).isInstanceOf(BusinessException.class).hasMessageContaining("cultcode");
	}
}
