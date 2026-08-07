package com.synq.backend.domain.ai.rag.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingTranscriptIndexStatusTest {

	@Test
	void 처리를_시작하면_PROCESSING_이다() {
		MeetingTranscriptIndexStatus status = MeetingTranscriptIndexStatus.startProcessing(1L, 2L);

		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.PROCESSING);
		assertThat(status.getChunkCount()).isZero();
		assertThat(status.getFailureReason()).isNull();
	}

	@Test
	void 성공하면_청크_수를_남긴다() {
		MeetingTranscriptIndexStatus status = MeetingTranscriptIndexStatus.startProcessing(1L, 2L);

		status.markCompleted(7);

		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.COMPLETED);
		assertThat(status.getChunkCount()).isEqualTo(7);
		assertThat(status.getFailureReason()).isNull();
	}

	@Test
	void 실패하면_사유를_남기고_청크_수를_0_으로_되돌린다() {
		MeetingTranscriptIndexStatus status = MeetingTranscriptIndexStatus.startProcessing(1L, 2L);
		status.markCompleted(7);

		status.markFailed("임베딩 API 실패");

		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.FAILED);
		assertThat(status.getFailureReason()).isEqualTo("임베딩 API 실패");
		// 실패 시 청크를 전부 지우므로 개수도 0 이어야 실제 저장량과 맞는다.
		assertThat(status.getChunkCount()).isZero();
	}

	@Test
	void 재시도로_성공하면_실패_사유가_지워진다() {
		MeetingTranscriptIndexStatus status = MeetingTranscriptIndexStatus.startProcessing(1L, 2L);
		status.markFailed("임베딩 API 실패");

		status.markProcessing();
		status.markCompleted(3);

		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.COMPLETED);
		assertThat(status.getFailureReason()).isNull();
	}

	@Test
	void 녹음이_없으면_SKIPPED_다() {
		MeetingTranscriptIndexStatus status = MeetingTranscriptIndexStatus.startProcessing(1L, 2L);

		status.markSkipped();

		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.SKIPPED);
		assertThat(status.getChunkCount()).isZero();
	}

	@Test
	void 실패_사유가_너무_길면_잘라서_저장한다() {
		MeetingTranscriptIndexStatus status = MeetingTranscriptIndexStatus.startProcessing(1L, 2L);

		status.markFailed("가".repeat(3000));

		assertThat(status.getFailureReason()).hasSize(1000);
	}

	@Test
	void 실패_사유가_null_이어도_전이한다() {
		// RuntimeException.getMessage() 는 null 일 수 있다.
		MeetingTranscriptIndexStatus status = MeetingTranscriptIndexStatus.startProcessing(1L, 2L);

		status.markFailed(null);

		assertThat(status.getStatus()).isEqualTo(TranscriptIndexStatus.FAILED);
		assertThat(status.getFailureReason()).isNull();
	}
}
