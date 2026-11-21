package com.hackathon.server.industry.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.industry.entity.IndustrySupply;

public interface IndustrySupplyRepository extends JpaRepository<IndustrySupply, IndustrySupply.Key> {

	List<IndustrySupply> findByIndustryIdOrderBySortOrder(Long industryId);
}
