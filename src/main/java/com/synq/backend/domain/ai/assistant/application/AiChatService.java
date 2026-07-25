package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.ai.assistant.code.AiChatErrorCode;
import com.synq.backend.domain.ai.assistant.domain.AiChatClient;
import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;
import com.synq.backend.domain.ai.assistant.domain.AiChatPrompt;
import com.synq.backend.domain.ai.assistant.domain.AiChatResult;
import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final AiChatClient aiChatClient;
	private final AiChatStore aiChatStore;

	public AiChatService(
			MeetingRepository meetingRepository,
			MeetingParticipantRepository meetingParticipantRepository,
			AiChatClient aiChatClient,
			AiChatStore aiChatStore
	) {
		this.meetingRepository = meetingRepository;
		this.meetingParticipantRepository = meetingParticipantRepository;
		this.aiChatClient = aiChatClient;
		this.aiChatStore = aiChatStore;
	}

	public AiChatSendResult send(
			Long meetingId,
			Long userId,
			String question,
			Long linkedSegmentId,
			UUID clientRequestId
	) {
		validateSendAccess(meetingId, userId, linkedSegmentId);
		String normalizedQuestion = question.trim();

		AiChatMessage message;
		try {
			message = aiChatStore.start(
					meetingId,
					userId,
					linkedSegmentId,
					clientRequestId,
					normalizedQuestion
			);
		} catch (DataIntegrityViolationException duplicateRequest) {
			AiChatMessage existing = aiChatStore.findByRequestId(meetingId, userId, clientRequestId)
					.orElseThrow(() -> duplicateRequest);
			if (!existing.getQuestion().equals(normalizedQuestion)
					|| !Objects.equals(existing.getLinkedSegmentId(), linkedSegmentId)) {
				throw new GeneralException(AiChatErrorCode.IDEMPOTENCY_KEY_REUSED);
			}
			return new AiChatSendResult(existing, false);
		}

		AiChatResult result;
		try {
			result = aiChatClient.generate(
					new AiChatPrompt(meetingId, userId, normalizedQuestion, linkedSegmentId)
			);
		} catch (RuntimeException exception) {
			aiChatStore.fail(
					message.getId(),
					AiChatErrorCode.AI_GENERATION_FAILED.getCode(),
					AiChatErrorCode.AI_GENERATION_FAILED.getMessage()
			);
			throw new GeneralException(AiChatErrorCode.AI_GENERATION_FAILED, exception);
		}

		return new AiChatSendResult(aiChatStore.complete(message.getId(), result), true);
	}

	public Page<AiChatMessage> getHistory(Long meetingId, Long userId, int page, int size) {
		validateHistoryAccess(meetingId, userId);
		return aiChatStore.findHistory(meetingId, userId, page, size);
	}

	private void validateSendAccess(Long meetingId, Long userId, Long linkedSegmentId) {
		Meeting meeting = findMeeting(meetingId);
		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(AiChatErrorCode.CHAT_NOT_AVAILABLE);
		}
		if (!meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)) {
			throw new GeneralException(AiChatErrorCode.NOT_MEETING_PARTICIPANT);
		}
		// transcript_segment 영속 도메인이 main에 아직 없으므로, 소유 관계를 검증할 수 없는 ID는 사용하지 않는다.
		if (linkedSegmentId != null) {
			throw new GeneralException(AiChatErrorCode.LINKED_SEGMENT_NOT_AVAILABLE);
		}
	}

	private void validateHistoryAccess(Long meetingId, Long userId) {
		findMeeting(meetingId);
		if (!meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
			throw new GeneralException(AiChatErrorCode.NOT_MEETING_PARTICIPANT);
		}
	}

	private Meeting findMeeting(Long meetingId) {
		return meetingRepository.findById(meetingId)
				.orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
	}
}
