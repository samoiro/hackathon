package com.hackathon.server.common.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.hackathon.server.common.entity.ErrorLog;
import com.hackathon.server.common.repository.ErrorLogRepository;

/**
 * 모든 API 에러를 ProblemDetail + code 속성으로 응답하고 error_log에 남긴다.
 * 서비스에서는 BusinessException(ErrorCode, detail)을 던진다. 작성법은 server/docs/exception-guide.md
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private final ErrorLogRepository errorLogRepository;

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ProblemDetail body = ProblemDetail.forStatusAndDetail(status, ErrorCode.INTERNAL_ERROR.getMessage());
		return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, status, request);
		ErrorCode code = ex instanceof BusinessException b ? b.getErrorCode() : ErrorCode.from(status);
		String detail = null;
		if (response != null && response.getBody() instanceof ProblemDetail problem) {
			problem.setProperty("code", code.name());
			detail = problem.getDetail();
		}
		if (status.is5xxServerError()) {
			log.error("API 에러 {} {}", code, detail, ex);
		}
		// 스캐너·봇의 없는 경로 요청까지 쌓이지 않게 정적 리소스 404는 남기지 않는다
		if (!(ex instanceof NoResourceFoundException)) {
			save(code, status, detail, ex, request);
		}
		return response;
	}

	private void save(ErrorCode code, HttpStatusCode status, @Nullable String detail, Exception ex, WebRequest request) {
		try {
			var servlet = ((ServletWebRequest) request).getRequest();
			errorLogRepository.save(new ErrorLog(code, status.value(), servlet.getMethod(), servlet.getRequestURI(), detail,
					ex.getClass().getName(), status.is5xxServerError() ? stackTrace(ex) : null));
		} catch (RuntimeException e) {
			// 로그 저장 실패(DB 장애 등)가 원래 에러 응답을 가리지 않게 한다
			log.warn("error_log 저장 실패: {}", e.toString());
		}
	}

	private static String stackTrace(Exception ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
}
