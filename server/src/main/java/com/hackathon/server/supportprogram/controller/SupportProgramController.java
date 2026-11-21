package com.hackathon.server.supportprogram.controller;

import java.time.LocalDate;
import java.time.ZoneId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.supportprogram.dto.SupportProgramListResponse;
import com.hackathon.server.supportprogram.service.SupportProgramService;

@Tag(name = "SupportProgram", description = "마감 임박 지원사업")
@RestController
@RequestMapping("/api/support-programs")
public class SupportProgramController {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final SupportProgramService service;

	public SupportProgramController(SupportProgramService service) {
		this.service = service;
	}

	@Operation(summary = "마감 임박 지원사업", description = "서울·전국 소상공인 대상, 오늘 이후 마감. 마감일 빠른 순 최대 5개. 일배치가 한 번도 돌지 않았으면 503")
	@GetMapping
	public SupportProgramListResponse supportPrograms() {
		return service.closingSoon(LocalDate.now(SEOUL));
	}
}
