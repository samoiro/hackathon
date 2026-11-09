package com.hackathon.server.common;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Common", description = "공통 API")
@RestController
public class PingController {

	@Operation(summary = "서버 상태 확인", description = "서버가 살아있는지 확인합니다.")
	@GetMapping("/api/ping")
	public Map<String, String> ping() {
		return Map.of("status", "ok");
	}
}
