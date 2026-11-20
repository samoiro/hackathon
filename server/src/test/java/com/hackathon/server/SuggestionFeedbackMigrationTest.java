package com.hackathon.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** V25: 추천 카드 피드백은 분석 1건의 카드 1장에 1표이고, 카드 키와 칸 값이 같아야 한다. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SuggestionFeedbackMigrationTest {

	private static final UUID ANALYSIS_ID = UUID.fromString("6f1c2a4e-0d0b-4e7b-9a51-2b1f0c3d9e10");
	private static final String INSERT = "insert into suggestion_feedback (analysis_id, industry_id, card_key, category, task_type, item_id, vote)"
			+ " values (?, (select id from industry where name = '한식·국밥'), ?, ?, ?, ?, ?)";

	@Autowired
	DataSource dataSource;

	private JdbcTemplate jdbc;

	@BeforeEach
	void createAnalysis() {
		jdbc = new JdbcTemplate(dataSource);
		jdbc.update("insert into briefing_analysis_job (id, base_time, input_snapshot) values (?, '2026-11-17 07:00+09', '{}')",
				ANALYSIS_ID);
	}

	// 제약을 어기면 Postgres가 테스트 트랜잭션을 끊으므로, 실패하는 insert는 각 테스트의 마지막에 둔다.
	@Test
	void oneVotePerCardInAnalysis() {
		assertThat(jdbc.update(INSERT, ANALYSIS_ID, "INGREDIENT:PRICE_CHECK:12", "INGREDIENT", "PRICE_CHECK", 12L, "HELPFUL"))
				.isEqualTo(1);

		assertThatThrownBy(() -> jdbc.update(INSERT, ANALYSIS_ID, "INGREDIENT:PRICE_CHECK:12", "INGREDIENT", "PRICE_CHECK", 12L,
				"NOT_RELEVANT")).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void cardKeyMustMatchItsColumns() {
		assertThat(jdbc.update(INSERT, ANALYSIS_ID, "EVENT:STAFFING:-", "EVENT", "STAFFING", null, "HELPFUL")).isEqualTo(1);

		assertThatThrownBy(() -> jdbc.update(INSERT, ANALYSIS_ID, "EVENT:HOURS:-", "EVENT", "PACKING", null, "HELPFUL"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
