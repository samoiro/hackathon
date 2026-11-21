package com.hackathon.server.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.ingredient.entity.IngredientPriceBand;

public interface IngredientPriceBandRepository extends JpaRepository<IngredientPriceBand, String> {
}
