package com.hackathon.server.event.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import com.hackathon.server.analysis.dto.DayPart;
import com.hackathon.server.analysis.dto.WeatherForecast;
import com.hackathon.server.analysis.dto.WeatherImpact;
import com.hackathon.server.analysis.dto.WeatherImpactStart;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.publicdata.client.DataGoKrClient;

/**
 * 가게 좌표의 기상청 단기예보(공공데이터포털 VilageFcstInfoService_2.0)로 날씨 근거 W1을 만든다.
 * 시간별 예보를 오전(0~11시)·오후(12~23시) 칸으로 묶는다. 날씨는 보조 근거라 호출이 실패하면 null을 돌려주고 분석은 계속한다.
 * shortcut: 단기예보는 발표 후 약 3~4일까지만 있다, 주 후반 날씨가 필요하면 중기예보(MidFcstInfoService)를 더한다.
 */
@Service
public class WeatherEvidenceService {

	private static final Logger log = LoggerFactory.getLogger(WeatherEvidenceService.class);
	private static final String PATH = "/1360000/VilageFcstInfoService_2.0/getVilageFcst";
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
	/** 단기예보 발표 시각. 발표 10분 뒤부터 받을 수 있다 */
	private static final int[] BASE_HOURS = {23, 20, 17, 14, 11, 8, 5, 2};
	static final int DAYS = 7;
	// 기상청 특보 기준: 한파주의보 아침 최저 -12℃ 이하, 폭염주의보 최고 33℃ 이상
	private static final double COLD_WAVE_MIN = -12;
	private static final double HEATWAVE_MAX = 33;
	private static final int RAIN_CHANCE = 60;

	private final DataGoKrClient dataGoKr;

	public WeatherEvidenceService(DataGoKrClient dataGoKr) {
		this.dataGoKr = dataGoKr;
	}

	/** 예보를 못 받거나 비어 있으면 null. 미세먼지 자료는 없어 FINE_DUST는 넣지 않는다. */
	public WeatherResponse collect(StoreContext store) {
		int[] grid = grid(store.latitude(), store.longitude());
		LocalDateTime base = baseTime(store.baseTime().atZoneSameInstant(SEOUL).toLocalDateTime());
		Map<String, String> query = new LinkedHashMap<>();
		query.put("pageNo", "1");
		query.put("numOfRows", "1500");
		query.put("dataType", "JSON");
		query.put("base_date", DATE.format(base));
		query.put("base_time", String.format("%02d00", base.getHour()));
		query.put("nx", String.valueOf(grid[0]));
		query.put("ny", String.valueOf(grid[1]));
		List<WeatherForecast> forecasts;
		try {
			forecasts = forecasts(dataGoKr.list(PATH, query).items(), store.baseDate());
		}
		catch (RuntimeException e) {
			log.warn("기상청 단기예보를 받지 못해 날씨를 비워 둡니다 (격자 {},{}): {}", grid[0], grid[1], e.getMessage());
			return null;
		}
		if (forecasts.isEmpty()) return null;
		Map<WeatherImpact, LocalDate> starts = impactStarts(forecasts);
		return new WeatherResponse("W1", store.adminDongCode(), List.copyOf(starts.keySet()), List.of(), forecasts, store.baseTime(),
				starts.entrySet().stream().map(e -> new WeatherImpactStart(e.getKey(), e.getValue())).toList());
	}

	/** 지금 받을 수 있는 가장 최근 발표 시각 (KST) */
	static LocalDateTime baseTime(LocalDateTime now) {
		LocalDateTime ready = now.minusMinutes(10);
		for (int hour : BASE_HOURS) {
			if (ready.getHour() >= hour) return ready.toLocalDate().atTime(hour, 0);
		}
		return ready.toLocalDate().minusDays(1).atTime(23, 0);
	}

	/** 위경도 → 기상청 동네예보 격자 (Lambert 정각원추도법, 기상청 제공 변환식) */
	static int[] grid(double lat, double lon) {
		double re = 6371.00877 / 5.0;
		double rad = Math.PI / 180.0;
		double slat1 = 30.0 * rad, slat2 = 60.0 * rad, olon = 126.0 * rad, olat = 38.0 * rad;
		double sn = Math.log(Math.cos(slat1) / Math.cos(slat2))
				/ Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5));
		double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn) * Math.cos(slat1) / sn;
		double ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn);
		double ra = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + lat * rad * 0.5), sn);
		double theta = lon * rad - olon;
		if (theta > Math.PI) theta -= 2.0 * Math.PI;
		if (theta < -Math.PI) theta += 2.0 * Math.PI;
		theta *= sn;
		return new int[] {(int) Math.floor(ra * Math.sin(theta) + 43 + 0.5), (int) Math.floor(ro - ra * Math.cos(theta) + 136 + 0.5)};
	}

	/** 한 칸에 모은 시간별 값 */
	private static final class Slot {
		Integer sky;
		Integer pty;
		Integer pop;
		Double tmpMin;
		Double tmpMax;
		Double tmn;
		Double tmx;
	}

	/**
	 * 시간별 항목(category: SKY 하늘, PTY 강수 형태, POP 강수 확률, TMP 기온, TMN 최저, TMX 최고)을 오전·오후 칸으로 묶는다.
	 * 오전은 최저(TMN, 없으면 그 칸 TMP 최저), 오후는 최고(TMX, 없으면 TMP 최고) 기온만 둔다. baseDate부터 7일만.
	 */
	static List<WeatherForecast> forecasts(JsonNode items, LocalDate baseDate) {
		Map<LocalDate, Map<DayPart, Slot>> days = new LinkedHashMap<>();
		for (JsonNode item : items) {
			String date = item.path("fcstDate").asString("");
			String time = item.path("fcstTime").asString("");
			if (date.length() != 8 || time.length() != 4) continue;
			LocalDate day = LocalDate.parse(date, DATE);
			DayPart part = Integer.parseInt(time.substring(0, 2)) < 12 ? DayPart.AM : DayPart.PM;
			Slot slot = days.computeIfAbsent(day, d -> new EnumMap<>(DayPart.class)).computeIfAbsent(part, p -> new Slot());
			Double value = number(item.path("fcstValue").asString(""));
			if (value == null) continue;
			switch (item.path("category").asString("")) {
				case "SKY" -> slot.sky = Math.max(slot.sky == null ? 0 : slot.sky, value.intValue());
				// 강수 형태는 0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기. 칸에서 처음 나타난 강수를 쓴다
				case "PTY" -> slot.pty = slot.pty == null || slot.pty == 0 ? value.intValue() : slot.pty;
				case "POP" -> slot.pop = Math.max(slot.pop == null ? 0 : slot.pop, value.intValue());
				case "TMP" -> {
					slot.tmpMin = slot.tmpMin == null ? value : Math.min(slot.tmpMin, value);
					slot.tmpMax = slot.tmpMax == null ? value : Math.max(slot.tmpMax, value);
				}
				case "TMN" -> slot.tmn = value;
				case "TMX" -> slot.tmx = value;
				default -> {
				}
			}
		}
		LocalDate last = baseDate.plusDays(DAYS - 1);
		return days.entrySet().stream()
				.filter(d -> !d.getKey().isBefore(baseDate) && !d.getKey().isAfter(last))
				.flatMap(d -> d.getValue().entrySet().stream().map(p -> {
					Slot s = p.getValue();
					boolean am = p.getKey() == DayPart.AM;
					return new WeatherForecast(d.getKey(), p.getKey(), sky(s), am ? (s.tmn != null ? s.tmn : s.tmpMin) : null,
							am ? null : (s.tmx != null ? s.tmx : s.tmpMax), s.pop);
				}))
				.sorted(Comparator.comparing(WeatherForecast::date).thenComparing(WeatherForecast::dayPart))
				.toList();
	}

	/** "맑음", "구름많음", "흐림", 강수가 있으면 "비"·"흐리고 비"처럼 */
	private static String sky(Slot s) {
		String sky = s.sky == null ? "" : switch (s.sky) {
			case 1 -> "맑음";
			case 3 -> "구름많음";
			case 4 -> "흐림";
			default -> "";
		};
		String rain = s.pty == null ? null : switch (s.pty) {
			case 1 -> "비";
			case 2 -> "비/눈";
			case 3 -> "눈";
			case 4 -> "소나기";
			default -> null;
		};
		if (rain == null) return sky;
		return s.sky != null && s.sky == 4 ? "흐리고 " + rain : rain;
	}

	/**
	 * 영향마다 예보가 기준을 처음 넘는 날(부자재 준비 기한을 이 날부터 거꾸로 센다). 영향 순서는 WeatherImpact 순.
	 */
	static Map<WeatherImpact, LocalDate> impactStarts(List<WeatherForecast> forecasts) {
		Map<WeatherImpact, LocalDate> starts = new EnumMap<>(WeatherImpact.class);
		for (WeatherForecast f : forecasts) {
			if (f.sky().contains("비") || f.sky().contains("눈") || f.sky().contains("소나기")
					|| (f.precipitationChance() != null && f.precipitationChance() >= RAIN_CHANCE)) {
				starts.merge(WeatherImpact.RAIN_SNOW, f.date(), WeatherEvidenceService::earlier);
			}
			if (f.minTemperature() != null && f.minTemperature() <= COLD_WAVE_MIN) {
				starts.merge(WeatherImpact.COLD_WAVE, f.date(), WeatherEvidenceService::earlier);
			}
			if (f.maxTemperature() != null && f.maxTemperature() >= HEATWAVE_MAX) {
				starts.merge(WeatherImpact.HEATWAVE, f.date(), WeatherEvidenceService::earlier);
			}
		}
		return starts;
	}

	private static LocalDate earlier(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}

	private static Double number(String value) {
		try {
			return Double.parseDouble(value);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
}
