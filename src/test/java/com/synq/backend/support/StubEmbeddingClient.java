package com.synq.backend.support;

import com.synq.backend.domain.ai.client.EmbeddingClient;
import com.synq.backend.domain.ai.client.EmbeddingException;

import java.util.ArrayList;
import java.util.List;

/** 실제 Gemini 를 호출하지 않는 대역. 실패를 강제할 수 있다. */
public class StubEmbeddingClient implements EmbeddingClient {

	private boolean shouldFail = false;

	public void failNext() {
		this.shouldFail = true;
	}

	@Override
	public List<float[]> embedDocuments(List<String> texts) {
		if (shouldFail) {
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
	public String modelName() {
		return "stub-embedding-model";
	}
}
