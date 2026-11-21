package com.hackathon.server.ingredient.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hackathon.server.batch.service.BatchJobService;
import com.hackathon.server.ingredient.service.IngredientPriceService;

/** 원자재 가격 일배치 스케줄. 실행 내용은 IngredientPriceService, 실행·재시도 기록은 BatchJobService. */
@Component
public class IngredientPriceBatch {

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
		LocalDate today = LocalDate.now(SEOUL);
		batchJobService.run(JOB_NAME, Map.of("runDate", today.toString()),
				() -> priceService.saveAndRemap(priceService.fetchPrices(today)));
	}
}
