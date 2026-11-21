package com.hackathon.server.industry.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.industry.dto.IndustryResponse;
import com.hackathon.server.industry.dto.RecommendedItemsResponse;
import com.hackathon.server.industry.service.IndustryService;

@Tag(name = "Industry", description = "업종")
@RestController
@RequestMapping("/api/industries")
public class IndustryController {

	private final IndustryService service;

	public IndustryController(IndustryService service) {
		this.service = service;
	}

	@Operation(summary = "업종 목록", description = "전체 업종 28개")
	@GetMapping
	public List<IndustryResponse> industries() {
		return service.industries();
	}

	@Operation(summary = "업종 추천 품목", description = "업종 기본 원자재 최대 3 · 부자재 최대 2 (\"한 번에 담기\"). 없는 업종은 404")
	@GetMapping("/{industryId}/recommended-items")
	public RecommendedItemsResponse recommendedItems(@PathVariable long industryId) {
		return service.recommendedItems(industryId);
	}
}
