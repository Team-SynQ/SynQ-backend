package com.synq.backend.domain.ai.client.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.synq.backend.domain.ai.client.EmbeddingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiEmbeddingClientTest {

	private static final String BASE_URL = "https://gemini.test/v1beta";
	private static final String URL = BASE_URL + "/models/gemini-embedding-001:batchEmbedContents";

	private MockRestServiceServer server;
	private GeminiEmbeddingClient client;

	/** 배치 크기 2, 재시도 3회. 백오프는 테스트가 느려지지 않도록 1ms. */
	private static GeminiProperties properties() {
		return new GeminiProperties(
				"test-key",
				BASE_URL,
				new GeminiProperties.Embedding("gemini-embedding-001", 768, 2, 3, 1L, 2L));
	}

	/** 768차원 벡터 count 개를 담은 응답. 각 벡터는 첫 원소만 3.0, 나머지 0. */
	private static String responseWith(int count) {
		String vector = IntStream.range(0, 768)
				.mapToObj(i -> i == 0 ? "3.0" : "0.0")
				.collect(Collectors.joining(","));
		String embeddings = IntStream.range(0, count)
				.mapToObj(i -> "{\"values\":[" + vector + "]}")
				.collect(Collectors.joining(","));
		return "{\"embeddings\":[" + embeddings + "]}";
	}

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new GeminiEmbeddingClient(builder, properties());
	}

	@Test
	void 요청_형식과_API_키_헤더를_보낸다() {
		server.expect(requestTo(URL))
				.andExpect(method(POST))
				.andExpect(header("x-goog-api-key", "test-key"))
				.andExpect(jsonPath("$.requests[0].model").value("models/gemini-embedding-001"))
				.andExpect(jsonPath("$.requests[0].taskType").value("RETRIEVAL_DOCUMENT"))
				.andExpect(jsonPath("$.requests[0].outputDimensionality").value(768))
				.andExpect(jsonPath("$.requests[0].content.parts[0].text").value("첫 청크"))
				.andRespond(withSuccess(responseWith(1), MediaType.APPLICATION_JSON));

		List<float[]> vectors = client.embedDocuments(List.of("첫 청크"));

		assertThat(vectors).hasSize(1);
		assertThat(vectors.get(0)).hasSize(768);
		server.verify();
	}

	@Test
	void 응답_벡터를_L2_정규화한다() {
		// gemini-embedding-001 은 768 같은 축소 차원에서 정규화된 벡터를 주지 않는다.
		// 첫 원소 3.0 짜리 벡터를 정규화하면 1.0 이 되어야 한다.
		server.expect(requestTo(URL))
				.andRespond(withSuccess(responseWith(1), MediaType.APPLICATION_JSON));

		float[] vector = client.embedDocuments(List.of("텍스트")).get(0);

		assertThat(vector[0]).isCloseTo(1.0f, Offset.offset(0.0001f));

		double norm = 0;
		for (float v : vector) {
			norm += v * v;
		}
		assertThat(Math.sqrt(norm)).isCloseTo(1.0, Offset.offset(0.0001));
	}

	@Test
	void 배치_크기를_넘으면_나눠서_호출한다() {
		// 배치 크기 2, 텍스트 5개 → 2 + 2 + 1 = 3번 호출
		server.expect(requestTo(URL))
				.andRespond(withSuccess(responseWith(2), MediaType.APPLICATION_JSON));
		server.expect(requestTo(URL))
				.andRespond(withSuccess(responseWith(2), MediaType.APPLICATION_JSON));
		server.expect(requestTo(URL))
				.andRespond(withSuccess(responseWith(1), MediaType.APPLICATION_JSON));

		List<float[]> vectors = client.embedDocuments(List.of("a", "b", "c", "d", "e"));

		assertThat(vectors).hasSize(5);
		server.verify();
	}

	@Test
	void 응답_개수가_요청과_다르면_실패한다() {
		// 2개를 요청했는데 1개만 오면 청크와 벡터가 한 칸씩 밀린다. 조용히 넘어가면 안 된다.
		server.expect(times(3), requestTo(URL))
				.andRespond(withSuccess(responseWith(1), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.embedDocuments(List.of("a", "b")))
				.isInstanceOf(EmbeddingException.class);

		server.verify();
	}

	@Test
	void 서버_오류가_나면_재시도한다() {
		server.expect(requestTo(URL)).andRespond(withServerError());
		server.expect(requestTo(URL))
				.andRespond(withSuccess(responseWith(1), MediaType.APPLICATION_JSON));

		List<float[]> vectors = client.embedDocuments(List.of("텍스트"));

		assertThat(vectors).hasSize(1);
		server.verify();
	}

	@Test
	void 인증_오류는_재시도하지_않고_즉시_실패한다() {
		// 401 은 다시 호출해도 결과가 같다. 재시도하면 21초를 버리고 진짜 원인이 로그에 묻힌다.
		server.expect(times(1), requestTo(URL))
				.andRespond(withUnauthorizedRequest());

		assertThatThrownBy(() -> client.embedDocuments(List.of("텍스트")))
				.isInstanceOf(EmbeddingException.class)
				.hasMessageContaining("클라이언트 오류");

		server.verify();  // 호출이 1번뿐이었는지 확인
	}

	@Test
	void 요청_과다_429_는_재시도한다() {
		// 429 는 4xx 지만 잠시 뒤 다시 하면 성공할 수 있다.
		server.expect(requestTo(URL))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		server.expect(requestTo(URL))
				.andRespond(withSuccess(responseWith(1), MediaType.APPLICATION_JSON));

		List<float[]> vectors = client.embedDocuments(List.of("텍스트"));

		assertThat(vectors).hasSize(1);
		server.verify();
	}

	@Test
	void 응답_벡터의_차원이_다르면_실패한다() {
		// 768 이 아닌 벡터가 오면 pgvector 컬럼에 넣는 시점이 아니라 여기서 걸러야 한다.
		String wrongDimension = "{\"embeddings\":[{\"values\":[1.0,2.0,3.0]}]}";
		server.expect(times(3), requestTo(URL))
				.andRespond(withSuccess(wrongDimension, MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.embedDocuments(List.of("텍스트")))
				.isInstanceOf(EmbeddingException.class);

		server.verify();
	}

	@Test
	void 재시도를_소진하면_EmbeddingException_을_던진다() {
		server.expect(times(3), requestTo(URL)).andRespond(withServerError());

		assertThatThrownBy(() -> client.embedDocuments(List.of("텍스트")))
				.isInstanceOf(EmbeddingException.class);

		server.verify();
	}

	@Test
	void 빈_입력은_API를_호출하지_않는다() {
		assertThat(client.embedDocuments(List.of())).isEmpty();

		server.verify();
	}

	@Test
	void 모델명을_반환한다() {
		assertThat(client.modelName()).isEqualTo("gemini-embedding-001");
	}
}
