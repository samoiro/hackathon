package com.hackathon.server.analysis.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.analysis.entity.AnalysisJob;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {
}
