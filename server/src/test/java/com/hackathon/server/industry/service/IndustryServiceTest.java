package com.hackathon.server.industry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.common.exception.BusinessException;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.industry.repository.IndustryRepository;
import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.supply.dto.SupplyResponse;

/** 업종 추천 품목을 Flyway 시드(V21)가 들어간 실제 Postgres로 확인한다. */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, IndustryService.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IndustryServiceTest {

	@Autowired IndustryService service;
	@Autowired IndustryRepository industryRepository;

	private long idOf(String name) {
		return industryRepository.findAll().stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow().getId();
	}

	@Test
	void returnsRecommendationsInSortOrder() {
		var response = service.recommendedItems(idOf("카페"));

		assertThat(response.ingredients()).extracting(IngredientResponse::name).containsExactly("우유", "딸기", "바나나");
		assertThat(response.supplies()).extracting(SupplyResponse::name).containsExactly("테이크아웃 컵", "빨대");
	}

	@Test
	void industryWithoutIngredientsReturnsEmptyList() {
		var response = service.recommendedItems(idOf("꽃집"));

		assertThat(response.ingredients()).isEmpty();
		assertThat(response.supplies()).extracting(SupplyResponse::name).containsExactly("꽃 포장지", "리본");
	}

	@Test
	void unknownIndustryIsNotFound() {
		var error = assertThrows(BusinessException.class, () -> service.recommendedItems(999_999L));

		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
	}
}
