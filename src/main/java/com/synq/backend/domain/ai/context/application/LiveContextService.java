package com.synq.backend.domain.ai.context.application;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import com.synq.backend.domain.ai.context.domain.LiveContextAiClient;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * 확정 전사를 기존 AI 맥락에 반영하고, 회의별 최신 상태를 갱신한다.
 */
@Service
public class LiveContextService {

	private static final int MAX_SAVE_ATTEMPTS = 2;

	private final LiveContextRepository liveContextRepository;
	private final LiveContextAiClient liveContextAiClient;
	private final TranscriptSegmentRepository transcriptSegmentRepository;
	private final LiveContextBatchProperties batchProperties;

	@Autowired
	public LiveContextService(
			LiveContextRepository liveContextRepository,
			LiveContextAiClient liveContextAiClient,
			TranscriptSegmentRepository transcriptSegmentRepository,
			LiveContextBatchProperties batchProperties
	) {
		this.liveContextRepository = liveContextRepository;
		this.liveContextAiClient = liveContextAiClient;
		this.transcriptSegmentRepository = transcriptSegmentRepository;
		this.batchProperties = batchProperties;
	}

	// 기존 단위 테스트와 단건 갱신 호출 호환용 생성자다. 배치 갱신에는 사용하지 않는다.
	public LiveContextService(
			LiveContextRepository liveContextRepository,
			LiveContextAiClient liveContextAiClient
	) {
		this(liveContextRepository, liveContextAiClient, null, null);
	}

	public LiveContextService(
			LiveContextRepository liveContextRepository,
			LiveContextAiClient liveContextAiClient,
			TranscriptSegmentRepository transcriptSegmentRepository
	) {
		this(liveContextRepository, liveContextAiClient, transcriptSegmentRepository,
				new LiveContextBatchProperties(3, 3, java.time.Duration.ofSeconds(30), java.time.Duration.ofSeconds(30)));
	}

	/** 기존 Live Context 조회 계약을 유지한다. */
	public Optional<LiveContext> refresh(TranscriptFinalizedEvent event) {
		return refreshWithDecision(event).map(LiveContextRefreshResult::context);
	}

	public Optional<LiveContextRefreshResult> refreshWithDecision(TranscriptFinalizedEvent event) {
		RuntimeException lastException = null;

		for (int attempt = 0; attempt < MAX_SAVE_ATTEMPTS; attempt++) {
			Optional<LiveContext> existing = liveContextRepository.findByMeetingId(event.meetingId());

			if (existing.isPresent() && existing.get().hasProcessed(event)) {
				// STT 재전송이나 재연결로 같은 확정 전사가 다시 와도 AI 비용을 발생시키지 않는다.
				return Optional.empty();
			}

			LiveContextSnapshot previousContext = existing
					.map(LiveContextSnapshot::from)
					.orElseGet(LiveContextSnapshot::empty);
			LiveContextResult result = liveContextAiClient.refresh(previousContext, event);

			try {
				if (existing.isPresent()) {
					LiveContext context = existing.get();
					context.update(result, event);
					return Optional.of(new LiveContextRefreshResult(
							liveContextRepository.saveAndFlush(context), result.autoHintDecision()));
				}

				LiveContext created = LiveContext.create(event.meetingId(), result, event);
				return Optional.of(new LiveContextRefreshResult(
						liveContextRepository.saveAndFlush(created), result.autoHintDecision()));
			} catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
				// 다른 확정 전사가 먼저 저장된 경우 최신 맥락을 다시 읽고 한 번만 재계산한다.
				lastException = e;
			}
		}

		throw new IllegalStateException("회의 맥락을 동시에 갱신하지 못했습니다.", lastException);
	}

	/**
	 * 마지막 반영 이후의 전사를 DB에서 다시 읽어 한 번에 갱신한다. 이벤트 큐가 유실되거나
	 * 배치 대기 중 서버가 재시작되어도 다음 갱신에서 누락 구간을 회복할 수 있다.
	 */
	public Optional<LiveContextRefreshResult> refreshPending(Long meetingId) {
		if (transcriptSegmentRepository == null) {
			throw new IllegalStateException("배치 Live Context 갱신에는 TranscriptSegmentRepository가 필요합니다.");
		}

		int lastSequenceIndex = liveContextRepository.findByMeetingId(meetingId)
				.map(LiveContext::getLastSequenceIndex)
				.orElse(-1);
		List<TranscriptSegment> pending = transcriptSegmentRepository
				.findByMeetingIdAndSequenceIndexGreaterThanOrderByStartMsAscSequenceIndexAsc(
						meetingId,
						lastSequenceIndex,
						PageRequest.of(0, batchProperties.maxSegmentsPerRequest() + 1));
		if (pending.isEmpty()) {
			return Optional.empty();
		}
		boolean hasMorePending = pending.size() > batchProperties.maxSegmentsPerRequest();
		if (hasMorePending) {
			pending = pending.subList(0, batchProperties.maxSegmentsPerRequest());
		}

		TranscriptSegment last = pending.get(pending.size() - 1);
		TranscriptFinalizedEvent batchEvent = new TranscriptFinalizedEvent(
				meetingId,
				last.getId(),
				last.getSequenceIndex(),
				pending.get(0).getStartMs(),
				last.getEndMs(),
				formatBatch(pending),
				null
		);
		Set<Long> pendingSegmentIds = pending.stream().map(TranscriptSegment::getId).collect(Collectors.toSet());

		return refreshWithDecision(batchEvent).map(refreshed -> new LiveContextRefreshResult(
				refreshed.context(), normalizeAutoHintDecision(refreshed.autoHintDecision(), pendingSegmentIds), hasMorePending));
	}

	private String formatBatch(List<TranscriptSegment> pending) {
		return pending.stream()
				.map(segment -> "[segmentId=%d]\n%s".formatted(segment.getId(), segment.getContent()))
				.collect(Collectors.joining("\n\n"));
	}

	private com.synq.backend.domain.ai.context.domain.AutoHintDecision normalizeAutoHintDecision(
			com.synq.backend.domain.ai.context.domain.AutoHintDecision decision,
			Set<Long> pendingSegmentIds
	) {
		if (!decision.shouldGenerate() || decision.targetSegmentId() == null
				|| !pendingSegmentIds.contains(decision.targetSegmentId())) {
			return com.synq.backend.domain.ai.context.domain.AutoHintDecision.none();
		}
		return decision;
	}

	@Transactional(readOnly = true)
	public LiveContext get(Long meetingId) {
		return liveContextRepository.findByMeetingId(meetingId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
	}
}
