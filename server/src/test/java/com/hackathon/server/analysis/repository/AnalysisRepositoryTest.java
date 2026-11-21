package com.hackathon.server.analysis.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.analysis.entity.AnalysisJob;
import com.hackathon.server.ingredient.dto.IngredientResponse;

/** 분석 작업의 jsonb 칸 저장, 입력 확인용 조회, 피드백 1표 upsert를 실제 Postgres로 확인한다. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnalysisRepositoryTest {

	private static final UUID ID = UUID.fromString("6f1c2a4e-0d0b-4e7b-9a51-2b1f0c3d9e10");

	@Autowired
	AnalysisJobRepository jobRepository;
	@Autowired
	TestEntityManager em;
	@Autowired
	DataSource dataSource;

	private JdbcClient jdbc;
	private AnalysisCatalogRepository catalog;

	@BeforeEach
	void setUp() {
		jdbc = JdbcClient.create(dataSource);
		catalog = new AnalysisCatalogRepository(jdbc);
	}

	@Test
	void savesJsonColumnsAsJsonb() {
		var job = new AnalysisJob(ID, OffsetDateTime.parse("2026-11-17T07:00:00+09:00"), "{\"store\":{\"gu\":\"성동구\"}}",
				"[{\"code\":\"STORE_CHECK\",\"status\":\"WAITING\"}]");
		jobRepository.save(job);
		em.flush();
		job.recordEventSearch("{\"days\":7}");
		job.recordEvidence("[{\"evidenceId\":\"E1\",\"total\":6}]", "2026-11-21");
		job.succeed("[]", "{\"suggestions\":[]}");
		jobRepository.save(job);
		em.flush();
		em.clear();

		var row = jdbc.sql("""
				select status, jsonb_typeof(input_snapshot) as input, input_snapshot ->> 'store' as store,
				       event_search ->> 'days' as days, evidence_snapshot -> 0 ->> 'evidenceId' as evidence
				from briefing_analysis_job where id = ?""").param(ID).query().singleRow();
		assertThat(row).containsEntry("status", "SUCCEEDED").containsEntry("input", "object").containsEntry("days", "7")
				.containsEntry("evidence", "E1");
		assertThat(jobRepository.findById(ID)).get().extracting(AnalysisJob::getFinalResult).asString().contains("suggestions");
	}

	@Test
	void findsCatalogItemsById() {
		long cabbage = jdbc.sql("select id from ingredient where item_name = '배추'").query(Long.class).single();
		long container = jdbc.sql("select id from supply where name = '포장 용기'").query(Long.class).single();
		long industry = jdbc.sql("select id from industry where name = '한식·국밥'").query(Long.class).single();

		assertThat(catalog.ingredients(List.of(cabbage, -1L))).containsExactly(new IngredientResponse(cabbage, "배추"));
		assertThat(catalog.supplies(List.of(container))).extracting(s -> s.name()).containsExactly("포장 용기");
		assertThat(catalog.industry(industry)).get().extracting(i -> i.category()).isEqualTo("음식점");
		assertThat(catalog.industry(-1)).isEmpty();
	}

	@Test
	void nearestAreaWithinOneKilometer() {
		// 가게(127.0446, 37.5446)에서 동쪽 약 400m, 3km 떨어진 두 장소
		jdbc.sql("""
				insert into city_data_area (code, name, category, boundary) values
				('T1', '테스트숲', '공원', ST_GeogFromText('MULTIPOLYGON(((127.049 37.544, 127.052 37.544, 127.052 37.546, 127.049 37.546, 127.049 37.544)))')),
				('T2', '먼곳', '공원', ST_GeogFromText('MULTIPOLYGON(((127.078 37.544, 127.080 37.544, 127.080 37.546, 127.078 37.546, 127.078 37.544)))'))
				""").update();

		assertThat(catalog.nearestAreaName(37.5446, 127.0446)).contains("테스트숲");
		assertThat(catalog.nearestAreaName(37.5446, 127.0790)).contains("먼곳");
		assertThat(catalog.nearestAreaName(37.60, 126.90)).isEmpty();
	}

	@Test
	void feedbackUpsertKeepsOneVotePerCard() {
		jobRepository.saveAndFlush(new AnalysisJob(ID, OffsetDateTime.parse("2026-11-17T07:00:00+09:00"), "{}", "[]"));
		long industry = jdbc.sql("select id from industry where name = '한식·국밥'").query(Long.class).single();

		catalog.upsertFeedback(ID, industry, "INGREDIENT:PRICE_CHECK:12", "INGREDIENT", "PRICE_CHECK", 12L, "HELPFUL");
		catalog.upsertFeedback(ID, industry, "INGREDIENT:PRICE_CHECK:12", "INGREDIENT", "PRICE_CHECK", 12L, "NOT_RELEVANT");
		catalog.upsertFeedback(ID, industry, "EVENT:STAFFING:-", "EVENT", "STAFFING", null, "HELPFUL");

		assertThat(jdbc.sql("select card_key || '=' || vote from suggestion_feedback where analysis_id = ? order by card_key").param(ID)
				.query(String.class).list())
				.containsExactly("EVENT:STAFFING:-=HELPFUL", "INGREDIENT:PRICE_CHECK:12=NOT_RELEVANT");
	}
}
