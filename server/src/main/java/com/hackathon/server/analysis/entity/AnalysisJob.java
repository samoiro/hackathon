package com.hackathon.server.analysis.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.hackathon.server.analysis.dto.AnalysisStatus;

/**
 * 분석 한 건 (briefing_analysis_job). jsonb 칸은 서비스가 JSON 문자열로 만들어 넣는다.
 * created/updated_datetime은 DB 기본값·트리거가 채운다.
 */
@Entity
@Table(name = "briefing_analysis_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisJob {

	@Id
	private UUID id;
	@Enumerated(EnumType.STRING)
	private AnalysisStatus status;
	@JdbcTypeCode(SqlTypes.JSON)
	private String steps;
	private OffsetDateTime baseTime;
	@JdbcTypeCode(SqlTypes.JSON)
	private String inputSnapshot;
	@JdbcTypeCode(SqlTypes.JSON)
	private String eventSearch;
	@JdbcTypeCode(SqlTypes.JSON)
	private String evidenceSnapshot;
	@JdbcTypeCode(SqlTypes.JSON)
	private String finalResult;
	@JdbcTypeCode(SqlTypes.JSON)
	private String failure;
	private String scoreVersion;

	public AnalysisJob(UUID id, OffsetDateTime baseTime, String inputSnapshot, String steps) {
		this.id = id;
		this.status = AnalysisStatus.PENDING;
		this.baseTime = baseTime;
		this.inputSnapshot = inputSnapshot;
		this.steps = steps;
	}

	public void progress(String steps) {
		this.status = AnalysisStatus.RUNNING;
		this.steps = steps;
	}

	public void recordEventSearch(String eventSearch) {
		this.eventSearch = eventSearch;
	}

	public void recordEvidence(String evidenceSnapshot, String scoreVersion) {
		this.evidenceSnapshot = evidenceSnapshot;
		this.scoreVersion = scoreVersion;
	}

	public void succeed(String steps, String finalResult) {
		this.status = AnalysisStatus.SUCCEEDED;
		this.steps = steps;
		this.finalResult = finalResult;
	}

	public void fail(String steps, String failure) {
		this.status = AnalysisStatus.FAILED;
		this.steps = steps;
		this.failure = failure;
	}
}
