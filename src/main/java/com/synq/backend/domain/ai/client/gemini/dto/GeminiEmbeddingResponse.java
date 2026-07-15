package com.synq.backend.domain.ai.client.gemini.dto;

import java.util.List;

/** batchEmbedContents 응답 본문: {"embeddings":[{"values":[...]}]} */
public record GeminiEmbeddingResponse(List<Embedding> embeddings) {

	public record Embedding(List<Float> values) {
	}
}
