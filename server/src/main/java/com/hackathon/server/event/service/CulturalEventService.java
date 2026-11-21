package com.hackathon.server.event.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import com.hackathon.server.batch.dto.BatchCounts;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.event.dto.CulturalEventRow;
import com.hackathon.server.event.repository.CulturalEventRepository;
import com.hackathon.server.publicdata.client.SeoulOpenApiClient;
import com.hackathon.server.publicdata.dto.ListResponse;

/**
 * 서울시 문화행사(aiViewCulture) 전체를 받아 cultural_event에 반영한다. 기준: docs/database-schema.md cultural_event.
 * 전체 페이지 건수가 맞고 식별자가 모두 있고 겹치지 않을 때만 반영한다. 아니면 기존 데이터를 건드리지 않는다.
 */
@Service
public class CulturalEventService {

	static final String SERVICE = "aiViewCulture";
	private static final int PAGE_SIZE = 1000;
	private static final Pattern CULTCODE = Pattern.compile("[?&]cultcode=(\\d+)");
	// 좌표가 있어도 온라인 행사일 수 있다. 제목·장소에 적힌 경우만 보수적으로 뺀다
	private static final Pattern ONLINE = Pattern.compile("온라인|비대면|원격|유튜브|youtube|zoom|웨비나|라이브\\s*스트리밍", Pattern.CASE_INSENSITIVE);

	/** 가져온 원본 전체 중 적재할 행. skipped = 온라인·좌표 오류·일정 오류로 뺀 건수. */
	public record Fetched(List<CulturalEventRow> rows, int total, int skipped) {
	}

	private final SeoulOpenApiClient seoul;
	private final CulturalEventRepository repository;

	public CulturalEventService(SeoulOpenApiClient seoul, CulturalEventRepository repository) {
		this.seoul = seoul;
		this.repository = repository;
	}

	/** API 호출이라 트랜잭션 밖에서 부른다. */
	public Fetched fetchAll() {
		List<JsonNode> raw = new ArrayList<>();
		long total = 0;
		for (int page = 1; ; page++) {
			ListResponse response = seoul.list(SERVICE, page, PAGE_SIZE);
			total = response.totalCount();
			response.items().forEach(raw::add);
			if (response.items().isEmpty() || (long) page * PAGE_SIZE >= total) break;
		}
		if (raw.isEmpty() || raw.size() != total) {
			throw new BusinessException(ErrorCode.PUBLIC_API_INVALID_RESPONSE,
					"서울시 문화행사 전체 건수(" + total + ")와 받은 건수(" + raw.size() + ")가 달라 적재하지 않습니다.");
		}
		Set<String> keys = new HashSet<>();
		List<CulturalEventRow> rows = new ArrayList<>();
		for (JsonNode node : raw) {
			Matcher m = CULTCODE.matcher(node.path("HMPG_ADDR").asString(""));
			if (!m.find() || !keys.add(m.group(1))) {
				throw new BusinessException(ErrorCode.PUBLIC_API_INVALID_RESPONSE,
						"서울시 문화행사 식별자(cultcode)가 없거나 겹치는 행이 있어 적재하지 않습니다: " + node.path("TITLE").asString(""));
			}
			CulturalEventRow row = toRow(m.group(1), node);
			if (row != null) rows.add(row);
		}
		return new Fetched(rows, raw.size(), raw.size() - rows.size());
	}

	/** read = 원본 전체, write = upsert한 행, skip = 뺀 행. 이번에 없거나 뺀 기존 행사는 비활성화한다. */
	@Transactional
	public BatchCounts save(Fetched fetched) {
		repository.upsertAll(fetched.rows());
		repository.deactivateExcept(fetched.rows().stream().map(CulturalEventRow::sourceKey).toList());
		return new BatchCounts(fetched.total(), fetched.rows().size(), fetched.skipped());
	}

	/** 적재 규칙에 맞지 않으면 null. */
	static CulturalEventRow toRow(String sourceKey, JsonNode node) {
		String title = text(node, "TITLE");
		String place = text(node, "PLACE");
		if (title == null || place == null || ONLINE.matcher(title + " " + place).find()) return null;
		LocalDate start = date(node, "STRTDATE");
		LocalDate end = date(node, "END_DATE");
		if (start == null || end == null || end.isBefore(start)) return null;
		Double lat = number(node, "LAT");
		Double lon = number(node, "LOT");
		if (lat == null || lon == null) return null;
		// 원본에 LAT·LOT가 뒤바뀐 행이 있다. 한국은 경도가 위도보다 커서 작은 값을 위도로 본다
		double latitude = Math.min(lat, lon);
		double longitude = Math.max(lat, lon);
		// 0·범위 밖 좌표는 뺀다. 서울시 자료라 한국 범위로 좁혀 엉뚱한 좌표도 거른다
		if (!(latitude >= 33 && latitude <= 39 && longitude >= 124 && longitude <= 132)) return null; // NaN도 거른다
		String isFree = text(node, "IS_FREE");
		return new CulturalEventRow(sourceKey, title, text(node, "CODENAME"), text(node, "GUNAME"), place, start, end,
				text(node, "PRO_TIME"), latitude, longitude, text(node, "USE_TRGT"), text(node, "PROGRAM"), text(node, "ETC_DESC"),
				text(node, "USE_FEE"), "무료".equals(isFree) ? Boolean.TRUE : "유료".equals(isFree) ? Boolean.FALSE : null,
				text(node, "HMPG_ADDR"), text(node, "MAIN_IMG"), node.toString());
	}

	private static String text(JsonNode node, String field) {
		String value = node.path(field).asString("").strip();
		return value.isEmpty() ? null : value;
	}

	/** "2026-11-27 00:00:00.0" → 2026-11-27 */
	private static LocalDate date(JsonNode node, String field) {
		String value = text(node, field);
		try {
			return value == null || value.length() < 10 ? null : LocalDate.parse(value.substring(0, 10));
		}
		catch (DateTimeParseException e) {
			return null;
		}
	}

	private static Double number(JsonNode node, String field) {
		String value = text(node, field);
		try {
			return value == null ? null : Double.valueOf(value);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
}
