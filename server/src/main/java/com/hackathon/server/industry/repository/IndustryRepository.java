package com.hackathon.server.industry.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.industry.entity.Industry;

public interface IndustryRepository extends JpaRepository<Industry, Long> {
}
