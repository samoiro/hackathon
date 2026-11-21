package com.hackathon.server.common.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import lombok.Getter;

/**
 * 서비스에서 던지는 예외. ErrorCode의 status로 응답하고 code를 그대로 남긴다.
 * cause는 받지 않는다: 외부 API 예외 메시지에 인증키가 든 URL이 섞여 error_log stack trace로 새기 때문.
 */
@Getter
public class BusinessException extends ErrorResponseException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	public BusinessException(ErrorCode errorCode, String detail) {
		super(errorCode.getStatus(), ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail), null);
		this.errorCode = errorCode;
	}
}
