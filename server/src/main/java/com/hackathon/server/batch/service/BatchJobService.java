package com.hackathon.server.batch.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
import com.hackathon.server.common.exception.ErrorCode;

/** 배치 회차(batch_job)와 시도 이력(batch_job_history)을 남기며 작업을 실행한다. 실패한 작업은 retryFailed로 batch.max-retry-times까지 다시 시도한다. */
@Service
public class BatchJobService {

	private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);
	private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

	private final BatchJobRepository jobRepository;
	private final BatchJobHistoryRepository historyRepository;
	private final TransactionTemplate tx;
	private final int maxRetryTimes;
	// BatchTask 구현이 이 서비스를 주입받아서 ObjectProvider로 늦게 꺼낸다(순환 의존)
	private final ObjectProvider<BatchTask> tasks;

	public BatchJobService(BatchJobRepository jobRepository, BatchJobHistoryRepository historyRepository, TransactionTemplate tx,
			@Value("${batch.max-retry-times}") int maxRetryTimes, ObjectProvider<BatchTask> tasks) {
		this.jobRepository = jobRepository;
		this.historyRepository = historyRepository;
		this.tx = tx;
		this.maxRetryTimes = maxRetryTimes;
		this.tasks = tasks;
	}

	/** 스케줄·수동 실행. 한 번만 시도하고, 실패한 작업은 retryFailed가 다시 돌린다. */
	public BatchJob run(BatchTask task, LocalDate targetDate) {
		BatchJob job = jobRepository.save(new BatchJob(task.jobName(), targetDate));
		return attempt(job, task);
	}

	public BatchJob run(String jobName, LocalDate targetDate) {
		return run(task(jobName), targetDate);
	}

	/** 재시도가 남은 FAILED 작업을 모아 한 번씩 다시 돌린다. 하루 3번 BatchRetryScheduler가 부른다. */
	// shortcut: 서버 한 대 기준 synchronized로 중복 호출만 막는다. 서버를 여러 대 띄우면 DB 락으로 바꾼다
	public synchronized List<BatchJob> retryFailed() {
		return jobRepository.findByStatusAndRetryTimesLessThanOrderById(BatchStatus.FAILED, maxRetryTimes).stream()
				.filter(job -> findTask(job.getJobName()).isPresent())
				.map(this::retry)
				.toList();
	}

	private BatchJob retry(BatchJob job) {
		job.retry(job.getRetryTimes() + 1);
		return attempt(job, task(job.getJobName()));
	}

	private BatchJob attempt(BatchJob job, BatchTask task) {
		BatchJobHistory history = startAttempt(job);
		try {
			BatchCounts counts = task.execute(job.getTargetDate());
			history.succeed(counts);
			job.finish(BatchStatus.SUCCEEDED);
			log.info("{} 성공: read {}, write {}, skip {}", job.getJobName(), counts.read(), counts.write(), counts.skip());
		}
		catch (RuntimeException e) {
			history.fail(e instanceof BusinessException b ? b.getErrorCode().name() : e.getClass().getSimpleName(), errorMessage(e));
			job.finish(BatchStatus.FAILED);
			log.warn("{} 실패 (재시도 {}/{})", job.getJobName(), job.getRetryTimes(), maxRetryTimes, e);
		}
		return save(job, history);
	}

	private Optional<BatchTask> findTask(String jobName) {
		return tasks.stream().filter(t -> t.jobName().equals(jobName)).findFirst();
	}

	private BatchTask task(String jobName) {
		return findTask(jobName).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "없는 배치 작업입니다: " + jobName));
	}

	/** 부모 재시도 횟수 증가와 시도 이력 생성은 같은 트랜잭션에서 한다(V18 주석). */
	private BatchJobHistory startAttempt(BatchJob job) {
		return tx.execute(s -> {
			jobRepository.save(job);
			return historyRepository.save(new BatchJobHistory(job.getId(), job.getRetryTimes()));
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
