package com.hackathon.server.batch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.batch.entity.BatchJob;
import com.hackathon.server.batch.entity.BatchStatus;

public interface BatchJobRepository extends JpaRepository<BatchJob, Long> {

	List<BatchJob> findByStatusAndRetryTimesLessThanOrderById(BatchStatus status, int retryTimes);
}
