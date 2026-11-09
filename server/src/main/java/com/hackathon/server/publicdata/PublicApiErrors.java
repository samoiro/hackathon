package com.hackathon.server.publicdata;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

final class PublicApiErrors {

	private PublicApiErrors() {
	}

	static ErrorResponseException of(HttpStatus status, String detail) {
		return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
	}
}
