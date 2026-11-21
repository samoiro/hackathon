package com.hackathon.server.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.server.ingredient.entity.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
