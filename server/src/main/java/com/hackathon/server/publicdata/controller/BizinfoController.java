package com.hackathon.server.publicdata.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.publicdata.client.DataGoKrClient;
import com.hackathon.server.publicdata.dto.ListResponse;

@Tag(name = "Bizinfo", description = "기업마당 지원사업 공고 (응답 필드명은 원본 API 그대로)")
@RestController
@RequestMapping("/api/bizinfo")
public class BizinfoController {

	private final DataGoKrClient dataGoKrClient;

	public BizinfoController(DataGoKrClient dataGoKrClient) {
		this.dataGoKrClient = dataGoKrClient;
	}

	@Operation(summary = "지원사업 공고", description = "중앙부처·지자체·유관기관 지원사업 공고, 최신 등록순. 공공데이터포털 15157820 (1421000/bizinfo). 출처표시·변경금지(공공누리 제3유형). 주요 필드: pblancNm(공고명), pblancUrl(기업마당 URL), jrsdInsttNm(소관기관), excInsttNm(수행기관), pldirSportRealmLclasCodeNm(분야), reqstBeginEndDe(신청기간, 예: 2026-11-18 ~ 2026-11-25), trgetNm(지원대상), hashtags(쉼표 구분, 지역명 포함), bsnsSumryCn(사업개요, HTML)")
	@GetMapping("/support-programs")
	public ListResponse supportPrograms(
			@Parameter(description = "분야: 01 금융, 02 기술, 03 인력, 04 수출, 05 내수, 06 창업, 07 경영, 09 기타", example = "07") @RequestParam(required = false) @Pattern(regexp = "\\d{2}") String field,
			@Parameter(description = "해시태그, 쉼표로 여러 개면 하나라도 맞으면 포함", example = "소상공인") @RequestParam(required = false) String hashtags,
			@Parameter(description = "공고 ID", example = "PBLN_000000000128062") @RequestParam(required = false) String pblancId,
			@Parameter(description = "등록일자 (YYYYMMDD)", example = "20261120") @RequestParam(required = false) @Pattern(regexp = "\\d{8}") String registeredDate,
			@Parameter(description = "페이지 (1부터)") @RequestParam(defaultValue = "1") @Min(1) int page,
			@Parameter(description = "페이지 크기 (최대 1000)") @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size) {
		Map<String, String> query = new LinkedHashMap<>();
		query.put("dataType", "json");
		query.put("pageNo", String.valueOf(page));
		query.put("numOfRows", String.valueOf(size));
		query.put("searchLclasId", field);
		query.put("hashtags", hashtags);
		query.put("pblancId", pblancId);
		query.put("registDe", registeredDate);
		return dataGoKrClient.list("/1421000/bizinfo/pblancBsnsService", query);
	}
}
