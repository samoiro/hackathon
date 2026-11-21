package com.hackathon.server.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.hackathon.server.common.exception.ErrorCode;

/** API 에러 한 건. created_datetime은 DB가 채운다. */
@Entity
@Table(name = "error_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Enumerated(EnumType.STRING)
	private ErrorCode errorCode;
	private Integer httpStatus;
	private String httpMethod;
	private String requestPath;
	private String message;
	private String exceptionClass;
	private String stackTrace;

	public ErrorLog(ErrorCode errorCode, int httpStatus, String httpMethod, String requestPath, String message,
			String exceptionClass, String stackTrace) {
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
		this.httpMethod = httpMethod;
		this.requestPath = requestPath;
		this.message = message;
		this.exceptionClass = exceptionClass;
		this.stackTrace = stackTrace;
	}
}
