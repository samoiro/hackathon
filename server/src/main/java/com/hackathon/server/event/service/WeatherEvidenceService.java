package com.hackathon.server.event.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import com.hackathon.server.analysis.dto.DayPart;
import com.hackathon.server.analysis.dto.WeatherForecast;
import com.hackathon.server.analysis.dto.WeatherImpact;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.dto.WeatherWarning;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.publicdata.client.KweatherClient;

/**
 * 가게 행정동의 기상특보(kw-warning1)·단기예보(kw-fct-dl1)·주간예보(kw-fct-wl1)로 향후 7일 날씨 근거 W1을 만든다.
 * 날씨는 보조 근거라 케이웨더 호출이 실패하면 null을 돌려주고 분석은 계속한다.
 */
@Service
public class WeatherEvidenceService {

	private static final Logger log = LoggerFactory.getLogger(WeatherEvidenceService.class);
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter KWEATHER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	static final int DAYS = 7;
	// 기상청 특보 기준: 한파주의보 아침 최저 -12℃ 이하, 폭염주의보 최고 33℃ 이상
	private static final double COLD_WAVE_MIN = -12;
	private static final double HEATWAVE_MAX = 33;
	private static final int RAIN_CHANCE = 60;
	private static final Map<String, WeatherImpact> WARNING_IMPACTS = Map.of(
			"한파", WeatherImpact.COLD_WAVE, "폭염", WeatherImpact.HEATWAVE, "강풍", WeatherImpact.STRONG_WIND,
			"태풍", WeatherImpact.STRONG_WIND, "대설", WeatherImpact.HEAVY_SNOW, "호우", WeatherImpact.RAIN_SNOW);
	// 건조·풍랑 특보는 가게 손님 수와 상관이 적어 특보 목록에만 두고 WARNING으로 보지 않는다
	private static final Set<String> NO_IMPACT_WARNINGS = Set.of("건조", "풍랑");

	private final KweatherClient kweather;

	public WeatherEvidenceService(KweatherClient kweather) {
		this.kweather = kweather;
	}

	/** 행정동 코드가 없거나 케이웨더가 실패하면 null. FINE_DUST는 미세먼지 자료가 없어 넣지 않는다. */
	public WeatherResponse collect(StoreContext store) {
		String code = store.adminDongCode();
		if (code == null) return null;
		try {
			List<WeatherWarning> warnings = warnings(kweather.get("kw-warning1", code).path(code).path("data"), store.baseTime());
			List<WeatherForecast> forecasts = forecasts(kweather.get("kw-fct-dl1", code).path(code),
					kweather.get("kw-fct-wl1", code).path(code), store.baseDate());
			return new WeatherResponse("W1", code, impacts(warnings, forecasts), warnings, forecasts, store.baseTime());
		}
		catch (RuntimeException e) {
			log.warn("날씨 근거를 받지 못해 비워 둡니다 (행정동 {}): {}", code, e.getMessage());
			return null;
		}
	}

	/** 특보는 같은 인덱스가 한 건. warn "한파주의보" → kind "한파", level "주의보". 해제 시각이 지난 것은 뺀다. */
	static List<WeatherWarning> warnings(JsonNode data, OffsetDateTime now) {
		List<WeatherWarning> warnings = new ArrayList<>();
		JsonNode warn = data.path("warn");
		for (int i = 0; i < warn.size(); i++) {
			String name = warn.get(i).asString("");
			String level = name.endsWith("주의보") ? "주의보" : name.endsWith("경보") ? "경보" : "";
			OffsetDateTime start = time(data.path("startTime").path(i));
			OffsetDateTime end = time(data.path("endTime").path(i));
			if (name.isBlank() || start == null || (end != null && !end.isAfter(now))) continue;
			warnings.add(new WeatherWarning(name.substring(0, name.length() - level.length()), level.isEmpty() ? name : level, start, end));
		}
		return warnings;
	}

	/**
	 * 단기예보는 시각 배열이 없어 발표 시각으로 첫 칸을 정한다(05시 발표: 오늘 오전부터, 11·17시: 오늘 오후부터, 오전·오후 번갈아).
	 * 칸마다 기온이 하나라 오전은 최저, 오후는 최고 기온으로 둔다. 주간예보는 단기예보에 없는 칸만 쓴다. baseDate부터 7일만.
	 */
	static List<WeatherForecast> forecasts(JsonNode shortTerm, JsonNode weekly, LocalDate baseDate) {
		Map<String, WeatherForecast> slots = new LinkedHashMap<>();
		OffsetDateTime issued = time(shortTerm.path("service").path("timestamp"));
		JsonNode s = shortTerm.path("data");
		if (issued != null) {
			LocalDate date = issued.toLocalDate();
			boolean am = issued.getHour() < 11;
			for (int i = 0; i < s.path("wText").size(); i++) {
				Double ta = number(s.path("ta").path(i));
				slots.put(date + (am ? "AM" : "PM"), new WeatherForecast(date, am ? DayPart.AM : DayPart.PM, s.path("wText").get(i).asString(""),
						am ? ta : null, am ? null : ta, integer(s.path("rnSt").path(i))));
				if (!am) date = date.plusDays(1);
				am = !am;
			}
		}
		JsonNode w = weekly.path("data");
		JsonNode tm = w.path("tm");
		Map<LocalDate, Long> perDay = new HashMap<>();
		for (JsonNode t : tm) {
			OffsetDateTime at = time(t);
			if (at != null) perDay.merge(at.toLocalDate(), 1L, Long::sum);
		}
		for (int i = 0; i < tm.size(); i++) {
			OffsetDateTime at = time(tm.get(i));
			if (at == null) continue;
			// 3~7일 후는 오전(00시)·오후(12시) 두 칸, 8~10일 후는 하루 한 칸
			DayPart part = perDay.get(at.toLocalDate()) == 1 ? DayPart.ALL_DAY : at.getHour() < 12 ? DayPart.AM : DayPart.PM;
			slots.putIfAbsent(at.toLocalDate() + part.name(), new WeatherForecast(at.toLocalDate(), part, w.path("wText").path(i).asString(""),
					number(w.path("taMin").path(i)), number(w.path("taMax").path(i)), integer(w.path("rePer").path(i))));
		}
		LocalDate last = baseDate.plusDays(DAYS - 1);
		return slots.values().stream()
				.filter(f -> !f.date().isBefore(baseDate) && !f.date().isAfter(last))
				.sorted(Comparator.comparing(WeatherForecast::date).thenComparing(WeatherForecast::dayPart))
				.toList();
	}

	static List<WeatherImpact> impacts(List<WeatherWarning> warnings, List<WeatherForecast> forecasts) {
		Set<WeatherImpact> impacts = EnumSet.noneOf(WeatherImpact.class);
		for (WeatherWarning warning : warnings) {
			if (NO_IMPACT_WARNINGS.contains(warning.kind())) continue;
			impacts.add(WeatherImpact.WARNING);
			WeatherImpact impact = WARNING_IMPACTS.get(warning.kind());
			if (impact != null) impacts.add(impact);
		}
		for (WeatherForecast f : forecasts) {
			if (f.sky().contains("비") || f.sky().contains("눈") || f.sky().contains("소나기")
					|| (f.precipitationChance() != null && f.precipitationChance() >= RAIN_CHANCE)) impacts.add(WeatherImpact.RAIN_SNOW);
			if (f.minTemperature() != null && f.minTemperature() <= COLD_WAVE_MIN) impacts.add(WeatherImpact.COLD_WAVE);
			if (f.maxTemperature() != null && f.maxTemperature() >= HEATWAVE_MAX) impacts.add(WeatherImpact.HEATWAVE);
		}
		return List.copyOf(impacts);
	}

	/** "202611211000" (KST) */
	private static OffsetDateTime time(JsonNode node) {
		String value = node.asString("");
		try {
			return value.length() == 12 ? LocalDateTime.parse(value, KWEATHER_TIME).atZone(SEOUL).toOffsetDateTime() : null;
		}
		catch (DateTimeParseException e) {
			return null;
		}
	}

	private static Double number(JsonNode node) {
		return node.isNumber() ? node.asDouble() : null;
	}

	private static Integer integer(JsonNode node) {
		return node.isNumber() ? node.asInt() : null;
	}
}
