package com.synq.backend.domain.ai.rag.entity;

public enum TranscriptIndexStatus {
	PROCESSING,
	COMPLETED,
	FAILED,
	/** 녹음이 없어 인덱싱할 전사가 없었던 회의. 실패와 구분한다. */
	SKIPPED
}
