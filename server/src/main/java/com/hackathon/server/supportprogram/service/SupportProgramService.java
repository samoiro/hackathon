package com.hackathon.server.supportprogram.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import com.hackathon.server.batch.dto.BatchCounts;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.publicdata.client.DataGoKrClient;
import com.hackathon.server.publicdata.dto.ListResponse;
import com.hackathon.server.supportprogram.dto.SupportProgramListResponse;
import com.hackathon.server.supportprogram.dto.SupportProgramResponse;
import com.hackathon.server.supportprogram.entity.SupportProgram;
import com.hackathon.server.supportprogram.repository.SupportProgramRepository;

/** 기업마당 접수 중인 공고를 전부 받아 서울 소상공인 대상 공고로 support_program을 통째로 바꾸고, 마감 임박 카드를 만든다. 기준: docs/database-schema.md support_program */
@Service
public class SupportProgramService {

	static final String PATH = "/1421000/bizinfo/pblancBsnsService";
	// 1,000건은 6~11초 걸려 공공 API 응답 제한 15초(PublicApiConfig)를 넘길 때가 있다. 500건은 4~5초
	static final int PAGE_SIZE = 500;
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter REGISTERED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	// 신청 기간이 날짜 범위일 때만 마감일이 있다. '예산 소진시까지'·'상시 접수' 등은 null
	private static final Pattern DATE_RANGE = Pattern.compile("^\\d{4}[-.]\\d{2}[-.]\\d{2} ~ (\\d{4})[-.](\\d{2})[-.](\\d{2})$");
	// 제목 맨 앞 [경북]·[충남ㆍ충북ㆍ대전ㆍ세종]·[비수도권] 같은 지역 표시 (2026-11-21 데이터 31종 모두 지역)
	private static final Pattern TITLE_REGION = Pattern.compile("^\\[([^\\]]+)\\]");

	private final DataGoKrClient dataGoKrClient;
	private final SupportProgramRepository repository;

	public SupportProgramService(DataGoKrClient dataGoKrClient, SupportProgramRepository repository) {
		this.dataGoKrClient = dataGoKrClient;
		this.repository = repository;
	}

	/** totalCount: API가 알려 준 전체 건수. programs에 없는 만큼은 필수 값이 없거나 서울 소상공인 대상이 아니어서 뺀 공고다. */
	public record Fetched(List<SupportProgram> programs, long totalCount) {
	}

	/** 전체 공고를 받아 서울 소상공인 대상 공고만 돌려준다. 받은 건수가 전체와 다르거나 0건이면 기존 데이터를 지키려고 실패한다. API 호출이라 트랜잭션 밖에서 부른다. */
	public Fetched fetchAll() {
		Map<String, SupportProgram> programs = new LinkedHashMap<>();
		long received = 0;
		long valid = 0;
		long totalCount;
		for (int page = 1; ; page++) {
			ListResponse response = dataGoKrClient.list(PATH,
					Map.of("dataType", "json", "pageNo", String.valueOf(page), "numOfRows", String.valueOf(PAGE_SIZE)));
			totalCount = response.totalCount();
			for (JsonNode item : response.items()) {
				received++;
				SupportProgram program = toEntity(item);
				if (program != null) {
					valid++;
					programs.put(program.getId(), program);
				}
			}
			if ((long) page * PAGE_SIZE >= totalCount) break;
		}
		// 받는 사이에 공고가 새로 올라오면 페이지가 밀려 중복(같은 id)·누락이 생긴다. BatchJobService가 다시 받는다
		if (totalCount == 0 || received != totalCount || programs.size() != valid) {
			throw new BusinessException(ErrorCode.PUBLIC_API_INVALID_RESPONSE,
					"기업마당 공고 건수가 맞지 않습니다 (받은 공고 %d건, 전체 %d건).".formatted(received, totalCount));
		}
		return new Fetched(programs.values().stream().filter(SupportProgramService::isTarget).toList(), totalCount);
	}

	/**
	 * 서울: 서울 태그가 있고(전국 공고는 17개 시도 태그를 모두 단다), 서울 밖 지자체 공고가 아니고, 제목 맨 앞 [지역]에 서울이 있거나 [지역]이 없다.
	 * 광역 지자체 이름은 …도·…시로, 중앙부처 이름은 부·처·청·회로 끝난다. 제목 중간의 지역명은 '환경기업'의 '경기'처럼 잘못 걸려서 보지 않는다.
	 * 소상공인: 소상공인 태그와 지원 대상이 서로 빠뜨리는 공고가 있어 둘 중 하나면 된다.
	 */
	static boolean isTarget(SupportProgram program) {
		String agency = program.getAgencyName();
		Matcher region = TITLE_REGION.matcher(program.getTitle());
		boolean seoul = program.getHashtags().contains("서울")
				&& (agency.equals("서울특별시") || !(agency.endsWith("도") || agency.endsWith("시")))
				&& (!region.find() || region.group(1).contains("서울"));
		return seoul && (program.getHashtags().contains("소상공인") || program.getTargetName().equals("소상공인"));
	}

	@Transactional
	public BatchCounts replace(Fetched fetched) {
		repository.deleteAllInBatch();
		repository.saveAll(fetched.programs());
		long saved = fetched.programs().size();
		return new BatchCounts(fetched.totalCount(), saved, fetched.totalCount() - saved);
	}

	@Transactional(readOnly = true)
	public SupportProgramListResponse closingSoon(LocalDate today) {
		var checkedAt = repository.findFirstByOrderByCreatedDatetimeDesc().map(SupportProgram::getCreatedDatetime)
				.orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "지원사업 공고를 아직 받아 오지 않았습니다. 일배치가 돈 뒤에 다시 시도해 주세요."));
		var items = repository.findTop5ByDeadlineGreaterThanEqualOrderByDeadlineAsc(today).stream()
				.map(p -> new SupportProgramResponse(p.getId(), p.getTitle(), p.getUrl(), p.getAgencyName(), p.getFieldName(),
						p.getDeadline(), p.getApplicationPeriod()))
				.toList();
		return new SupportProgramListResponse(items, checkedAt);
	}

	/** DB 제약과 같은 필수 값이 없으면 null(건너뜀). 제목·기관명은 공공누리 변경금지라 원문 그대로 둔다. */
	static SupportProgram toEntity(JsonNode item) {
		String id = text(item, "pblancId");
		String title = text(item, "pblancNm");
		String url = text(item, "pblancUrl");
		String agency = text(item, "jrsdInsttNm");
		String target = text(item, "trgetNm");
		String period = text(item, "reqstBeginEndDe");
		OffsetDateTime registered = registered(text(item, "creatPnttm"));
		if (!id.matches("PBLN_[0-9]+") || !StringUtils.hasText(title) || !url.startsWith("https://www.bizinfo.go.kr/")
				|| !StringUtils.hasText(agency) || !StringUtils.hasText(target) || !StringUtils.hasText(period) || registered == null) {
			return null;
		}
		String field = text(item, "pldirSportRealmLclasCodeNm");
		List<String> hashtags = Arrays.stream(text(item, "hashtags").split(",")).map(String::strip).filter(t -> !t.isEmpty()).toList();
		return new SupportProgram(id, title, url, agency, StringUtils.hasText(field) ? field : null, target, hashtags, period,
				deadline(period), registered);
	}

	static LocalDate deadline(String period) {
		Matcher m = DATE_RANGE.matcher(period.strip());
		if (!m.matches()) return null;
		try {
			return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
		}
		catch (DateTimeException e) {
			return null;   // 2026-02-30 같은 없는 날짜
		}
	}

	private static OffsetDateTime registered(String value) {
		try {
			return LocalDateTime.parse(value, REGISTERED).atZone(SEOUL).toOffsetDateTime();
		}
		catch (DateTimeParseException e) {
			return null;
		}
	}

	private static String text(JsonNode item, String field) {
		return item.path(field).asString("");
	}
}
