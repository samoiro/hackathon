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

import com.hackathon.server.publicdata.client.AgroPriceClient;
import com.hackathon.server.publicdata.dto.ListResponse;

@Tag(name = "Agro", description = "한국농수산식품유통공사(aT) 공공데이터 (응답 필드명은 원본 API 그대로)")
@RestController
@RequestMapping("/api/agro")
public class AgroPriceController {

	private final AgroPriceClient agroPriceClient;

	public AgroPriceController(AgroPriceClient agroPriceClient) {
		this.agroPriceClient = agroPriceClient;
	}

	@Operation(summary = "농산물 가격 추이", description = "조사일 평균가격과 1~4주 전 평균가격. 공공데이터포털 15156069 (B552845/priceSequel). 코드표는 데이터셋 페이지의 참고문서 zip에 있습니다. 주요 필드: item_nm(품목), vrty_nm(품종), grd_nm(등급), unit, exmn_dd_avg_prc(조사일 평균가), ww1~4_bfr_avg_prc(1~4주 전 평균가)")
	@GetMapping("/price-trends")
	public ListResponse priceTrends(
			@Parameter(description = "조사일자 (YYYYMMDD)", example = "20261106", required = true) @RequestParam @Pattern(regexp = "\\d{8}") String date,
			@Parameter(description = "구분코드", example = "02") @RequestParam(required = false) String seCd,
			@Parameter(description = "부류코드", example = "100") @RequestParam(required = false) String categoryCd,
			@Parameter(description = "품목코드") @RequestParam(required = false) String itemCd,
			@Parameter(description = "품종코드") @RequestParam(required = false) String varietyCd,
			@Parameter(description = "등급코드") @RequestParam(required = false) String gradeCd,
			@Parameter(description = "페이지 (1부터)") @RequestParam(defaultValue = "1") @Min(1) int page,
			@Parameter(description = "페이지 크기 (최대 1000)") @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size) {
		Map<String, String> conditions = new LinkedHashMap<>();
		conditions.put("se_cd", seCd);
		conditions.put("ctgry_cd", categoryCd);
		conditions.put("item_cd", itemCd);
		conditions.put("vrty_cd", varietyCd);
		conditions.put("grd_cd", gradeCd);
		return agroPriceClient.priceTrends(date, conditions, page, size);
	}
}
