package com.hackathon.server.common;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Test", description = "연동 확인용 테스트 API")
@RestController
@RequestMapping("/api/test")
public class TestController {

	@Operation(summary = "테스트 GET", description = "이름을 받아 인사 메시지와 서버 시간을 돌려줍니다.")
	@GetMapping
	public TestResponse test(
			@Parameter(description = "인사할 이름", example = "해커톤")
			@RequestParam(defaultValue = "world") String name) {
		return new TestResponse("Hello, " + name + "!", OffsetDateTime.now());
	}

	@Schema(description = "테스트 응답")
	public record TestResponse(
			@Schema(description = "인사 메시지", example = "Hello, 해커톤!", requiredMode = Schema.RequiredMode.REQUIRED) String message,
			@Schema(description = "서버 시간", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime serverTime) {
	}
}
