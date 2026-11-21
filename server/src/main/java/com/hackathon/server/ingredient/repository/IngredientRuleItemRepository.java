package com.hackathon.server.ingredient.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.hackathon.server.ingredient.entity.IngredientRuleItem;

public interface IngredientRuleItemRepository extends JpaRepository<IngredientRuleItem, IngredientRuleItem.Key> {

	@Modifying
	@Query("delete from IngredientRuleItem i where i.ruleId in :ruleIds")
	int deleteByRuleIds(Collection<Long> ruleIds);

	List<IngredientRuleItem> findByIngredientIdIn(Collection<Long> ingredientIds);
}
