package com.hackathon.server.batch.entity;

import java.time.LocalDate;
import java.util.Map;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 배치 실행 한 회차. created/updated_datetime은 DB 기본값·트리거가 채운다. */
@Entity
@Table(name = "batch_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchJob {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String jobName;
	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> jobParameters;
	private LocalDate targetDate;
	@Enumerated(EnumType.STRING)
	private BatchStatus status;
	private Integer retryTimes;

	public BatchJob(String jobName, LocalDate targetDate) {
		this.jobName = jobName;
		this.jobParameters = Map.of();
		this.targetDate = targetDate;
		this.status = BatchStatus.RUNNING;
		this.retryTimes = 0;
	}

	public void retry(int retryTimes) {
		this.retryTimes = retryTimes;
		this.status = BatchStatus.RUNNING;
	}

	public void finish(BatchStatus status) {
		this.status = status;
	}
}
