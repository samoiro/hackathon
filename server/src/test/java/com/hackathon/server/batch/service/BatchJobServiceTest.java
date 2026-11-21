package com.hackathon.server.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.batch.dto.BatchCounts;
import com.hackathon.server.batch.entity.BatchJob;
import com.hackathon.server.batch.entity.BatchStatus;
import com.hackathon.server.batch.repository.BatchJobHistoryRepository;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;

@DataJpaTest(properties = "batch.max-retry-times=3")
@Import({ TestcontainersConfiguration.class, BatchJobService.class, BatchJobServiceTest.FlakyTask.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BatchJobServiceTest {

	private static final LocalDate TARGET_DATE = LocalDate.of(2099, 1, 1);

	/** 처음 failures번은 실패하고 그다음부터 성공한다. */
	static class FlakyTask implements BatchTask {
		final AtomicInteger calls = new AtomicInteger();
		int failures;
		LocalDate lastTargetDate;

		void failFirst(int failures) {
			this.failures = failures;
			calls.set(0);
		}

		@Override
		public String jobName() {
			return "TEST_JOB";
		}

		@Override
		public BatchCounts execute(LocalDate targetDate) {
			lastTargetDate = targetDate;
			if (calls.incrementAndGet() <= failures) throw new IllegalStateException("일시 오류");
			return new BatchCounts(10, 3, 1);
		}
	}

	@Autowired
	BatchJobService service;
	@Autowired
	BatchJobHistoryRepository historyRepository;
	@Autowired
	FlakyTask task;

	private long historiesOf(BatchJob job, BatchStatus status) {
		return historyRepository.findAll().stream()
				.filter(h -> h.getBatchJobId().equals(job.getId()) && h.getStatus().equals(status)).count();
	}

	@Test
	void runTriesOnceAndLeavesFailedJob() {
		task.failFirst(1);

		BatchJob job = service.run(task, TARGET_DATE);

		assertThat(job.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(job.getRetryTimes()).isZero();
		assertThat(historiesOf(job, BatchStatus.FAILED)).isEqualTo(1);
	}

	@Test
	void retryFailedRetriesUntilTaskSucceeds() {
		task.failFirst(3);
		BatchJob failed = service.run(task, TARGET_DATE);

		assertThat(service.retryFailed()).singleElement().satisfies(job -> assertThat(job.getRetryTimes()).isEqualTo(1));
		service.retryFailed();
		assertThat(service.retryFailed()).singleElement().satisfies(job -> {
			assertThat(job.getId()).isEqualTo(failed.getId());
			assertThat(job.getStatus()).isEqualTo(BatchStatus.SUCCEEDED);
			assertThat(job.getRetryTimes()).isEqualTo(3);
		});
		assertThat(historiesOf(failed, BatchStatus.FAILED)).isEqualTo(3);
		assertThat(historiesOf(failed, BatchStatus.SUCCEEDED)).isEqualTo(1);
		assertThat(task.lastTargetDate).isEqualTo(TARGET_DATE);
	}

	@Test
	void retryFailedStopsAtMaxRetryTimesAndSkipsExhaustedJobs() {
		task.failFirst(Integer.MAX_VALUE);
		BatchJob failed = service.run(task, TARGET_DATE);

		service.retryFailed();
		service.retryFailed();
		assertThat(service.retryFailed()).singleElement().satisfies(job -> {
			assertThat(job.getStatus()).isEqualTo(BatchStatus.FAILED);
			assertThat(job.getRetryTimes()).isEqualTo(3);
		});
		assertThat(historiesOf(failed, BatchStatus.FAILED)).isEqualTo(4);
		assertThat(service.retryFailed()).isEmpty();
	}

	@Test
	void runByUnknownJobNameIsNotFound() {
		var error = assertThrows(BusinessException.class, () -> service.run("NO_SUCH_JOB", TARGET_DATE));
		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
	}
}
