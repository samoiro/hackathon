package com.hackathon.server.location.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.location.dto.LocationValidateRequest;
import com.hackathon.server.location.dto.LocationValidateResponse;
import com.hackathon.server.publicdata.client.KakaoLocalClient;
import com.hackathon.server.publicdata.dto.AdminDong;

/** 가게 정보 화면의 주소 확인. 분석 시작(AnalysisService.create)과 같은 기준: 좌표의 행정동 시도가 서울특별시인지 */
@Service
@RequiredArgsConstructor
public class LocationService {

	private static final String SEOUL = "서울특별시";

	private final KakaoLocalClient kakao;

	public LocationValidateResponse validate(LocationValidateRequest request) {
		AdminDong dong = kakao.adminDong(request.latitude(), request.longitude());
		if (dong == null || !SEOUL.equals(dong.sido())) {
			return new LocationValidateResponse(false, ErrorCode.ANALYSIS_OUTSIDE_SEOUL.getMessage());
		}
		return new LocationValidateResponse(true, null);
	}
}
