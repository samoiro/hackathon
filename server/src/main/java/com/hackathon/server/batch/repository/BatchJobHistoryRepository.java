package com.hackathon.server.batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.batch.entity.BatchJobHistory;

public interface BatchJobHistoryRepository extends JpaRepository<BatchJobHistory, Long> {
}
