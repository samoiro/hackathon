package com.hackathon.server.supply.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.supply.dto.SupplyResponse;
import com.hackathon.server.supply.service.SupplyService;

@Tag(name = "Supply", description = "부자재")
@RestController
@RequestMapping("/api/supplies")
public class SupplyController {

	private final SupplyService service;

	public SupplyController(SupplyService service) {
		this.service = service;
	}

	@Operation(summary = "부자재 검색", description = "이름에 query가 들어간 항목 전부를 이름순으로. query가 없으면 전체")
	@GetMapping
	public List<SupplyResponse> search(@RequestParam(required = false) String query) {
		return service.search(query);
	}
}
