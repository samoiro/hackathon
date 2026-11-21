package com.hackathon.server.area.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.hackathon.server.TestcontainersConfiguration;
import com.hackathon.server.area.dto.AreaResponse;

/** geography 영역의 중심 좌표를 위도·경도 순서로 읽고, 가게 좌표 기준 거리순 정렬이 맞는지 실제 PostGIS로 확인한다. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AreaRepositoryTest {

	@Autowired
	DataSource dataSource;

	@Test
	void listsAreasByNameWithCentroid() {
		var areas = insertAreas().findAll(null, null).stream().filter(a -> a.code().startsWith("T")).toList();

		assertThat(areas).extracting(AreaResponse::name).containsExactly("가나다길", "테스트숲");
		assertThat(areas.get(1).latitude()).isCloseTo(37.545, within(1e-6));
		assertThat(areas.get(1).longitude()).isCloseTo(127.045, within(1e-6));
		assertThat(areas.get(1).boundary()).startsWith("{\"type\":\"MultiPolygon\"").contains("[127.04,37.54]");
	}

	@Test
	void listsNearestAreaFirstWhenStoreCoordinateGiven() {
		// 성수역 근처 가게: 이름순이면 뒤인 테스트숲이 더 가깝다
		var areas = insertAreas().findAll(37.5446, 127.0559).stream().filter(a -> a.code().startsWith("T")).toList();

		assertThat(areas).extracting(AreaResponse::name).containsExactly("테스트숲", "가나다길");
	}

	private AreaRepository insertAreas() {
		JdbcClient jdbc = JdbcClient.create(dataSource);
		jdbc.sql("""
				insert into city_data_area (code, name, category, boundary) values
				('T2', '테스트숲', '공원', ST_GeogFromText('MULTIPOLYGON(((127.04 37.54, 127.05 37.54, 127.05 37.55, 127.04 37.55, 127.04 37.54)))')),
				('T1', '가나다길', '골목', ST_GeogFromText('MULTIPOLYGON(((126.97 37.57, 126.98 37.57, 126.98 37.58, 126.97 37.58, 126.97 37.57)))'))
				""").update();
		return new AreaRepository(jdbc);
	}
}
