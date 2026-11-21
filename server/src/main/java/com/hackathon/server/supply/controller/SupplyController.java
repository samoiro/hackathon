package com.hackathon.server.supply.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.supply.dto.SupplyResponse;

@Tag(name = "Supply", description = "부자재")
@RestController
@RequestMapping("/api/supplies")
public class SupplyController {

	@Operation(summary = "부자재 검색", description = "최대 20개")
	@GetMapping
	public List<SupplyResponse> search(@RequestParam(required = false) String query) {
		throw new UnsupportedOperationException("TODO: 부자재 검색");
	}
}
