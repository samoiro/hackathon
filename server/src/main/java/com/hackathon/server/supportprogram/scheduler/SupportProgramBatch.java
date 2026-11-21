package com.hackathon.server.supportprogram.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hackathon.server.batch.dto.BatchCounts;
import com.hackathon.server.batch.service.BatchJobService;
import com.hackathon.server.batch.service.BatchTask;
import com.hackathon.server.supportprogram.service.SupportProgramService;

/** 지원사업 공고 일배치 스케줄. 실행 내용은 SupportProgramService, 실행·재시도 기록은 BatchJobService. */
@Component
public class SupportProgramBatch implements BatchTask {

	static final String JOB_NAME = "SUPPORT_PROGRAM_DAILY";
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final BatchJobService batchJobService;
	private final SupportProgramService service;

	public SupportProgramBatch(BatchJobService batchJobService, SupportProgramService service) {
		this.batchJobService = batchJobService;
		this.service = service;
	}

	@Scheduled(cron = "${batch.support-program.cron}", zone = "Asia/Seoul")
	public void run() {
		batchJobService.run(this, LocalDate.now(SEOUL));
	}

	@Override
	public String jobName() {
		return JOB_NAME;
	}

	@Override
	public BatchCounts execute(LocalDate targetDate) {
		return service.replace(service.fetchAll());
	}
}
