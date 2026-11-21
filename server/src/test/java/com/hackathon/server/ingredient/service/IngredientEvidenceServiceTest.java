package com.hackathon.server.ingredient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.analysis.dto.IngredientPriceResponse;
import com.hackathon.server.analysis.dto.PriceChange;
import com.hackathon.server.analysis.dto.Trend;
import com.hackathon.server.analysis.evidence.IngredientEvidence;
import com.hackathon.server.analysis.evidence.IngredientEvidenceResult;
import com.hackathon.server.analysis.evidence.IngredientSignal;
import com.hackathon.server.analysis.evidence.StoreContext;
import com.hackathon.server.ingredient.entity.IngredientPriceHistory;
import com.hackathon.server.ingredient.entity.PriceStage;
import com.hackathon.server.ingredient.repository.IngredientPriceHistoryRepository;
import com.hackathon.server.supply.service.SupplyEvidenceService;

/** 마이그레이션이 넣은 원자재·부류 경계·규칙·김장 날짜 위에서 가격 계산과 이번 주 규칙을 실제 Postgres로 확인한다. */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, IngredientEvidenceService.class, SupplyEvidenceService.class })   // 부자재 테스트와 같은 context·컨테이너를 쓴다
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IngredientEvidenceServiceTest {

	// 김장(2026-11-21 시작) 21~14일 전 안
	private static final LocalDate BASE = LocalDate.of(2026, 11, 1);
	private static final LocalDate SURVEYED = LocalDate.of(2026, 10, 30);

	@Autowired
	IngredientEvidenceService service;
	@Autowired
	IngredientPriceHistoryRepository priceHistoryRepository;
	@Autowired
	DataSource dataSource;

	private long id(String name) {
		return new JdbcTemplate(dataSource).queryForObject("select id from ingredient where item_name = ?", Long.class, name);
	}

	private void price(String name, LocalDate date, String price, String w1, String w2, String w3, String w4) {
		BigDecimal[] values = Arrays.stream(new String[] { price, w1, w2, w3, w4 }).map(v -> v == null ? null : new BigDecimal(v))
				.toArray(BigDecimal[]::new);
		priceHistoryRepository.saveAndFlush(new IngredientPriceHistory(id(name), date, values[0], values[1], values[2], values[3], values[4]));
	}

	private IngredientEvidenceResult collect(LocalDate baseDate, List<Long> ingredientIds) {
		return service.collect(new StoreContext(null, OffsetDateTime.parse("2026-11-01T09:00:00+09:00"), baseDate, 37.54, 127.05,
				"성동구", "1120011400", 3, "음식점", "한식·국밥", null, ingredientIds, List.of()));
	}

	private static List<String> ruleCodes(IngredientEvidence item) {
		return item.signals().stream().map(IngredientSignal::ruleCode).toList();
	}

	@Test
	void calculatesChangesTrendStreakAndStageFromLatestSurvey() {
		price("배추", SURVEYED, "4380", "4207", "4100", "4000", "3500");   // 4주 +25.1%: 채소류 상위 12.5%(+21.19%) 이상
		price("돼지", SURVEYED, "1534", "1556", "1601", "1532", "1674");   // 4주 −8.4%: 축산물 하위 5%(−8.20%) 이하
		price("쌀", LocalDate.of(2026, 10, 20), "30000", "30000", null, null, "30000");
		price("쌀", SURVEYED, "35282", "35500", null, null, null);          // 1주 −0.6%, 4주 전 가격 없음
		price("쌀", BASE.plusDays(1), "99999", null, null, null, null);    // 기준일 뒤 조사는 안 본다

		IngredientEvidenceResult result = collect(BASE, List.of(id("배추"), id("돼지"), id("쌀"), id("파"), 999_999L));

		assertThat(result.items()).extracting(item -> item.price().evidenceId()).containsExactly("R1", "R2", "R3", "R4");
		IngredientPriceResponse cabbage = result.items().get(0).price();
		assertThat(cabbage.baseDate()).isEqualTo(SURVEYED);
		assertThat(cabbage.price()).isEqualTo(4380L);
		assertThat(cabbage.weeklyPrices()).containsExactly(3500L, 4000L, 4100L, 4207L, 4380L);
		assertThat(cabbage.change1w()).isEqualTo(new PriceChange(173, 4.1));
		assertThat(cabbage.change4w()).isEqualTo(new PriceChange(880, 25.1));
		assertThat(cabbage.trend()).isEqualTo(Trend.UP);
		assertThat(cabbage.consecutiveWeeks()).isEqualTo(4);
		assertThat(cabbage.priceStage()).isEqualTo(PriceStage.ALERT_HIGH);
		assertThat(cabbage.unit()).isEqualTo("1포기");

		IngredientPriceResponse pork = result.items().get(1).price();
		assertThat(pork.trend()).isEqualTo(Trend.DOWN);
		assertThat(pork.consecutiveWeeks()).isEqualTo(2);   // 1532 → 1601에서 올라 끊긴다
		assertThat(pork.priceStage()).isEqualTo(PriceStage.SEVERE_LOW);
		assertThat(pork.change4w()).isEqualTo(new PriceChange(-140, -8.4));
		// 화면 변동률이 −1.0%(반올림 전 −0.95%)면 FLAT이 아니라 DOWN
		price("사과", SURVEYED, "35282", "35622", null, null, null);
		assertThat(collect(BASE, List.of(id("사과"))).items().getFirst().price().trend()).isEqualTo(Trend.DOWN);

		IngredientPriceResponse rice = result.items().get(2).price();
		assertThat(rice.price()).isEqualTo(35282L);
		assertThat(rice.weeklyPrices()).containsExactly(null, null, null, 35500L, 35282L);
		assertThat(rice.trend()).isEqualTo(Trend.FLAT);
		assertThat(rice.consecutiveWeeks()).isZero();
		assertThat(rice.change4w()).isNull();
		assertThat(rice.priceStage()).isNull();

		// 가격 자료가 없어도 넣고, 가격 필드는 모두 null
		IngredientPriceResponse greenOnion = result.items().get(3).price();
		assertThat(greenOnion.name()).isEqualTo("파");
		assertThat(Arrays.asList(greenOnion.baseDate(), greenOnion.price(), greenOnion.weeklyPrices(), greenOnion.change1w(),
				greenOnion.change4w(), greenOnion.trend(), greenOnion.consecutiveWeeks(), greenOnion.priceStage())).containsOnlyNulls();

		assertThat(result.sources()).singleElement().satisfies(source -> {
			assertThat(source.provider()).isEqualTo("한국농수산식품유통공사");
			assertThat(source.baseDate()).isEqualTo(SURVEYED);
		});
	}

	@Test
	void signalsOnlyRulesThatHitThisWeek() {
		price("배추", SURVEYED, "4380", "4207", "4100", "4000", "3500");
		price("돼지", SURVEYED, "1534", "1556", "1601", "1532", "1674");

		IngredientEvidenceResult result = collect(BASE, List.of(id("배추"), id("돼지"), id("쌀")));

		// 배추: 가격 단계 + 김장 전. 제철(7~10월)은 11월에 안 걸린다
		assertThat(result.items().get(0).signals()).extracting(IngredientSignal::ruleCode, IngredientSignal::demand).containsExactly(
				tuple("PRICE_RISE_FOUR_WEEKS_ALERT", "STRONG_DOWN"), tuple("BEFORE_KIMJANG", "UP"));
		// 돼지: 연결된 다른 가격 단계 규칙(마이그레이션이 6개 모두 연결)은 빼고 계산한 단계 하나만
		assertThat(ruleCodes(result.items().get(1))).containsExactly("PRICE_DROP_FOUR_WEEKS_SEVERE");
		assertThat(ruleCodes(result.items().get(2))).isEmpty();
	}

	@Test
	void seasonRulesFollowBaseMonth() {
		long cabbage = id("배추");   // 7~10월 제철

		assertThat(ruleCodes(collect(LocalDate.of(2026, 6, 15), List.of(cabbage)).items().get(0)))
				.containsExactly("BEFORE_SEASON_START_MONTH_07");
		assertThat(ruleCodes(collect(LocalDate.of(2026, 8, 10), List.of(cabbage)).items().get(0))).containsExactly("IN_SEASON_MONTH_08");
		// 김장 시작 14일 전(11-07)까지가 김장 전이고 그 뒤는 아니다
		assertThat(ruleCodes(collect(LocalDate.of(2026, 11, 7), List.of(cabbage)).items().get(0))).containsExactly("BEFORE_KIMJANG");
		assertThat(ruleCodes(collect(LocalDate.of(2026, 11, 8), List.of(cabbage)).items().get(0))).isEmpty();
	}

	@Test
	void noIngredientsGivesNoSource() {
		assertThat(collect(BASE, List.of())).isEqualTo(new IngredientEvidenceResult(List.of(), List.of()));
	}
}
