package com.hackathon.server.batch.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.hackathon.server.batch.dto.BatchCounts;

/** 배치 시도 한 번. running_time·created/updated_datetime은 DB가 채운다. */
@Entity
@Table(name = "batch_job_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchJobHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long batchJobId;
	private Integer retryTimes;
	@Enumerated(EnumType.STRING)
	private BatchStatus status;
	private OffsetDateTime startedDatetime;
	private OffsetDateTime finishedDatetime;
	private String errorCode;
	private String errorMessage;
	private Long readCount;
	private Long writeCount;
	private Long skipCount;

	public BatchJobHistory(Long batchJobId, int retryTimes) {
		this.batchJobId = batchJobId;
		this.retryTimes = retryTimes;
		this.status = BatchStatus.RUNNING;
		this.startedDatetime = OffsetDateTime.now();
		this.readCount = 0L;
		this.writeCount = 0L;
		this.skipCount = 0L;
	}

	public void succeed(BatchCounts counts) {
		finish(BatchStatus.SUCCEEDED);
		this.readCount = counts.read();
		this.writeCount = counts.write();
		this.skipCount = counts.skip();
	}

	public void fail(String errorCode, String errorMessage) {
		finish(BatchStatus.FAILED);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	private void finish(BatchStatus status) {
		this.status = status;
		this.finishedDatetime = OffsetDateTime.now();
	}
}
