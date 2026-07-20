package com.synq.backend.domain.ai.context.application;

import com.synq.backend.domain.ai.context.domain.LiveContext;
import com.synq.backend.domain.ai.context.domain.LiveContextAiClient;
import com.synq.backend.domain.ai.context.domain.LiveContextResult;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.context.repository.LiveContextRepository;
import com.synq.backend.domain.transcript.event.TranscriptFinalizedEvent;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.Optional;
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

	public LiveContextService(
			LiveContextRepository liveContextRepository,
			LiveContextAiClient liveContextAiClient
	) {
		this.liveContextRepository = liveContextRepository;
		this.liveContextAiClient = liveContextAiClient;
	}

	public Optional<LiveContext> refresh(TranscriptFinalizedEvent event) {
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
					return Optional.of(liveContextRepository.saveAndFlush(context));
				}

				LiveContext created = LiveContext.create(event.meetingId(), result, event);
				return Optional.of(liveContextRepository.saveAndFlush(created));
			} catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
				// 다른 확정 전사가 먼저 저장된 경우 최신 맥락을 다시 읽고 한 번만 재계산한다.
			}
		}

		throw new IllegalStateException("회의 맥락을 동시에 갱신하지 못했습니다.");
	}

	@Transactional(readOnly = true)
	public LiveContext get(Long meetingId) {
		return liveContextRepository.findByMeetingId(meetingId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
	}
}
