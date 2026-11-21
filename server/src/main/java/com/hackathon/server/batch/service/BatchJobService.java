package com.hackathon.server.batch.service;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.ErrorResponseException;

import com.hackathon.server.batch.dto.BatchCounts;
import com.hackathon.server.batch.entity.BatchJob;
import com.hackathon.server.batch.entity.BatchJobHistory;
import com.hackathon.server.batch.entity.BatchStatus;
import com.hackathon.server.batch.repository.BatchJobHistoryRepository;
import com.hackathon.server.batch.repository.BatchJobRepository;
import com.hackathon.server.common.exception.BusinessException;

/** 배치 회차(batch_job)와 시도 이력(batch_job_history)을 남기며 작업을 실행한다. 실패하면 batch.max-retry-times까지 다시 시도한다. */
@Service
public class BatchJobService {

	private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);
	private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

	private final BatchJobRepository jobRepository;
	private final BatchJobHistoryRepository historyRepository;
	private final TransactionTemplate tx;
	private final int maxRetryTimes;

	public BatchJobService(BatchJobRepository jobRepository, BatchJobHistoryRepository historyRepository, TransactionTemplate tx,
			@Value("${batch.max-retry-times}") int maxRetryTimes) {
		this.jobRepository = jobRepository;
		this.historyRepository = historyRepository;
		this.tx = tx;
		this.maxRetryTimes = maxRetryTimes;
	}

	// shortcut: 실패하면 바로 다시 시도한다. 공공 API 일시 장애가 잦으면 재시도 사이에 대기를 넣는다
	public BatchJob run(String jobName, Map<String, Object> parameters, Supplier<BatchCounts> task) {
		BatchJob job = jobRepository.save(new BatchJob(jobName, parameters));
		for (int retry = 0; ; retry++) {
			BatchJobHistory history = startAttempt(job, retry);
			try {
				BatchCounts counts = task.get();
				history.succeed(counts);
				job.finish(BatchStatus.SUCCEEDED);
				log.info("{} 성공: read {}, write {}, skip {}", jobName, counts.read(), counts.write(), counts.skip());
				return save(job, history);
			}
			catch (RuntimeException e) {
				history.fail(e instanceof BusinessException b ? b.getErrorCode().name() : e.getClass().getSimpleName(), errorMessage(e));
				boolean last = retry >= maxRetryTimes;
				if (last) job.finish(BatchStatus.FAILED);
				log.warn("{} 실패 (재시도 {}/{})", jobName, retry, maxRetryTimes, e);
				job = save(job, history);
				if (last) return job;
			}
		}
	}

	/** 부모 재시도 횟수 증가와 시도 이력 생성은 같은 트랜잭션에서 한다(V18 주석). */
	private BatchJobHistory startAttempt(BatchJob job, int retryTimes) {
		return tx.execute(s -> {
			job.retry(retryTimes);
			jobRepository.save(job);
			return historyRepository.save(new BatchJobHistory(job.getId(), retryTimes));
		});
	}

	private BatchJob save(BatchJob job, BatchJobHistory history) {
		return tx.execute(s -> {
			// 다음 시도 이력을 넣기 전에 RUNNING을 닫아야 한다(batch_job_history_running_idx)
			historyRepository.saveAndFlush(history);
			return jobRepository.save(job);
		});
	}

	private static String errorMessage(RuntimeException e) {
		// DataGoKrClient 오류 detail에는 인증키가 들어가지 않는다
		String message = e instanceof ErrorResponseException r ? r.getBody().getDetail() : e.getMessage();
		return message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH ? message : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
	}
}
