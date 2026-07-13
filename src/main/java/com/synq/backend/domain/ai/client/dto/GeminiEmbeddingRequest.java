package com.synq.backend.domain.ai.client.dto;

import java.util.List;

/** POST {base}/models/{model}:batchEmbedContents 요청 본문. */
public record GeminiEmbeddingRequest(List<EmbedRequest> requests) {

	public record EmbedRequest(
			String model,
			Content content,
			String taskType,
			int outputDimensionality
	) {
	}

	public record Content(List<Part> parts) {
	}

	public record Part(String text) {
	}

	public static GeminiEmbeddingRequest of(List<String> texts, String model,
											String taskType, int dimensions) {
		String qualifiedModel = "models/" + model;
		List<EmbedRequest> requests = texts.stream()
				.map(text -> new EmbedRequest(
						qualifiedModel,
						new Content(List.of(new Part(text))),
						taskType,
						dimensions))
				.toList();
		return new GeminiEmbeddingRequest(requests);
	}
}
