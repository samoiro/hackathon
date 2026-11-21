package com.hackathon.server.industry.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.industry.entity.IndustryIngredient;

public interface IndustryIngredientRepository extends JpaRepository<IndustryIngredient, IndustryIngredient.Key> {

	List<IndustryIngredient> findByIndustryIdOrderBySortOrder(Long industryId);
}
