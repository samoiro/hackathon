package com.hackathon.server.batch.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.hackathon.server.batch.entity.BatchJob;
import com.hackathon.server.batch.entity.BatchStatus;

@Schema(description = "배치 실행 결과")
public record BatchJobResponse(
		@Schema(description = "batch_job ID") Long id,
		@Schema(description = "작업명", example = "INGREDIENT_PRICE_DAILY") String jobName,
		@Schema(description = "기준일. 원자재 가격은 전날 하루를 받는다") LocalDate targetDate,
		@Schema(description = "상태") BatchStatus status,
		@Schema(description = "재시도 횟수. 최초 실행 0") Integer retryTimes) {

	public static BatchJobResponse from(BatchJob job) {
		return new BatchJobResponse(job.getId(), job.getJobName(), job.getTargetDate(), job.getStatus(), job.getRetryTimes());
	}
}
