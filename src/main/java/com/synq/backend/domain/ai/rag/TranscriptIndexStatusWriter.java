package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptIndexStatus;
import com.synq.backend.domain.ai.rag.entity.TranscriptIndexStatus;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptIndexStatusRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
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

	/**
	 * 이 시간을 넘겨 PROCESSING 에 머문 행은 죽은 것으로 보고 인계한다.
	 * 없으면 서버가 인덱싱 도중 죽었을 때 그 회의를 영영 재인덱싱할 수 없다.
	 * 임베딩은 배치 100 + 재시도 3회라 정상 흐름이 이 값에 닿지 않는다.
	 */
	private static final Duration STALE_PROCESSING_TIMEOUT = Duration.ofMinutes(10);

	private final MeetingTranscriptIndexStatusRepository repository;

	/**
	 * 없으면 만들고 있으면 이어 쓴다. 회의당 한 행이므로 upsert 다.
	 *
	 * <p>이미 PROCESSING 이면 거부한다. 회의 종료 이벤트 인덱싱이 도는 중에 수동 재인덱싱이
	 * 들어오면 두 파이프라인이 같은 meeting_id 의 청크를 동시에 replace 해
	 * UNIQUE(meeting_id, chunk_index) 위반이나 chunk_count 불일치가 생긴다.
	 *
	 * @throws GeneralException 이미 인덱싱이 진행 중일 때(409)
	 */
	@Transactional
	public void markProcessing(Long meetingId, Long projectId) {
		MeetingTranscriptIndexStatus status = repository.findByMeetingId(meetingId).orElse(null);
		if (status == null) {
			insertProcessing(meetingId, projectId);
			return;
		}

		if (status.getStatus() == TranscriptIndexStatus.PROCESSING && !isStale(status)) {
			throw new GeneralException(GeneralErrorCode.CONFLICT);
		}

		status.markProcessing();
		repository.save(status);
	}

	private void insertProcessing(Long meetingId, Long projectId) {
		try {
			// flush 해야 PK 충돌이 이 자리에서 잡힌다. save 만 하면 커밋 시점까지 밀린다.
			repository.saveAndFlush(MeetingTranscriptIndexStatus.startProcessing(meetingId, projectId));
		} catch (DataIntegrityViolationException e) {
			// 첫 인덱싱을 두 요청이 동시에 시작하면 PK(meeting_id) 가 하나를 막는다.
			throw new GeneralException(GeneralErrorCode.CONFLICT, e);
		}
	}

	private boolean isStale(MeetingTranscriptIndexStatus status) {
		return status.getUpdatedAt().isBefore(OffsetDateTime.now().minus(STALE_PROCESSING_TIMEOUT));
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
