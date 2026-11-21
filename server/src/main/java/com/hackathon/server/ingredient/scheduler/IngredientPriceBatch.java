package com.hackathon.server.ingredient.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hackathon.server.batch.dto.BatchCounts;
import com.hackathon.server.batch.service.BatchJobService;
import com.hackathon.server.batch.service.BatchTask;
import com.hackathon.server.ingredient.service.IngredientPriceService;

/** 원자재 가격 일배치 스케줄. 실행 내용은 IngredientPriceService, 실행·재시도 기록은 BatchJobService. */
@Component
public class IngredientPriceBatch implements BatchTask {

	static final String JOB_NAME = "INGREDIENT_PRICE_DAILY";
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final BatchJobService batchJobService;
	private final IngredientPriceService priceService;

	public IngredientPriceBatch(BatchJobService batchJobService, IngredientPriceService priceService) {
		this.batchJobService = batchJobService;
		this.priceService = priceService;
	}

	@Scheduled(cron = "${batch.ingredient-price.cron}", zone = "Asia/Seoul")
	public void run() {
		batchJobService.run(this, LocalDate.now(SEOUL));
	}

	@Override
	public String jobName() {
		return JOB_NAME;
	}

	@Override
	public BatchCounts execute(LocalDate targetDate) {
		return priceService.saveAndRemap(priceService.fetchPrices(targetDate));
	}
}
