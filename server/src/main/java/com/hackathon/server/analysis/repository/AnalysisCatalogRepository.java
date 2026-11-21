package com.hackathon.server.analysis.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import com.hackathon.server.industry.dto.IndustryResponse;
import com.hackathon.server.ingredient.dto.IngredientResponse;
import com.hackathon.server.supply.dto.SupplyResponse;

/** 분석 입력 확인용 업종·품목 조회와 카드 피드백 저장. supply 엔티티가 없어 SQL로 바로 읽는다. */
@Repository
@RequiredArgsConstructor
public class AnalysisCatalogRepository {

	private final JdbcClient jdbc;

	public Optional<IndustryResponse> industry(long id) {
		return jdbc.sql("select id, category, name from industry where id = ?").param(id).query(IndustryResponse.class).optional();
	}

	public List<IngredientResponse> ingredients(Collection<Long> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		return jdbc.sql("select id, item_name as name from ingredient where id in (:ids)").param("ids", ids)
				.query(IngredientResponse.class).list();
	}

	public List<SupplyResponse> supplies(Collection<Long> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		return jdbc.sql("select id, name from supply where id in (:ids)").param("ids", ids).query(SupplyResponse.class).list();
	}

	/** 가게 좌표에서 1km 안에 있는 가장 가까운 도시데이터 장소 이름 (생활권 표시용). 없으면 빈 값 */
	public Optional<String> nearestAreaName(double latitude, double longitude) {
		return jdbc.sql("""
				select name from city_data_area
				where ST_DWithin(boundary, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, 1000)
				order by ST_Distance(boundary, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography), name
				limit 1
				""").param("lat", latitude).param("lng", longitude).query(String.class).optional();
	}

	/** 분석 1건의 카드 1장에 1표. 다시 보내면 표를 바꾼다. */
	public void upsertFeedback(UUID analysisId, long industryId, String cardKey, String category, String taskType, Long itemId,
			String vote) {
		jdbc.sql("""
				insert into suggestion_feedback (analysis_id, industry_id, card_key, category, task_type, item_id, vote)
				values (:analysisId, :industryId, :cardKey, :category, :taskType, :itemId, :vote)
				on conflict (analysis_id, card_key) do update set vote = excluded.vote
				""")
				.param("analysisId", analysisId)
				.param("industryId", industryId)
				.param("cardKey", cardKey)
				.param("category", category)
				.param("taskType", taskType)
				.param("itemId", itemId)
				.param("vote", vote)
				.update();
	}
}
