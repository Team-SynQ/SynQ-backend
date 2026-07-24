package com.synq.backend;

import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PgvectorExtensionTest extends PostgresTestContainer {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void pgvector_확장이_설치된다() {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);

		assertThat(count).isEqualTo(1);
	}

	@Test
	void 코사인_거리_연산자를_쓸_수_있다() {
		Double distance = jdbcTemplate.queryForObject(
				"SELECT '[1,0,0]'::vector <=> '[0,1,0]'::vector", Double.class);

		// 직교하는 두 벡터의 코사인 거리는 1
		assertThat(distance).isEqualTo(1.0);
	}

	@Test
	void 커넥션마다_iterative_scan_이_켜진다() {
		// 꺼져 있으면 project_id 로 거른 결과가 topK 보다 적게 나올 수 있다.
		// 효과는 데이터가 쌓여야 드러나므로, 설정이 걸렸는지만 확인한다.
		String mode = jdbcTemplate.queryForObject("SHOW hnsw.iterative_scan", String.class);

		assertThat(mode).isEqualTo("strict_order");
	}
}
