package com.hackathon.server.batch.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.batch.dto.BatchJobResponse;
import com.hackathon.server.batch.service.BatchJobService;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;

// shortcut: 인증 없이 누구나 호출할 수 있다. 외부에 공개하기 전에 관리자 인증을 붙인다
@Slf4j
@Tag(name = "Batch", description = "배치 수동 실행·재시도")
@RestController
@RequestMapping("/api/batch")
public class BatchController {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final BatchJobService service;

	public BatchController(BatchJobService service) {
		this.service = service;
	}

	@Operation(summary = "배치 수동 실행", description = "스케줄이 돌지 않은 기준일로 한 번 실행한다. 비우면 오늘. 원자재는 기준일 전날 가격을 받는다. 없는 작업명은 404, 미래 날짜는 400")
	@PostMapping("/jobs/{jobName}/run")
	public BatchJobResponse run(
			@Parameter(description = "작업명", example = "INGREDIENT_PRICE_DAILY") @PathVariable String jobName,
			@Parameter(description = "기준일(yyyy-MM-dd). 스케줄이 돌았어야 할 날짜", example = "2026-11-21")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
		LocalDate today = LocalDate.now(SEOUL);
		if (targetDate != null && targetDate.isAfter(today)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "기준일은 오늘 이후일 수 없습니다.");
		}
		LocalDate date = targetDate == null ? today : targetDate;
		log.info("배치 수동 실행 요청: {} {}", jobName, date);
		return BatchJobResponse.from(service.run(jobName, date));
	}

	@Operation(summary = "실패 배치 재시도", description = "재시도가 남은(3회 미만) FAILED 작업을 모아 한 번씩 다시 실행한다. 스케줄은 하루 3번 자동으로 부른다")
	@PostMapping("/retry")
	public List<BatchJobResponse> retryFailed() {
		List<BatchJobResponse> results = service.retryFailed().stream().map(BatchJobResponse::from).toList();
		log.info("실패 배치 수동 재시도: {}건", results.size());
		return results;
	}
}
