package com.hackathon.server.ingredient.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.ingredient.service.IngredientService;

@Tag(name = "Ingredient", description = "원자재")
@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

	private final IngredientService service;

	public IngredientController(IngredientService service) {
		this.service = service;
	}

	@Operation(summary = "원자재 검색", description = "이름에 query가 들어간 항목 전부를 이름순으로. query가 없으면 전체")
	@GetMapping
	public List<IngredientResponse> search(@RequestParam(required = false) String query) {
		return service.search(query);
	}
}
