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

/** V26: 원자재 가격 규칙을 수급조절매뉴얼 방식(4주 변동률 분위수 단계)으로 바꾼다. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PriceBandMigrationTest {

	@Autowired
	DataSource dataSource;

	private JdbcTemplate jdbc() {
		return new JdbcTemplate(dataSource);
	}

	private int count(String sql, Object... args) {
		return jdbc().queryForObject(sql, Integer.class, args);
	}

	@Test
	void fixedPercentPriceRulesAreReplacedByStageRules() {
		assertThat(count("select count(*) from ingredient_rule where trigger_code like 'PRICE_%PERCENT'"
				+ " or trigger_code = 'PRICE_RISE_ONE_WEEK'")).isZero();
		assertThat(count("select count(*) from ingredient_rule where trigger_code like 'PRICE_%'")).isEqualTo(6);
		assertThat(count("select count(*) from ingredient_rule where trigger_code like 'PRICE_%' and demand = 'STRONG_DOWN'"))
				.isEqualTo(2);
		// 가격 규칙은 전체 원자재에 연결한다
		assertThat(count("select count(*) from ingredient_rule_item ri join ingredient_rule r on r.id = ri.rule_id"
				+ " where r.trigger_code like 'PRICE_%'")).isEqualTo(6 * count("select count(*) from ingredient"));
	}

	@Test
	void everyIngredientCategoryHasBand() {
		assertThat(count("select count(distinct i.category_name) from ingredient i"
				+ " left join ingredient_price_band b on b.category_name = i.category_name where b.category_name is null")).isZero();
	}

	@Test
	void bandsMustBeOrdered() {
		assertThatThrownBy(() -> jdbc().update("insert into ingredient_price_band"
				+ " (category_name, sample_count, p5, p12_5, p20, p80, p87_5, p95, sample_period)"
				+ " values ('테스트', 10, 0.1, 0, 0, 0, 0, 0, '테스트')"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
