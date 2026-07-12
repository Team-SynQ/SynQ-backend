package com.synq.backend;

import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;

// @SpringBootTest 는 PostgresTestContainer 가 갖고 있다.
// 로컬 DB(localhost:5432) 대신 일회용 컨테이너를 쓰므로 docker compose 없이도 돈다.
class BackendApplicationTests extends PostgresTestContainer {

	@Test
	void contextLoads() {
	}

}
