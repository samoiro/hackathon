package com.hackathon.server.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** API 에러 코드. DB error_code 테이블(V28)과 같은 목록이어야 한다. 추가 방법은 server/docs/exception-guide.md */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통: Spring이 던지는 예외는 status로 이 중 하나가 정해진다 (from)
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
	CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
	EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다."),
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스를 일시적으로 사용할 수 없습니다."),
	GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 API 응답 시간이 초과되었습니다."),

	// 공공데이터 API (서울시·공공데이터포털·카카오·케이웨더)
	PUBLIC_API_KEY_MISSING(HttpStatus.SERVICE_UNAVAILABLE, "공공데이터 API 인증키가 설정되지 않았습니다."),
	PUBLIC_API_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "공공데이터 API에 연결하지 못했습니다."),
	PUBLIC_API_BAD_REQUEST(HttpStatus.BAD_REQUEST, "공공데이터 API가 요청 값을 거부했습니다."),
	PUBLIC_API_ERROR(HttpStatus.BAD_GATEWAY, "공공데이터 API가 오류를 응답했습니다."),
	PUBLIC_API_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "공공데이터 API 응답이 비어 있거나 형식이 올바르지 않습니다."),

	// 분석
	ANALYSIS_OUTSIDE_SEOUL(HttpStatus.UNPROCESSABLE_CONTENT, "서울 밖 주소는 아직 지원하지 않아요.");

	private final HttpStatus status;
	private final String message;

	/** BusinessException이 아닌 예외(Spring 검증·라우팅 오류 등)는 응답 status로 공통 코드를 고른다. */
	public static ErrorCode from(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> BAD_REQUEST;
			case 404 -> NOT_FOUND;
			case 405 -> METHOD_NOT_ALLOWED;
			case 409 -> CONFLICT;
			case 429 -> TOO_MANY_REQUESTS;
			case 502 -> EXTERNAL_API_ERROR;
			case 503 -> SERVICE_UNAVAILABLE;
			case 504 -> GATEWAY_TIMEOUT;
			default -> status.is4xxClientError() ? BAD_REQUEST : INTERNAL_ERROR;
		};
	}
}
