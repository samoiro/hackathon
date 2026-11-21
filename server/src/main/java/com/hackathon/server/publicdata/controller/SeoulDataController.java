package com.hackathon.server.publicdata.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.hackathon.server.publicdata.client.SeoulOpenApiClient;
import com.hackathon.server.publicdata.dto.ListResponse;

@Tag(name = "Seoul", description = "서울 열린데이터광장 공공데이터 (응답 필드명은 원본 API 그대로)")
@RestController
@RequestMapping("/api/seoul")
public class SeoulDataController {

	private static final String AREA_DESC = "장소명 또는 장소코드 (예: 광화문·덕수궁, POI009). 서울시 주요 120개 장소 목록은 데이터셋 OA-21285 페이지에서 확인. 없는 장소면 서울시가 ERROR-500을 줘서 502로 응답합니다.";

	private final SeoulOpenApiClient seoul;

	public SeoulDataController(SeoulOpenApiClient seoul) {
		this.seoul = seoul;
	}

	@Operation(summary = "생필품 농수축산물 가격", description = "자치구별 전통시장·대형마트 품목 가격 (매주 화요일 갱신). 데이터셋 OA-1170 / 서비스 ListNecessariesPricesService. 주요 필드: M_NAME(시장명), A_NAME(품목), A_PRICE(가격), M_GU_NAME(자치구), P_DATE(점검일자)")
	@GetMapping("/necessaries-prices")
	public ListResponse necessariesPrices(
			@Parameter(description = "품목 이름 (정확히 일치)", example = "쌀(이천쌀) 4kg 1포") @RequestParam(required = false) String itemName,
			@Parameter(description = "년도-월", example = "2026-11") @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}") String yearMonth,
			@Parameter(description = "시장유형", example = "전통시장") @RequestParam(required = false) String marketType,
			@Parameter(description = "자치구", example = "강남구") @RequestParam(required = false) String gu,
			@Parameter(description = "시장/마트 이름 (부분 일치)") @RequestParam(required = false) String marketName,
			@Parameter(description = "페이지 (1부터)") @RequestParam(defaultValue = "1") @Min(1) int page,
			@Parameter(description = "페이지 크기 (최대 1000)") @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size) {
		return seoul.list("ListNecessariesPricesService", page, size, itemName, yearMonth, marketType, gu, marketName);
	}

	@Operation(summary = "문화행사 정보", description = "서울문화포털 공연·전시·축제 등 (매일 갱신). 데이터셋 OA-23041 / 서비스 aiViewCulture. 주요 필드: TITLE, CODENAME(분류), GUNAME(자치구), PLACE, STRTDATE, END_DATE, IS_FREE, MAIN_IMG, LAT, LOT")
	@GetMapping("/cultural-events")
	public ListResponse culturalEvents(
			@Parameter(description = "분류", example = "콘서트") @RequestParam(required = false) String category,
			@Parameter(description = "공연/행사명") @RequestParam(required = false) String title,
			@Parameter(description = "날짜 (YYYY-MM-DD)", example = "2026-12-24") @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date,
			@Parameter(description = "페이지 (1부터)") @RequestParam(defaultValue = "1") @Min(1) int page,
			@Parameter(description = "페이지 크기 (최대 1000)") @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size) {
		return seoul.list("aiViewCulture", page, size, category, title, date);
	}

	@Operation(summary = "실시간 도시데이터", description = "장소별 인구·도로·주차·지하철·버스·날씨·문화행사 등 종합 (5분 단위 갱신, 응답이 큼). 데이터셋 OA-21285 / 서비스 citydata. CITYDATA 객체를 그대로 돌려줍니다.",
			responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "object"))))
	@GetMapping("/city-data/{area}")
	public JsonNode cityData(@Parameter(description = AREA_DESC, example = "광화문·덕수궁") @PathVariable String area) {
		return seoul.realtime("citydata", area).path("CITYDATA");
	}

	@Operation(summary = "실시간 인구", description = "장소별 혼잡도, 실시간 인구 범위, 성별·연령 비율, 12시간 인구 예측(FCST_PPLTN). 데이터셋 OA-21778 / 서비스 citydata_ppltn.",
			responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "object"))))
	@GetMapping("/population/{area}")
	public JsonNode population(@Parameter(description = AREA_DESC, example = "광화문·덕수궁") @PathVariable String area) {
		return seoul.realtime("citydata_ppltn", area).path("SeoulRtd.citydata_ppltn").path(0);
	}

	@Operation(summary = "실시간 상권 현황", description = "장소별 카드 결제 기반 상권 활성도, 업종별(CMRCL_RSB) 결제 건수·금액, 성별·연령 소비 비율. 데이터셋 OA-22385 / 서비스 citydata_cmrcl.",
			responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "object"))))
	@GetMapping("/commercial/{area}")
	public JsonNode commercial(@Parameter(description = AREA_DESC, example = "광화문·덕수궁") @PathVariable String area) {
		ObjectNode root = (ObjectNode) seoul.realtime("citydata_cmrcl", area);
		root.remove("RESULT");
		root.remove("list_total_count");
		return root;
	}
}
