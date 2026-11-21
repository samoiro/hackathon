package com.hackathon.server.ingredient.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.ingredient.repository.IngredientRepository;
import com.hackathon.server.ingredient.dto.IngredientResponse;

/** 원자재 검색을 Flyway 시드(V10)가 들어간 실제 Postgres로 확인한다. */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, IngredientService.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IngredientServiceTest {

	@Autowired IngredientService service;
	@Autowired IngredientRepository repository;

	@Test
	void findsNamesContainingQueryInNameOrder() {
		var names = service.search(" 배추 ").stream().map(IngredientResponse::name).toList();

		assertThat(names).contains("배추", "양배추", "얼갈이배추").allMatch(n -> n.contains("배추")).isSorted();
	}

	@Test
	void missingQueryReturnsAll() {
		assertThat(service.search(null)).hasSize((int) repository.count());
	}

	@Test
	void likeWildcardIsMatchedLiterally() {
		assertThat(service.search("%")).isEmpty();
	}
}
