package com.hackathon.server.publicdata.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import com.hackathon.server.common.exception.PublicApiErrors;
import com.hackathon.server.publicdata.client.KweatherClient;

@Tag(name = "Kweather", description = "케이웨더 날씨 API (응답 필드명은 원본 API 그대로)")
@RestController
@RequestMapping("/api/kweather")
public class KweatherController {

	private static final String CODE_DESC = "행정구역코드 10자리. 생략하면 전체 조회. 읍면동은 행정동 코드, 시군구는 뒤 5자리 0, 광역시도는 뒤 8자리 0";

	/** 케이웨더 게이트웨이 경로 끝 번호: kw-warning1, kw-fct-wl2 … */
	public enum Level {
		DONG, SIGUNGU, SIDO;

		int number() {
			return ordinal() + 1;
		}
	}

	private final KweatherClient kweather;

	public KweatherController(KweatherClient kweather) {
		this.kweather = kweather;
	}

	@Operation(summary = "동별 기상특보", description = "행정구역별 발효 중인 기상특보 (kw-warning1/2). stnId를 뺀 필드는 배열이고 같은 인덱스가 특보 하나입니다. 주요 필드: warn(특보), warnVar(종류), warnStress(강도), startTime, endTime(해제 전이면 빈 문자열). 발효 중인 특보가 없으면 빈 객체를 돌려줍니다. 광역시도(SIDO) 단위는 없습니다.",
			responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "object", description = "행정구역코드 → {hcode, service.timestamp, data}"))))
	@GetMapping("/warnings")
	public JsonNode warnings(
			@Parameter(description = "조회 단위 (DONG: 읍면동, SIGUNGU: 시군구)") @RequestParam(defaultValue = "DONG") Level level,
			@Parameter(description = CODE_DESC, example = "1111051500") @RequestParam(required = false) @Pattern(regexp = "\\d{10}") String code) {
		if (level == Level.SIDO) {
			throw PublicApiErrors.of(HttpStatus.BAD_REQUEST, "기상특보는 DONG, SIGUNGU 단위만 지원합니다.");
		}
		return kweather.get("kw-warning" + level.number(), code);
	}

	@Operation(summary = "단기예보 (3일)", description = "오전/오후 단위 육상 단기예보 (kw-fct-dl1/2/3). 시각 배열이 없어 service.timestamp(발표시각 05·11·17시)로 시점을 읽습니다. 05시: 오늘 오전부터 6개, 11시: 오늘 오후부터 5개, 17시: 오늘 오후부터 글피 오후까지 7개. 주요 필드: ta(기온 ℃), rnSt(강수확률 %), wIcon, wText(날씨)",
			responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "object", description = "행정구역코드 → {service.timestamp, data}"))))
	@GetMapping("/short-forecast")
	public JsonNode shortForecast(
			@Parameter(description = "조회 단위 (DONG: 읍면동, SIGUNGU: 시군구, SIDO: 광역시도)") @RequestParam(defaultValue = "DONG") Level level,
			@Parameter(description = CODE_DESC, example = "1111051500") @RequestParam(required = false) @Pattern(regexp = "\\d{10}") String code) {
		return kweather.get("kw-fct-dl" + level.number(), code);
	}

	@Operation(summary = "주간예보 (3~10일 후)", description = "육상 주간예보 (kw-fct-wl1/2/3). 배열의 같은 인덱스가 같은 시점이고 tm이 그 시각입니다. 3~7일 후는 오전/오후, 8~10일 후는 하루 단위. 주요 필드: tm, taMin, taMax(℃), rePer(강수확률 %), wIcon, wText(날씨)",
			responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "object", description = "행정구역코드 → {service.timestamp, data}"))))
	@GetMapping("/weekly-forecast")
	public JsonNode weeklyForecast(
			@Parameter(description = "조회 단위 (DONG: 읍면동, SIGUNGU: 시군구, SIDO: 광역시도)") @RequestParam(defaultValue = "DONG") Level level,
			@Parameter(description = CODE_DESC, example = "1111051500") @RequestParam(required = false) @Pattern(regexp = "\\d{10}") String code) {
		return kweather.get("kw-fct-wl" + level.number(), code);
	}
}
