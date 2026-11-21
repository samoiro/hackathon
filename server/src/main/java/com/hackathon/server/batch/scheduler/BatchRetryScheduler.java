package com.hackathon.server.batch.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hackathon.server.batch.service.BatchJobService;

/** 실패한 일배치 재시도 스케줄. 하루 3번 돌아 작업마다 최대 3번(batch.max-retry-times) 다시 시도한다. */
@Component
public class BatchRetryScheduler {

	private final BatchJobService batchJobService;

	public BatchRetryScheduler(BatchJobService batchJobService) {
		this.batchJobService = batchJobService;
	}

	@Scheduled(cron = "${batch.retry.cron}", zone = "Asia/Seoul")
	public void retryFailed() {
		batchJobService.retryFailed();
	}
}
