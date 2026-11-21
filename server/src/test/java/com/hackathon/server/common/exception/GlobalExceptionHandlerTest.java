package com.hackathon.server.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.common.entity.ErrorLog;
import com.hackathon.server.common.repository.ErrorLogRepository;

class GlobalExceptionHandlerTest {

	@RestController
	static class FailingController {

		@GetMapping("/test/upstream")
		String upstream() {
			throw new BusinessException(ErrorCode.PUBLIC_API_CONNECTION_FAILED, "카카오 API 호출 실패");
		}

		@GetMapping("/test/bug")
		String bug() {
			throw new IllegalStateException("내부 상태 깨짐");
		}
	}

	private final ErrorLogRepository repository = mock(ErrorLogRepository.class);
	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
			.setControllerAdvice(new GlobalExceptionHandler(repository)).build();

	private ErrorLog savedLog() {
		ArgumentCaptor<ErrorLog> captor = ArgumentCaptor.forClass(ErrorLog.class);
		verify(repository).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void businessExceptionKeepsCodeAndDetail() throws Exception {
		mvc.perform(get("/test/upstream"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("PUBLIC_API_CONNECTION_FAILED"))
				.andExpect(jsonPath("$.detail").value("카카오 API 호출 실패"));

		ErrorLog log = savedLog();
		assertThat(log.getErrorCode()).isEqualTo(ErrorCode.PUBLIC_API_CONNECTION_FAILED);
		assertThat(log.getRequestPath()).isEqualTo("/test/upstream");
		assertThat(log.getMessage()).isEqualTo("카카오 API 호출 실패");
		assertThat(log.getStackTrace()).isNotBlank();
	}

	@Test
	void unexpectedExceptionHidesMessageButLogsIt() throws Exception {
		mvc.perform(get("/test/bug"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.detail").value(ErrorCode.INTERNAL_ERROR.getMessage()));

		ErrorLog log = savedLog();
		assertThat(log.getExceptionClass()).isEqualTo(IllegalStateException.class.getName());
		assertThat(log.getStackTrace()).contains("내부 상태 깨짐");
	}

	@Test
	void clientErrorIsLoggedWithoutStackTrace() throws Exception {
		mvc.perform(post("/test/bug"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

		ErrorLog log = savedLog();
		assertThat(log.getHttpMethod()).isEqualTo("POST");
		assertThat(log.getStackTrace()).isNull();
	}

	@Test
	void logSaveFailureDoesNotChangeResponse() throws Exception {
		org.mockito.Mockito.when(repository.save(any())).thenThrow(new IllegalStateException("DB 다운"));

		mvc.perform(get("/test/upstream")).andExpect(status().isBadGateway());
	}

	@Test
	void unknownStatusFallsBackBySeries() {
		assertThat(ErrorCode.from(HttpStatus.UNAUTHORIZED)).isEqualTo(ErrorCode.BAD_REQUEST);
		assertThat(ErrorCode.from(HttpStatus.NOT_IMPLEMENTED)).isEqualTo(ErrorCode.INTERNAL_ERROR);
		assertThat(ErrorCode.from(HttpStatus.BAD_GATEWAY)).isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
	}
}
