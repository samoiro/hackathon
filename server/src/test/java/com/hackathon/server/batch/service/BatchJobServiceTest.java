package com.hackathon.server.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
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

@DataJpaTest(properties = "batch.max-retry-times=3")
@Import({ TestcontainersConfiguration.class, BatchJobService.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BatchJobServiceTest {

	@Autowired
	BatchJobService service;
	@Autowired
	BatchJobHistoryRepository historyRepository;

	private long historiesOf(BatchJob job, BatchStatus status) {
		return historyRepository.findAll().stream()
				.filter(h -> h.getBatchJobId().equals(job.getId()) && h.getStatus().equals(status)).count();
	}

	@Test
	void retriesUntilTaskSucceeds() {
		AtomicInteger calls = new AtomicInteger();
		BatchJob job = service.run("TEST_JOB", Map.of("runDate", "2099-01-01"), () -> {
			if (calls.incrementAndGet() <= 2) throw new IllegalStateException("일시 오류");
			return new BatchCounts(10, 3, 1);
		});

		assertThat(job.getStatus()).isEqualTo(BatchStatus.SUCCEEDED);
		assertThat(job.getRetryTimes()).isEqualTo(2);
		assertThat(historiesOf(job, BatchStatus.FAILED)).isEqualTo(2);
		assertThat(historyRepository.findAll()).filteredOn(h -> h.getBatchJobId().equals(job.getId()) && h.getStatus().equals(BatchStatus.SUCCEEDED))
				.singleElement().satisfies(h -> assertThat(h.getWriteCount()).isEqualTo(3));
	}

	@Test
	void failsAfterMaxRetryTimes() {
		BatchJob job = service.run("TEST_JOB", Map.of(), () -> {
			throw new IllegalStateException("계속 실패");
		});

		assertThat(job.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(job.getRetryTimes()).isEqualTo(3);
		assertThat(historiesOf(job, BatchStatus.FAILED)).isEqualTo(4);
		assertThat(historyRepository.findAll()).filteredOn(h -> h.getBatchJobId().equals(job.getId()))
				.allSatisfy(h -> assertThat(h.getErrorMessage()).isEqualTo("계속 실패"));
	}
}
