package com.hackathon.server.ingredient.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.ingredient.entity.IngredientRule;

public interface IngredientRuleRepository extends JpaRepository<IngredientRule, Long> {

	List<IngredientRule> findByTriggerCodeIn(Collection<String> triggerCodes);
}
