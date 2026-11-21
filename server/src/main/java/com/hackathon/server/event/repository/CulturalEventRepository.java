package com.hackathon.server.event.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.hackathon.server.event.dto.CulturalEventRow;
import com.hackathon.server.event.dto.NearbyEvent;

/** cultural_event(V15) 조회·적재. coord가 PostGIS geography라 JPA 엔티티 대신 SQL로 다룬다. */
@Repository
public class CulturalEventRepository {

	private static final String POINT = "ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography";

	private final JdbcClient jdbc;
	private final NamedParameterJdbcTemplate batchJdbc;

	public CulturalEventRepository(JdbcClient jdbc, NamedParameterJdbcTemplate batchJdbc) {
		this.jdbc = jdbc;
		this.batchJdbc = batchJdbc;
	}

	/**
	 * 활성 행사 중 분류가 categories이고, 기준일에 진행 중이거나 days일 안에 시작하며, 반경 radiusM 미터 안인 것.
	 * 15일 이상 이어지는 행사는 시작 후 첫 7일만 넣는다(몇 주씩 이어지는 행사는 몰리는 날이 없어서).
	 */
	public List<NearbyEvent> findNearby(double latitude, double longitude, int radiusM, LocalDate baseDate, int days,
			Collection<String> categories) {
		return jdbc.sql("""
				select title, category, place, start_date, end_date, time_text, is_free, source_url,
				       ST_Y(coord::geometry) as latitude, ST_X(coord::geometry) as longitude,
				       ST_Distance(coord, %1$s) as distance_m
				from cultural_event
				where is_active
				  and category in (:categories)
				  and end_date >= :baseDate
				  and start_date <= cast(:baseDate as date) + :days
				  and (end_date - start_date < 14 or cast(:baseDate as date) <= start_date + 6)
				  and ST_DWithin(coord, %1$s, :radiusM)
				order by distance_m, start_date, id""".formatted(POINT))
				.param("latitude", latitude).param("longitude", longitude).param("radiusM", radiusM)
				.param("baseDate", baseDate).param("days", days).param("categories", categories)
				.query(NearbyEvent.class).list();
	}

	/** source_key로 upsert하고 다시 받은 행사는 활성화한다. 내용이 같으면 V15 트리거가 updated_datetime을 유지한다. */
	public int[] upsertAll(List<CulturalEventRow> rows) {
		return batchJdbc.batchUpdate("""
				insert into cultural_event (source_key, title, category, district_name, place, start_date, end_date, time_text,
				    coord, target_audience, program, notes, fee_text, is_free, source_url, image_url, is_active, raw_payload)
				values (:sourceKey, :title, :category, :districtName, :place, :startDate, :endDate, :timeText,
				    %s, :targetAudience, :program, :notes, :feeText, :isFree, :sourceUrl, :imageUrl, true, cast(:rawPayload as jsonb))
				on conflict (source_key) do update set
				    title = excluded.title, category = excluded.category, district_name = excluded.district_name,
				    place = excluded.place, start_date = excluded.start_date, end_date = excluded.end_date,
				    time_text = excluded.time_text, coord = excluded.coord, target_audience = excluded.target_audience,
				    program = excluded.program, notes = excluded.notes, fee_text = excluded.fee_text, is_free = excluded.is_free,
				    source_url = excluded.source_url, image_url = excluded.image_url, is_active = true,
				    raw_payload = excluded.raw_payload""".formatted(POINT),
				rows.stream().map(SimplePropertySqlParameterSource::new).toArray(SqlParameterSource[]::new));
	}

	/** 이번 전체 수집에 없거나 제외된 행사를 지우지 않고 비활성화한다. source_key는 숫자뿐이라 쉼표로 이어 넘긴다. */
	public int deactivateExcept(Collection<String> activeKeys) {
		return jdbc.sql("""
				update cultural_event set is_active = false
				where is_active and source_key <> all(string_to_array(:keys, ','))""")
				.param("keys", String.join(",", activeKeys))
				.update();
	}
}
