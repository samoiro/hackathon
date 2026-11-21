package com.hackathon.server.publicdata.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.publicdata.client.KakaoLocalClient;
import com.hackathon.server.publicdata.dto.AdminDong;

@Tag(name = "Kakao", description = "카카오 로컬 API")
@RestController
@RequestMapping("/api/kakao")
public class KakaoController {

	private final KakaoLocalClient kakao;

	public KakaoController(KakaoLocalClient kakao) {
		this.kakao = kakao;
	}

	@Operation(summary = "좌표 → 행정동", description = "좌표가 속한 행정동 코드(10자리)와 이름. 코드는 /api/kweather의 code에 그대로 씁니다. 행정구역이 없는 좌표(바다·국외)면 404.")
	@GetMapping("/admin-dong")
	public AdminDong adminDong(
			@Parameter(description = "위도", example = "37.5446") @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude,
			@Parameter(description = "경도", example = "127.0446") @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude) {
		AdminDong dong = kakao.adminDong(latitude, longitude);
		if (dong == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "이 좌표에 해당하는 행정동이 없습니다.");
		}
		return dong;
	}
}
