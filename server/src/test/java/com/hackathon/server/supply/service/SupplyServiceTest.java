package com.hackathon.server.supply.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.supply.repository.SupplyRepository;
import com.hackathon.server.supply.dto.SupplyResponse;

/** 부자재 검색을 Flyway 시드(V2)가 들어간 실제 Postgres로 확인한다. */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, SupplyService.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SupplyServiceTest {

	@Autowired SupplyService service;
	@Autowired SupplyRepository repository;

	@Test
	void findsNamesContainingQueryInNameOrder() {
		var names = service.search("컵").stream().map(SupplyResponse::name).toList();

		assertThat(names).contains("테이크아웃 컵", "소스컵").allMatch(n -> n.contains("컵")).isSorted();
	}

	@Test
	void missingQueryReturnsAll() {
		assertThat(service.search(null)).hasSize((int) repository.count());
	}
}
