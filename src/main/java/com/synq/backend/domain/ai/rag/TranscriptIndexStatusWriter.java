package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptIndexStatus;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptIndexStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * 인덱싱 상태 전이의 트랜잭션 경계.
 *
 * <p>인덱싱 서비스 안의 메서드로 두면 self-invocation 이라 @Transactional 프록시를 타지 않는다.
 * 그리고 임베딩 API 대기 구간까지 트랜잭션으로 감싸면 외부 응답을 기다리는 동안
 * DB 커넥션을 점유한다(open-in-view=false).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptIndexStatusWriter {

	private final MeetingTranscriptIndexStatusRepository repository;

	/** 없으면 만들고 있으면 이어 쓴다. 회의당 한 행이므로 upsert 다. */
	@Transactional
	public void markProcessing(Long meetingId, Long projectId) {
		MeetingTranscriptIndexStatus status = repository.findByMeetingId(meetingId)
				.orElseGet(() -> MeetingTranscriptIndexStatus.startProcessing(meetingId, projectId));
		status.markProcessing();
		repository.save(status);
	}

	@Transactional
	public void markCompleted(Long meetingId, int chunkCount) {
		transition(meetingId, status -> status.markCompleted(chunkCount));
	}

	@Transactional
	public void markFailed(Long meetingId, String reason) {
		transition(meetingId, status -> status.markFailed(reason));
	}

	@Transactional
	public void markSkipped(Long meetingId) {
		transition(meetingId, MeetingTranscriptIndexStatus::markSkipped);
	}

	private void transition(Long meetingId, Consumer<MeetingTranscriptIndexStatus> change) {
		repository.findByMeetingId(meetingId).ifPresentOrElse(
				status -> {
					change.accept(status);
					repository.save(status);
				},
				// markProcessing 이 항상 먼저 돌므로 실제로는 도달하지 않는다.
				// 도달했다면 호출 순서가 깨진 것이라 조용히 넘기지 않고 남긴다.
				() -> log.error("인덱싱 상태 행이 없습니다. meetingId={}", meetingId));
	}
}
