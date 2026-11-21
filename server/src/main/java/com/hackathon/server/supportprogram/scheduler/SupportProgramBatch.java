package com.hackathon.server.supportprogram.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hackathon.server.batch.service.BatchJobService;
import com.hackathon.server.supportprogram.service.SupportProgramService;

/** 지원사업 공고 일배치 스케줄. 실행 내용은 SupportProgramService, 실행·재시도 기록은 BatchJobService. */
@Component
public class SupportProgramBatch {

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
		batchJobService.run(JOB_NAME, Map.of("runDate", LocalDate.now(SEOUL).toString()),
				() -> service.replace(service.fetchAll()));
	}
}
