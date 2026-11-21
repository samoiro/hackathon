package com.hackathon.server.supportprogram.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.supportprogram.dto.SupportProgramListResponse;

@Tag(name = "SupportProgram", description = "마감 임박 지원사업")
@RestController
@RequestMapping("/api/support-programs")
public class SupportProgramController {

	@Operation(summary = "마감 임박 지원사업", description = "서울·전국 소상공인 대상, 7일 안 마감 또는 마감일 없음. 최대 3개")
	@GetMapping
	public SupportProgramListResponse supportPrograms() {
		throw new UnsupportedOperationException("TODO: 마감 임박 지원사업");
	}
}
