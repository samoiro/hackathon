package com.hackathon.server.location.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.location.dto.LocationValidateRequest;
import com.hackathon.server.location.dto.LocationValidateResponse;

@Tag(name = "Location", description = "가게 위치")
@RestController
@RequestMapping("/api/locations")
public class LocationController {

	@Operation(summary = "주소 확인", description = "쓸 수 없는 주소도 200 + valid=false, 요청 형식이 틀리면 400")
	@PostMapping("/validate")
	public LocationValidateResponse validate(@Valid @RequestBody LocationValidateRequest request) {
		throw new UnsupportedOperationException("TODO: 주소 확인");
	}
}
