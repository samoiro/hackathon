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

	/** 전체 장소와 영역 중심 좌표, 이름 순 */
	public List<AreaResponse> findAll() {
		return jdbc.sql("""
				select code, name, category,
				       ST_Y(ST_Centroid(boundary::geometry)) as latitude,
				       ST_X(ST_Centroid(boundary::geometry)) as longitude
				from city_data_area
				order by name
				""").query(AreaResponse.class).list();
	}
}
