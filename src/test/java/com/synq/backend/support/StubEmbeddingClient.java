package com.synq.backend.support;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.ai.client.EmbeddingException;

import java.util.ArrayList;
import java.util.List;

/**
 * 실제 Gemini 를 호출하지 않는 대역. 실패를 강제할 수 있다.
 *
 * 입력 검증은 GeminiEmbeddingClient 와 같게 유지한다. 대역이 더 관대하면
 * 검증이 사라져도 테스트가 통과해 운영에서만 터진다.
 */
public class StubEmbeddingClient implements EmbeddingClient {

	private boolean shouldFail = false;

	public void failNext() {
		this.shouldFail = true;
	}

	@Override
	public List<float[]> embedDocuments(List<String> texts) {
		if (shouldFail) {
			shouldFail = false;
			throw new EmbeddingException("스텁이 강제로 실패시킴");
		}
		List<float[]> vectors = new ArrayList<>(texts.size());
		for (int i = 0; i < texts.size(); i++) {
			float[] vector = new float[768];
			vector[0] = 1.0f;  // 정규화된 단위 벡터
			vectors.add(vector);
		}
		return vectors;
	}

	@Override
	public float[] embedQuery(String text) {
		if (shouldFail) {
			shouldFail = false;
			throw new EmbeddingException("스텁이 강제로 실패시킴");
		}
		if (text == null || text.isBlank()) {
			throw new EmbeddingException("검색 질의가 비어 있습니다.");
		}
		float[] vector = new float[768];
		vector[0] = 1.0f;  // 정규화된 단위 벡터
		return vector;
	}

	@Override
	public String modelName() {
		return "stub-embedding-model";
	}
}
