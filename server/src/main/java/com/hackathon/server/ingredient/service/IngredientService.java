package com.hackathon.server.ingredient.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.ingredient.repository.IngredientRepository;

@Service
public class IngredientService {

	private final IngredientRepository repository;

	public IngredientService(IngredientRepository repository) {
		this.repository = repository;
	}

	/** 품목 이름에 query가 들어간 원자재를 전부 이름순으로. query가 없으면 전체. */
	@Transactional(readOnly = true)
	public List<IngredientResponse> search(String query) {
		return repository.findByItemNameContaining(query == null ? "" : query.strip()).stream()
				// DB collation은 한글을 가나다순으로 정렬하지 않아 코드 포인트 순으로 다시 정렬한다
				.sorted(Comparator.comparing(x -> x.getItemName()))
				.map(i -> new IngredientResponse(i.getId(), i.getItemName()))
				.toList();
	}
}
