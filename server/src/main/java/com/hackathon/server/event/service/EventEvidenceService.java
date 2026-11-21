package com.hackathon.server.event.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.hackathon.server.analysis.dto.EventResponse;
import com.hackathon.server.analysis.dto.EventSearch;
import com.hackathon.server.analysis.dto.EventSource;
import com.hackathon.server.analysis.dto.EventStatus;
import com.hackathon.server.analysis.dto.EventTrigger;
import com.hackathon.server.analysis.dto.SeasonEventResponse;
import com.hackathon.server.analysis.dto.SourceResponse;
import com.hackathon.server.analysis.dto.WeatherResponse;
import com.hackathon.server.analysis.evidence.EventCandidate;
import com.hackathon.server.analysis.evidence.EventEvidence;
import com.hackathon.server.analysis.evidence.EventEvidenceCollector;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.event.dto.NearbyEvent;
import com.hackathon.server.event.repository.CulturalEventRepository;
import com.hackathon.server.event.repository.SeasonEventRepository;

/** 행사·시즌·날씨 근거. 기준: ai-server/docs/API_CONTRACT_DRAFT.md "행사 후보 (Java)" */
@Service
public class EventEvidenceService implements EventEvidenceCollector {

	static final int EVENT_DAYS = 7;
	// 부자재 준비표(supply_rule)의 최대 준비 일수. 그만큼 앞의 시즌 행사까지 봐야 준비 시점을 놓치지 않는다
	static final int SEASON_DAYS = 21;
	static final int RADIUS_M = 1500;
	static final int MAX_EVENTS = 5;
	// 성인 평균 보행 속도 4km/h. 길을 돌아가는 거리는 모르니 직선거리로 추정한다
	private static final double WALK_METERS_PER_MINUTE = 67;
	static final Map<String, EventTrigger> TRIGGERS = Stream.concat(
			Stream.of("축제-문화/예술", "축제-기타", "축제-전통/역사", "축제-자연/경관", "축제-시민화합", "축제-관광/체육")
					.map(c -> Map.entry(c, EventTrigger.FESTIVAL)),
			Stream.of("콘서트", "클래식", "국악", "독주/독창회", "뮤지컬/오페라", "연극", "무용")
					.map(c -> Map.entry(c, EventTrigger.PERFORMANCE)))
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

	private final CulturalEventRepository eventRepository;
	private final SeasonEventRepository seasonRepository;
	private final WeatherEvidenceService weatherService;

	public EventEvidenceService(CulturalEventRepository eventRepository, SeasonEventRepository seasonRepository,
			WeatherEvidenceService weatherService) {
		this.eventRepository = eventRepository;
		this.seasonRepository = seasonRepository;
		this.weatherService = weatherService;
	}

	@Override
	public EventEvidence collect(StoreContext store) {
		List<EventCandidate> events = events(store);
		List<SeasonEventResponse> seasons = seasons(store.baseDate());
		WeatherResponse weather = weatherService.collect(store);

		EventSource source = !events.isEmpty() ? EventSource.CULTURAL_EVENT : !seasons.isEmpty() ? EventSource.SEASON
				: weather != null && !weather.impacts().isEmpty() ? EventSource.WEATHER : EventSource.NONE;
		List<SourceResponse> sources = new ArrayList<>();
		sources.add(new SourceResponse("서울시 문화행사 정보", "서울 열린데이터광장", "매일 갱신", null, store.baseTime(),
				"https://data.seoul.go.kr/dataList/OA-23041/S/1/datasetView.do"));
		if (!seasons.isEmpty()) {
			sources.add(new SourceResponse("기념일·학사·시즌 일정", "우주항공청 월력요항·학사일정 정리", "매년 갱신", null, store.baseTime(), null));
		}
		if (weather != null) {
			sources.add(new SourceResponse("단기예보", "기상청 (공공데이터포털)", "하루 8번 갱신", null, weather.checkedAt(),
					"https://www.data.go.kr/data/15084084/openapi.do"));
		}
		return new EventEvidence(new EventSearch(EVENT_DAYS, store.baseDate(), events.size(), source), events, seasons, weather, sources);
	}

	/** 가게 1.5km 안. 회차(같은 제목·장소)는 날짜를 이어 1건으로 묶는다. */
	private List<EventCandidate> events(StoreContext store) {
		List<NearbyEvent> rows = eventRepository.findNearby(store.latitude(), store.longitude(), RADIUS_M, store.baseDate(),
				EVENT_DAYS, TRIGGERS.keySet());
		Map<String, NearbyEvent> merged = new LinkedHashMap<>();
		for (NearbyEvent row : rows) {
			merged.merge(row.title() + "\n" + row.place(), row, (a, b) -> new NearbyEvent(a.title(), a.category(), a.place(),
					min(a.startDate(), b.startDate()), max(a.endDate(), b.endDate()), a.timeText(), a.isFree(), a.sourceUrl(),
					a.latitude(), a.longitude(), Math.min(a.distanceM(), b.distanceM())));
		}
		// 강추 점수는 분석 쪽에서 매기므로, 점수에 크게 드는 가까운 거리와 빠른 날짜 순으로 5건을 고른다
		List<NearbyEvent> top = merged.values().stream()
				.sorted(Comparator.comparingDouble(NearbyEvent::distanceM).thenComparing(NearbyEvent::startDate))
				.limit(MAX_EVENTS).toList();
		List<EventCandidate> candidates = new ArrayList<>();
		for (NearbyEvent e : top) {
			EventTime time = EventTime.parse(e.timeText());
			int distance = (int) Math.round(e.distanceM());
			EventResponse event = new EventResponse("E" + (candidates.size() + 1), e.title(), e.category(), TRIGGERS.get(e.category()),
					e.place(), e.startDate(), e.endDate(), e.timeText(),
					e.startDate().isAfter(store.baseDate()) ? EventStatus.UPCOMING : EventStatus.ONGOING,
					distance, Math.max(1, (int) Math.ceil(distance / WALK_METERS_PER_MINUTE)), e.isFree(),
					e.latitude(), e.longitude(), e.sourceUrl());
			candidates.add(new EventCandidate(event, time.timeBand(), time.peakLabel()));
		}
		return candidates;
	}

	private List<SeasonEventResponse> seasons(LocalDate baseDate) {
		List<SeasonEventResponse> seasons = new ArrayList<>();
		for (SeasonEventRepository.SeasonEvent s : seasonRepository.findActiveOrStartingWithin(baseDate, SEASON_DAYS)) {
			seasons.add(new SeasonEventResponse("H" + (seasons.size() + 1), s.code(), s.label(), s.type(), s.startDate(), s.endDate()));
		}
		return seasons;
	}

	private static LocalDate min(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}

	private static LocalDate max(LocalDate a, LocalDate b) {
		return a.isAfter(b) ? a : b;
	}
}
