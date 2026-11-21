package com.hackathon.server.ingredient.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.ingredient.dto.IngredientResponse;

@Tag(name = "Ingredient", description = "원자재")
@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

	@Operation(summary = "원자재 검색", description = "최대 20개")
	@GetMapping
	public List<IngredientResponse> search(@RequestParam(required = false) String query) {
		throw new UnsupportedOperationException("TODO: 원자재 검색");
	}
}
