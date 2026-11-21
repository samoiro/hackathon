package com.hackathon.server.batch.entity;

/** batch_job·batch_job_history.status 값. 작업은 PENDING으로 만들 수 있지만 이 서버는 바로 RUNNING으로 만든다. */
public enum BatchStatus {
	PENDING, RUNNING, SUCCEEDED, FAILED
}
