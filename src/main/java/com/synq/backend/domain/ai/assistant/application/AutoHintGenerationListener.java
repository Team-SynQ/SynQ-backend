package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.service.HintService;
import com.synq.backend.domain.ai.context.domain.AutoHintDecision;
import com.synq.backend.domain.ai.event.AutoHintCreatedEvent;
import com.synq.backend.domain.ai.event.LiveContextUpdatedEvent;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 중요 전사 하나를 활성 참여자별 개인 3-hint로 확장한다.
 *
 * <p>전사 중요도 판단은 Live Context OpenAI 호출에서 함께 받아 별도 분류 호출을 만들지 않는다.
 * 참여자 하나의 AI 호출이나 저장이 실패해도 다른 참여자 생성에는 영향을 주지 않는다.
 */
@Slf4j
@Component
public class AutoHintGenerationListener {

	private final AutoHintProperties properties;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final MeetingRepository meetingRepository;
	private final HintService hintService;
	private final ApplicationEventPublisher eventPublisher;
	private final Executor autoHintExecutor;
	private final Set<AutoHintKey> generating = ConcurrentHashMap.newKeySet();

	public AutoHintGenerationListener(
			AutoHintProperties properties,
			MeetingParticipantRepository meetingParticipantRepository,
			MeetingRepository meetingRepository,
			HintService hintService,
			ApplicationEventPublisher eventPublisher,
			@Qualifier("autoHintExecutor") Executor autoHintExecutor
	) {
		this.properties = properties;
		this.meetingParticipantRepository = meetingParticipantRepository;
		this.meetingRepository = meetingRepository;
		this.hintService = hintService;
		this.eventPublisher = eventPublisher;
		this.autoHintExecutor = autoHintExecutor;
	}

	@EventListener
	public void generate(LiveContextUpdatedEvent event) {
		AutoHintDecision decision = event.autoHintDecision();
		if (!properties.enabled() || !decision.shouldGenerate()
				|| decision.targetSegmentId() == null
				|| decision.importance() < properties.importanceThreshold()) {
			return;
		}
		if (!meetingRepository.findById(event.meetingId())
				.map(meeting -> meeting.getStatus() == MeetingStatus.IN_PROGRESS)
				.orElse(false)) {
			return;
		}

		meetingParticipantRepository.findByMeetingIdAndLeftAtIsNull(event.meetingId()).stream()
				.map(MeetingParticipant::getUserId)
				.forEach(userId -> submit(event, userId, decision));
	}

	private void submit(LiveContextUpdatedEvent event, Long userId, AutoHintDecision decision) {
		String topic = event.context().currentTopic();
		if (hintService.hasRecentAutomaticDuplicate(event.meetingId(), userId, topic, decision.triggerReason())) {
			return;
		}
		AutoHintKey key = new AutoHintKey(event.meetingId(), userId, topic, decision.triggerReason());
		if (!generating.add(key)) {
			return;
		}
		try {
			autoHintExecutor.execute(() -> generateForParticipant(event, userId, decision, key));
		} catch (RuntimeException exception) {
			generating.remove(key);
			log.error("자동 3-hint 작업을 제출하지 못했습니다. meetingId={}, segmentId={}, userId={}",
					event.meetingId(), decision.targetSegmentId(), userId, exception);
		}
	}

	private void generateForParticipant(
			LiveContextUpdatedEvent event,
			Long userId,
			AutoHintDecision decision,
			AutoHintKey key
	) {
		try {
			HintResult result = hintService.generateAutomatically(userId, event.meetingId(), decision.targetSegmentId());
			hintService.saveAutomatically(event.meetingId(), decision.targetSegmentId(), userId, result,
					decision.importance(), decision.triggerReason(), event.context().currentTopic())
					.ifPresent(hint -> eventPublisher.publishEvent(new AutoHintCreatedEvent(event.meetingId(), userId, hint)));
		} catch (RuntimeException exception) {
			log.warn("자동 3-hint 생성에 실패했습니다. meetingId={}, segmentId={}, userId={}",
					event.meetingId(), decision.targetSegmentId(), userId, exception);
		} finally {
			generating.remove(key);
		}
	}

	private record AutoHintKey(Long meetingId, Long userId, String topic, String triggerReason) {
	}
}
