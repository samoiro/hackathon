package com.hackathon.server.batch.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hackathon.server.batch.service.BatchJobService;
import com.hackathon.server.common.exception.GlobalExceptionHandler;
import com.hackathon.server.common.repository.ErrorLogRepository;

class BatchControllerTest {

	private final BatchJobService service = mock(BatchJobService.class);
	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BatchController(service))
			.setControllerAdvice(new GlobalExceptionHandler(mock(ErrorLogRepository.class))).build();

	@Test
	void runsWithGivenTargetDate() throws Exception {
		mvc.perform(post("/api/batch/jobs/INGREDIENT_PRICE_DAILY/run").param("targetDate", "2026-11-20"));

		verify(service).run("INGREDIENT_PRICE_DAILY", LocalDate.of(2026, 11, 20));
	}

	@Test
	void rejectsFutureTargetDateBeforeRunning() throws Exception {
		String tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1).toString();

		mvc.perform(post("/api/batch/jobs/INGREDIENT_PRICE_DAILY/run").param("targetDate", tomorrow))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(service);
	}
}
