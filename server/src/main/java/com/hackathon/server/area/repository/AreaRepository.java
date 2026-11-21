package com.hackathon.server.area.repository;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import com.hackathon.server.area.dto.AreaResponse;

/** city_data_area(V7) 조회. geography 칸이라 엔티티 없이 SQL로 읽는다. */
@Repository
@RequiredArgsConstructor
public class AreaRepository {

	private final JdbcClient jdbc;

	/** 전체 장소와 영역 중심 좌표·경계 GeoJSON(소수 6자리 ≈ 0.1m). 좌표를 주면 영역까지 가까운 순(영역 안이면 0), 같거나 좌표가 없으면 이름 순 */
	public List<AreaResponse> findAll(Double latitude, Double longitude) {
		return jdbc.sql("""
				select code, name, category,
				       ST_Y(ST_Centroid(boundary::geometry)) as latitude,
				       ST_X(ST_Centroid(boundary::geometry)) as longitude,
				       ST_AsGeoJSON(boundary, 6) as boundary
				from city_data_area
				order by ST_Distance(boundary,
				    ST_SetSRID(ST_MakePoint(cast(:longitude as float8), cast(:latitude as float8)), 4326)::geography) nulls last,
				    name
				""").param("latitude", latitude).param("longitude", longitude).query(AreaResponse.class).list();
	}
}
