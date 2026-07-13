package com.synq.backend.domain.ai.client;

public class EmbeddingException extends RuntimeException {

	public EmbeddingException(String message) {
		super(message);
	}

	public EmbeddingException(String message, Throwable cause) {
		super(message, cause);
	}
}
