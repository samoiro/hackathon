package com.hackathon.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hackathon.server.common.entity.ErrorLog;
import com.hackathon.server.common.exception.ErrorCode;
import com.hackathon.server.common.repository.ErrorLogRepository;

/** V28: error_code 테이블은 ErrorCode enum과 같고, error_log는 저장 시각을 DB가 채운다. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ErrorLogMigrationTest {

	@Autowired
	DataSource dataSource;
	@Autowired
	ErrorLogRepository repository;

	@Test
	void errorCodeTableMatchesEnum() {
		var rows = new JdbcTemplate(dataSource).query("select code, http_status, message from error_code",
				(rs, i) -> rs.getString(1) + "|" + rs.getInt(2) + "|" + rs.getString(3));

		assertThat(rows).containsExactlyInAnyOrderElementsOf(Arrays.stream(ErrorCode.values())
				.map(c -> c.name() + "|" + c.getStatus().value() + "|" + c.getMessage()).toList());
	}

	@Test
	void savesErrorLog() {
		Long id = repository.saveAndFlush(new ErrorLog(ErrorCode.EXTERNAL_API_ERROR, 502, "GET", "/api/kakao/region",
				"카카오 API 호출 실패", "org.springframework.web.ErrorResponseException", "trace")).getId();

		var row = new JdbcTemplate(dataSource).queryForMap("select error_code, created_datetime from error_log where id = ?", id);
		assertThat(row.get("error_code")).isEqualTo("EXTERNAL_API_ERROR");
		assertThat(row.get("created_datetime")).isNotNull();
	}
}
