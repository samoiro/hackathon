package com.hackathon.server.batch.service;

import java.time.LocalDate;

import com.hackathon.server.batch.dto.BatchCounts;

/** 일배치 하나. 재시도·수동 실행은 jobName으로 찾아 batch_job.target_date로 다시 실행한다. */
public interface BatchTask {

	String jobName();

	BatchCounts execute(LocalDate targetDate);
}
