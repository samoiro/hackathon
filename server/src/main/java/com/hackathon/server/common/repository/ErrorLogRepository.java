package com.hackathon.server.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.common.entity.ErrorLog;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
}
