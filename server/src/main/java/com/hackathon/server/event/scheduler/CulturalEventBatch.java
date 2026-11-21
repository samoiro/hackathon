package com.hackathon.server.event.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hackathon.server.batch.service.BatchJobService;
import com.hackathon.server.event.service.CulturalEventService;

/** 서울시 문화행사 일배치 스케줄. 실행 내용은 CulturalEventService, 실행·재시도 기록은 BatchJobService. */
@Component
public class CulturalEventBatch {

	static final String JOB_NAME = "CULTURAL_EVENT_DAILY";
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final BatchJobService batchJobService;
	private final CulturalEventService eventService;
	private final boolean runOnStartup;

	public CulturalEventBatch(BatchJobService batchJobService, CulturalEventService eventService,
			@Value("${batch.cultural-event.run-on-startup:false}") boolean runOnStartup) {
		this.batchJobService = batchJobService;
		this.eventService = eventService;
		this.runOnStartup = runOnStartup;
	}

	@Scheduled(cron = "${batch.cultural-event.cron}", zone = "Asia/Seoul")
	public void run() {
		batchJobService.run(JOB_NAME, Map.of("runDate", LocalDate.now(SEOUL).toString()),
				() -> eventService.save(eventService.fetchAll()));
	}

	/** 로컬에서 한 번 적재할 때: --batch.cultural-event.run-on-startup=true (server/README.md) */
	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		if (runOnStartup) run();
	}
}
