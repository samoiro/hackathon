package com.hackathon.server.area.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;

import com.hackathon.server.area.dto.AreaResponse;
import com.hackathon.server.area.service.AreaService;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;

@Tag(name = "Area", description = "\"지금 이 지역은\" 장소")
@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

	private final AreaService service;

	@Operation(summary = "장소 목록", description = "가게 좌표를 주면 장소 영역까지 가까운 순(영역 안이면 거리 0), 없으면 이름순. 위도·경도 중 하나만 주면 400")
	@GetMapping
	public List<AreaResponse> areas(
			@Parameter(description = "가게 위도", example = "37.5446") @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") Double latitude,
			@Parameter(description = "가게 경도", example = "127.0559") @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") Double longitude) {
		if ((latitude == null) != (longitude == null)) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "위도와 경도는 함께 보내야 합니다.");
		}
		return service.areas(latitude, longitude);
	}
}
