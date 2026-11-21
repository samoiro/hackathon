package com.hackathon.server.supportprogram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.batch.dto.BatchCounts;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.publicdata.client.DataGoKrClient;
import com.hackathon.server.publicdata.dto.ListResponse;
import com.hackathon.server.supportprogram.dto.SupportProgramResponse;
import com.hackathon.server.supportprogram.entity.SupportProgram;
import com.hackathon.server.supportprogram.repository.SupportProgramRepository;

/** 기업마당 응답 파싱·건수 검증과, 통째 교체 후 마감 임박 조회를 실제 Postgres로 확인한다. 응답 형태는 2026-11-21 실제 API 기준. */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, SupportProgramService.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SupportProgramServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	SupportProgramService service;
	@Autowired
	SupportProgramRepository repository;
	@Autowired
	TestEntityManager em;
	@MockitoBean
	DataGoKrClient dataGoKrClient;

	private static Map<String, Object> item(String id, String period) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("pblancId", id);
		item.put("pblancNm", "2026년 소상공인 법률ㆍ노무 현장 상담 지원사업 모집 공고 ");
		item.put("pblancUrl", "https://www.bizinfo.go.kr/sii/siia/selectSIIA200Detail.do?pblancId=" + id);
		item.put("jrsdInsttNm", "중소벤처기업부");
		item.put("pldirSportRealmLclasCodeNm", "경영");
		item.put("trgetNm", "소상공인");
		item.put("hashtags", "경영,서울, 부산 ,,2026,소상공인");
		item.put("reqstBeginEndDe", period);
		item.put("creatPnttm", "2026-11-20 10:08:39");
		item.put("flpthNm", null);
		return item;
	}

	private void apiReturns(long totalCount, List<Map<String, Object>> page1, List<Map<String, Object>> page2) {
		when(dataGoKrClient.list(eq(SupportProgramService.PATH), anyMap())).thenAnswer(call -> {
			Map<String, String> query = call.getArgument(1);
			var items = "1".equals(query.get("pageNo")) ? page1 : page2;
			return new ListResponse(totalCount, JSON.valueToTree(items));
		});
	}

	private static SupportProgram program(String id, LocalDate deadline, String registered) {
		return new SupportProgram(id, "공고 " + id, "https://www.bizinfo.go.kr/" + id, "서울특별시", null, "소상공인", List.of("서울", "소상공인"),
				deadline == null ? "예산 소진시까지" : "2026-11-01 ~ " + deadline, deadline, OffsetDateTime.parse(registered));
	}

	@Test
	void parsesItemsAndSkipsOnesMissingRequiredValues() {
		var dotRange = item("PBLN_000000000000001", "2020.01.01 ~ 2026.12.31");
		var noDate = item("PBLN_000000000000002", "예산 소진시까지");
		noDate.put("pldirSportRealmLclasCodeNm", "");
		var noId = item("", "상시 접수");
		apiReturns(3, List.of(dotRange, noDate, noId), List.of());

		var fetched = service.fetchAll();

		assertThat(fetched.totalCount()).isEqualTo(3);
		assertThat(fetched.programs()).hasSize(2);
		SupportProgram first = fetched.programs().get(0);
		assertThat(first.getTitle()).endsWith("모집 공고 ");   // 변경금지라 원문 그대로
		assertThat(first.getHashtags()).containsExactly("경영", "서울", "부산", "2026", "소상공인");
		assertThat(first.getDeadline()).isEqualTo(LocalDate.of(2026, 12, 31));
		assertThat(first.getFieldName()).isEqualTo("경영");
		assertThat(first.getRegisteredDatetime()).isEqualTo(OffsetDateTime.parse("2026-11-20T10:08:39+09:00"));
		SupportProgram second = fetched.programs().get(1);
		assertThat(second.getDeadline()).isNull();
		assertThat(second.getApplicationPeriod()).isEqualTo("예산 소진시까지");
		assertThat(second.getFieldName()).isNull();
	}

	@Test
	void pagesUntilTotalCount() {
		int pageSize = SupportProgramService.PAGE_SIZE;
		List<Map<String, Object>> page1 = new ArrayList<>();
		for (int i = 0; i < pageSize; i++) page1.add(item("PBLN_%015d".formatted(i), "상시 접수"));
		apiReturns(pageSize + 1, page1, List.of(item("PBLN_%015d".formatted(pageSize), "상시 접수")));

		assertThat(service.fetchAll().programs()).hasSize(pageSize + 1);
	}

	@Test
	void keepsOnlySeoulSmallBusiness() {
		var busanOnly = item("PBLN_000000000000001", "상시 접수");
		busanOnly.put("hashtags", "경영,부산,소상공인");
		// 실제 공고: 시도 태그는 다 달았지만 다른 지역 사업
		var gyeongbuk = item("PBLN_000000000000002", "예산 소진시까지");
		gyeongbuk.put("jrsdInsttNm", "경상북도");
		gyeongbuk.put("pblancNm", "2026년 경북 우체국쇼핑 소담스퀘어 경북 특별관 참가 지원 사업 참여 소상공인 모집 공고");
		var gwangju = item("PBLN_000000000000003", "예산 소진시까지");
		gwangju.put("pblancNm", "[광주] 2026년 소상공인 SNSㆍ블로그 홍보지원 참여업체 모집 공고");
		var capitalArea = item("PBLN_000000000000004", "예산 소진시까지");
		capitalArea.put("pblancNm", "[서울ㆍ인천ㆍ경기ㆍ강원] 2026년 수도권 소상공인 모집 공고");
		var seoulCity = item("PBLN_000000000000005", "예산 소진시까지");
		seoulCity.put("jrsdInsttNm", "서울특별시");
		seoulCity.put("pblancNm", "[서울] 2026년 2차 소상공인 온라인 홍보관 참여 업체 모집 공고");
		var national = item("PBLN_000000000000006", "예산 소진시까지");   // 중앙부처 전국 공고
		var notSmallBusiness = item("PBLN_000000000000007", "예산 소진시까지");   // 태그가 소상공인시장진흥공단일 뿐 대상은 중소기업
		notSmallBusiness.put("trgetNm", "중소기업");
		notSmallBusiness.put("hashtags", "경영,서울,소상공인시장진흥공단");
		var targetOnly = item("PBLN_000000000000008", "예산 소진시까지");   // 소상공인 태그는 없고 지원 대상만 소상공인
		targetOnly.put("hashtags", "경영,서울");
		apiReturns(8, List.of(busanOnly, gyeongbuk, gwangju, capitalArea, seoulCity, national, notSmallBusiness, targetOnly), List.of());

		var fetched = service.fetchAll();

		assertThat(fetched.programs()).extracting(SupportProgram::getId).containsExactly(
				"PBLN_000000000000004", "PBLN_000000000000005", "PBLN_000000000000006", "PBLN_000000000000008");
		assertThat(fetched.totalCount()).isEqualTo(8);
	}

	@Test
	void failsWhenCountDoesNotMatchSoExistingRowsStay() {
		var a = item("PBLN_000000000000001", "상시 접수");
		apiReturns(2, List.of(a, a), List.of());   // 받는 사이 페이지가 밀려 같은 공고가 두 번 옴
		assertThat(assertThrows(BusinessException.class, service::fetchAll).getErrorCode()).isEqualTo(ErrorCode.PUBLIC_API_INVALID_RESPONSE);

		apiReturns(0, List.of(), List.of());
		assertThat(assertThrows(BusinessException.class, service::fetchAll).getErrorCode()).isEqualTo(ErrorCode.PUBLIC_API_INVALID_RESPONSE);
	}

	@Test
	void replacesAllRowsAndPicksClosingSoon() {
		service.replace(new SupportProgramService.Fetched(List.of(program("PBLN_9", null, "2026-11-20T09:00:00+09:00")), 1));
		em.flush();
		em.clear();

		BatchCounts counts = service.replace(new SupportProgramService.Fetched(List.of(
				program("PBLN_1", TODAY, "2026-11-01T09:00:00+09:00"),   // 오늘 마감도 포함
				program("PBLN_2", TODAY.plusDays(2), "2026-11-01T09:00:00+09:00"),
				program("PBLN_3", null, "2026-11-19T09:00:00+09:00"),   // 마감일 없음
				program("PBLN_4", TODAY.minusDays(1), "2026-11-01T09:00:00+09:00"),   // 어제 마감
				program("PBLN_5", TODAY.plusDays(30), "2026-11-01T09:00:00+09:00"),
				program("PBLN_6", TODAY.plusDays(3), "2026-11-01T09:00:00+09:00"),
				program("PBLN_7", TODAY.plusDays(10), "2026-11-01T09:00:00+09:00"),
				program("PBLN_8", TODAY.plusDays(40), "2026-11-01T09:00:00+09:00")),   // 5개를 넘어 빠짐
				10));
		em.flush();
		em.clear();

		var response = service.closingSoon(TODAY);

		assertThat(counts).isEqualTo(new BatchCounts(10, 8, 2));
		assertThat(repository.existsById("PBLN_9")).isFalse();
		assertThat(response.items()).extracting(SupportProgramResponse::id).containsExactly("PBLN_1", "PBLN_2", "PBLN_6", "PBLN_7", "PBLN_5");
		assertThat(response.checkedAt()).isNotNull();
	}

	@Test
	void unavailableBeforeFirstCollection() {
		var error = assertThrows(BusinessException.class, () -> service.closingSoon(TODAY));
		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
	}
}
