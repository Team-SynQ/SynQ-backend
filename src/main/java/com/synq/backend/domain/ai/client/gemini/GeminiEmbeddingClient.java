package com.synq.backend.domain.ai.client.gemini;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.ai.client.EmbeddingException;
import com.synq.backend.domain.ai.client.gemini.dto.GeminiEmbeddingRequest;
import com.synq.backend.domain.ai.client.gemini.dto.GeminiEmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GeminiEmbeddingClient implements EmbeddingClient {

	private static final String TASK_TYPE_DOCUMENT = "RETRIEVAL_DOCUMENT";
	private static final String TASK_TYPE_QUERY = "RETRIEVAL_QUERY";

	private final RestClient restClient;
	private final GeminiProperties properties;

	public GeminiEmbeddingClient(RestClient.Builder builder, GeminiProperties properties) {
		this.properties = properties;
		this.restClient = builder
				.baseUrl(properties.baseUrl())
				.defaultHeader("x-goog-api-key", properties.apiKey())
				.build();
	}

	@Override
	public String modelName() {
		return properties.embedding().model();
	}

	@Override
	public List<float[]> embedDocuments(List<String> texts) {
		if (texts.isEmpty()) {
			return List.of();
		}
		int batchSize = properties.embedding().batchSize();
		List<float[]> vectors = new ArrayList<>(texts.size());

		// 하나씩 호출하면 왕복이 청크 수만큼 늘고 rate limit 에 걸리기 쉽다.
		for (int start = 0; start < texts.size(); start += batchSize) {
			List<String> batch = texts.subList(start, Math.min(start + batchSize, texts.size()));
			vectors.addAll(embedBatchWithRetry(batch, TASK_TYPE_DOCUMENT));
		}
		return vectors;
	}

	@Override
	public float[] embedQuery(String text) {
		if (text == null || text.isBlank()) {
			throw new EmbeddingException("검색 질의가 비어 있습니다.");
		}
		// 단건이지만 배치 경로를 그대로 쓴다. 재시도·백오프·차원 검증·정규화가 전부 거기 있다.
		return embedBatchWithRetry(List.of(text), TASK_TYPE_QUERY).get(0);
	}

	/**
	 * 일시적 실패(네트워크, 429, 5xx)만 지수 백오프로 재시도한다.
	 * 4xx(401, 400 등)는 다시 호출해도 결과가 같으므로 즉시 실패시킨다.
	 * spring-retry 의 @Retryable 은 같은 클래스 내부 호출에서 프록시를 안 거쳐 조용히 무시되므로 직접 구현한다.
	 */
	private List<float[]> embedBatchWithRetry(List<String> batch, String taskType) {
		int maxAttempts = properties.embedding().maxAttempts();
		long backoff = properties.embedding().initialBackoffMillis();
		RuntimeException lastFailure = null;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return embedBatch(batch, taskType);
			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().value() != HttpStatus.TOO_MANY_REQUESTS.value()) {
					throw new EmbeddingException(
							"임베딩 호출이 클라이언트 오류로 실패했습니다: %s".formatted(e.getStatusCode()), e);
				}
				lastFailure = e;
				logRetry(attempt, maxAttempts, e);
			} catch (RuntimeException e) {
				lastFailure = e;
				logRetry(attempt, maxAttempts, e);
			}

			if (attempt == maxAttempts) {
				break;
			}
			sleep(backoff);
			backoff *= properties.embedding().backoffMultiplier();
		}
		throw new EmbeddingException(
				"임베딩 호출이 %d회 재시도 후에도 실패했습니다.".formatted(maxAttempts), lastFailure);
	}

	private void logRetry(int attempt, int maxAttempts, RuntimeException e) {
		log.warn("임베딩 호출 실패 (시도 {}/{}): {}", attempt, maxAttempts, e.toString());
	}

	private List<float[]> embedBatch(List<String> batch, String taskType) {
		GeminiEmbeddingRequest request = GeminiEmbeddingRequest.of(
				batch,
				properties.embedding().model(),
				taskType,
				properties.embedding().dimensions());

		GeminiEmbeddingResponse response = restClient.post()
				.uri("/models/{model}:batchEmbedContents", properties.embedding().model())
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(GeminiEmbeddingResponse.class);

		if (response == null || response.embeddings() == null) {
			throw new EmbeddingException("임베딩 응답이 비어 있습니다.");
		}
		// 개수가 어긋나면 청크와 벡터가 한 칸씩 밀려 엉뚱한 청크에 엉뚱한 벡터가 붙는다.
		if (response.embeddings().size() != batch.size()) {
			throw new EmbeddingException(
					"임베딩 응답 개수(%d)가 요청 개수(%d)와 다릅니다."
							.formatted(response.embeddings().size(), batch.size()));
		}

		return response.embeddings().stream()
				.map(embedding -> normalize(embedding.values()))
				.toList();
	}

	/**
	 * L2 정규화.
	 * gemini-embedding-001 은 3072차원이 기본이고, 768 같은 축소 차원에서는 벡터가 정규화되어 있지 않다.
	 */
	private float[] normalize(List<Float> values) {
		int expected = properties.embedding().dimensions();
		if (values.size() != expected) {
			throw new EmbeddingException(
					"임베딩 차원(%d)이 기대값(%d)과 다릅니다.".formatted(values.size(), expected));
		}

		double sumOfSquares = 0;
		for (float value : values) {
			sumOfSquares += (double) value * value;
		}
		double norm = Math.sqrt(sumOfSquares);
		if (norm == 0) {
			throw new EmbeddingException("영벡터는 정규화할 수 없습니다.");
		}

		float[] normalized = new float[values.size()];
		for (int i = 0; i < values.size(); i++) {
			normalized[i] = (float) (values.get(i) / norm);
		}
		return normalized;
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EmbeddingException("임베딩 재시도 대기 중 인터럽트되었습니다.", e);
		}
	}
}
