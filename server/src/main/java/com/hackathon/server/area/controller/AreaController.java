package com.hackathon.server.area.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.hackathon.server.area.dto.AreaResponse;
import com.hackathon.server.area.service.AreaService;

@Tag(name = "Area", description = "\"지금 이 지역은\" 장소")
@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

	private final AreaService service;

	@Operation(summary = "장소 목록")
	@GetMapping
	public List<AreaResponse> areas() {
		return service.areas();
	}
}
