package com.hackathon.server.batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.batch.entity.BatchJob;

public interface BatchJobRepository extends JpaRepository<BatchJob, Long> {
}
