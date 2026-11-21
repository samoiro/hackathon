package com.hackathon.server.analysis.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import lombok.RequiredArgsConstructor;

import com.hackathon.server.analysis.dto.AiAnalyzeRequest;
import com.hackathon.server.analysis.dto.AiAnalyzeResponse;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;

/** Python ai-server 호출. 재시도하지 않는다(계약: 화면의 [다시 시도]에 맡김). */
@Component
@RequiredArgsConstructor
public class AiServerClient {

	private final RestClient aiServerRestClient;

	public AiAnalyzeResponse analyze(AiAnalyzeRequest request) {
		AiAnalyzeResponse body;
		try {
			body = aiServerRestClient.post().uri("/v1/analyze").contentType(MediaType.APPLICATION_JSON).body(request).retrieve()
					.body(AiAnalyzeResponse.class);
		}
		catch (RestClientResponseException e) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
					"ai-server 오류 (HTTP " + e.getStatusCode().value() + " " + errorCode(e) + ")");
		}
		catch (RestClientException e) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "ai-server 호출 실패: " + e.getMostSpecificCause().getMessage());
		}
		if (body == null || body.suggestions() == null) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "ai-server 응답이 비어 있습니다.");
		}
		return body;
	}

	private static String errorCode(RestClientResponseException e) {
		try {
			AiAnalyzeResponse.Error error = e.getResponseBodyAs(AiAnalyzeResponse.Error.class);
			return error != null && error.code() != null ? error.code() : "UNKNOWN";
		}
		catch (RuntimeException ignored) {
			// JSON이 아닌 본문(프록시 오류 페이지 등)
			return "UNKNOWN";
		}
	}
}
