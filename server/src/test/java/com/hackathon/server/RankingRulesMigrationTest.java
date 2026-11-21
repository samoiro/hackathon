package com.hackathon.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** V24: 강추 순위용 규칙 정리와 분석 작업 테이블. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RankingRulesMigrationTest {

	@Autowired
	DataSource dataSource;

	private JdbcTemplate jdbc() {
		return new JdbcTemplate(dataSource);
	}

	private int count(String sql, Object... args) {
		return jdbc().queryForObject(sql, Integer.class, args);
	}

	@Test
	void supplyRulesDropOrderAndTriggersWithoutData() {
		assertThat(count("select count(*) from information_schema.columns where column_name = 'sort_order'"
				+ " and table_name in ('supply_rule_item', 'ingredient_rule_item')")).isZero();
		assertThat(count("select count(*) from supply_trigger where code in ('CROWD_SURGE', 'SPORTS', 'TRAFFIC_CONTROL')"))
				.isZero();
		assertThat(count("select count(*) from supply_rule_item ri join supply_rule r on r.id = ri.rule_id"
				+ " join industry i on i.id = r.industry_id where i.name = '수선집' and r.trigger_code = 'GRADUATION'"))
				.isEqualTo(2);
	}

	@Test
	void ingredientRulesSwapFourWeekRuleForBigRiseAndHolidays() {
		assertThat(count("select count(*) from ingredient_rule where trigger_code = 'PRICE_BELOW_FOUR_WEEKS'")).isZero();
		// 10% 이상 상승 규칙은 V26에서 분위수 단계 규칙으로 바뀌었다 (PriceBandMigrationTest)
		assertThat(count("select count(*) from ingredient_rule_item ri join ingredient_rule r on r.id = ri.rule_id"
				+ " where r.trigger_code = 'BEFORE_KIMJANG'")).isEqualTo(11);
	}

	@Test
	void analysisJobNeedsResultWhenSucceeded() {
		assertThatThrownBy(() -> jdbc().update("insert into briefing_analysis_job (id, status, base_time, input_snapshot)"
				+ " values (gen_random_uuid(), 'SUCCEEDED', now(), '{}')"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
