package com.hackathon.server.analysis.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.analysis.dto.AnalysisCreatedResponse;
import com.hackathon.server.analysis.dto.AnalysisRequest;
import com.hackathon.server.analysis.dto.AnalysisResponse;
import com.hackathon.server.analysis.dto.SuggestionFeedbackRequest;

@Tag(name = "Analysis", description = "분석 요청·상태·피드백")
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

	@Operation(summary = "분석 시작", description = "약 1분 걸려 번호만 바로 돌려줌. 서울 밖 422, 품목 개수 초과 400")
	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public AnalysisCreatedResponse create(@Valid @RequestBody AnalysisRequest request) {
		throw new UnsupportedOperationException("TODO: 분석 시작");
	}

	@Operation(summary = "분석 상태", description = "1~2초마다 다시 물음. SUCCEEDED면 briefing, FAILED면 failure")
	@GetMapping("/{id}")
	public AnalysisResponse get(@PathVariable UUID id) {
		throw new UnsupportedOperationException("TODO: 분석 상태");
	}

	@Operation(summary = "추천 카드 피드백", description = "카드 1장에 1표, 다시 보내면 표를 바꿈")
	@PostMapping("/{id}/feedback")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void feedback(@PathVariable UUID id, @Valid @RequestBody SuggestionFeedbackRequest request) {
		throw new UnsupportedOperationException("TODO: 추천 카드 피드백");
	}
}
