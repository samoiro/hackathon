package com.hackathon.server.industry.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.industry.dto.IndustryResponse;
import com.hackathon.server.industry.dto.RecommendedItemsResponse;
import com.hackathon.server.industry.repository.IndustryIngredientRepository;
import com.hackathon.server.industry.repository.IndustryRepository;
import com.hackathon.server.industry.repository.IndustrySupplyRepository;
import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.supply.dto.SupplyResponse;

@Service
public class IndustryService {

	private final IndustryRepository industryRepository;
	private final IndustryIngredientRepository ingredientRepository;
	private final IndustrySupplyRepository supplyRepository;

	public IndustryService(IndustryRepository industryRepository, IndustryIngredientRepository ingredientRepository,
			IndustrySupplyRepository supplyRepository) {
		this.industryRepository = industryRepository;
		this.ingredientRepository = ingredientRepository;
		this.supplyRepository = supplyRepository;
	}

	@Transactional(readOnly = true)
	public List<IndustryResponse> industries() {
		return industryRepository.findAll(Sort.by("id")).stream()
				.map(i -> new IndustryResponse(i.getId(), i.getCategory(), i.getName()))
				.toList();
	}

	/** 추천 품목이 없는 업종(꽃집 등의 원자재)은 빈 목록, 없는 업종은 404. */
	@Transactional(readOnly = true)
	public RecommendedItemsResponse recommendedItems(long industryId) {
		if (!industryRepository.existsById(industryId)) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "업종을 찾을 수 없습니다 (id %d).".formatted(industryId));
		}
		var ingredients = ingredientRepository.findByIndustryIdOrderBySortOrder(industryId).stream()
				.map(i -> new IngredientResponse(i.getIngredientId(), i.getIngredientName()))
				.toList();
		var supplies = supplyRepository.findByIndustryIdOrderBySortOrder(industryId).stream()
				.map(s -> new SupplyResponse(s.getSupplyId(), s.getSupplyName()))
				.toList();
		return new RecommendedItemsResponse(industryId, ingredients, supplies);
	}
}
