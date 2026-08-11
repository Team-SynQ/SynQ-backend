package com.synq.backend.domain.ai.event;

import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 구독 전 회의 존재 여부와 현재 참가 여부를 확인하고 SSE 연결을 연다.
 */
@Service
public class AiEventSubscriptionService {

	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final AiEventSseEmitterRegistry emitterRegistry;
	private final AiEventSseProperties properties;

	public AiEventSubscriptionService(
			MeetingRepository meetingRepository,
			MeetingParticipantRepository meetingParticipantRepository,
			AiEventSseEmitterRegistry emitterRegistry,
			AiEventSseProperties properties
	) {
		this.meetingRepository = meetingRepository;
		this.meetingParticipantRepository = meetingParticipantRepository;
		this.emitterRegistry = emitterRegistry;
		this.properties = properties;
	}

	public SseEmitter subscribe(Long meetingId, Long userId) {
		if (!meetingRepository.existsById(meetingId)) {
			throw new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND);
		}
		if (!meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)) {
			throw new GeneralException(AiEventErrorCode.NOT_MEETING_PARTICIPANT);
		}

		SseEmitter emitter = emitterRegistry.register(meetingId, userId, properties.timeout().toMillis());
		emitterRegistry.sendTo(
				emitter,
				AiEventPayload.of(AiEventType.CONNECTED, meetingId, java.util.Map.of("status", "connected"))
		);
		return emitter;
	}
}
