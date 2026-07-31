package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.ai.assistant.code.AiChatErrorCode;
import com.synq.backend.domain.ai.assistant.domain.AiChatClient;
import com.synq.backend.domain.ai.assistant.domain.AiChatContext;
import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;
import com.synq.backend.domain.ai.assistant.domain.AiChatPrompt;
import com.synq.backend.domain.ai.assistant.domain.AiChatResult;
import com.synq.backend.domain.ai.assistant.domain.AiChatStatus;
import com.synq.backend.domain.ai.assistant.domain.AiChatWelcome;
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
	private final AiChatContextBuilder contextBuilder;

	public AiChatService(
			MeetingRepository meetingRepository,
			MeetingParticipantRepository meetingParticipantRepository,
			AiChatClient aiChatClient,
			AiChatStore aiChatStore,
			AiChatContextBuilder contextBuilder
	) {
		this.meetingRepository = meetingRepository;
		this.meetingParticipantRepository = meetingParticipantRepository;
		this.aiChatClient = aiChatClient;
		this.aiChatStore = aiChatStore;
		this.contextBuilder = contextBuilder;
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
		AiChatMessage existing = aiChatStore.findByRequestId(meetingId, userId, clientRequestId).orElse(null);
		if (existing != null) {
			return existingResult(existing, normalizedQuestion, linkedSegmentId);
		}

		// 선택 발화 검증과 RAG 검색이 실패하면 메시지를 남기지 않는다.
		AiChatContext context;
		try {
			context = contextBuilder.build(meetingId, userId, normalizedQuestion, linkedSegmentId);
		} catch (GeneralException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new GeneralException(AiChatErrorCode.AI_GENERATION_FAILED, exception);
		}

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
			AiChatMessage raceWinner = aiChatStore.findByRequestId(meetingId, userId, clientRequestId)
						.orElseThrow(() -> duplicateRequest);
			return existingResult(raceWinner, normalizedQuestion, linkedSegmentId);
		}

		AiChatResult result;
		try {
			result = aiChatClient.generate(new AiChatPrompt(normalizedQuestion, context));
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

	private AiChatSendResult existingResult(
			AiChatMessage existing,
			String normalizedQuestion,
			Long linkedSegmentId
	) {
		if (!existing.getQuestion().equals(normalizedQuestion)
				|| !Objects.equals(existing.getLinkedSegmentId(), linkedSegmentId)) {
			throw new GeneralException(AiChatErrorCode.IDEMPOTENCY_KEY_REUSED);
		}
		if (existing.getStatus() == AiChatStatus.FAILED) {
			throw new GeneralException(AiChatErrorCode.AI_GENERATION_FAILED);
		}
		return new AiChatSendResult(existing, false);
	}

	public Page<AiChatMessage> getHistory(Long meetingId, Long userId, int page, int size) {
		validateHistoryAccess(meetingId, userId);
		return aiChatStore.findHistory(meetingId, userId, page, size);
	}

	public AiChatWelcome getWelcome(Long meetingId, Long userId) {
		validateSendAccess(meetingId, userId, null);
		try {
			return aiChatClient.generateWelcome(contextBuilder.build(
					meetingId,
					userId,
					"현재 회의의 핵심 논의와 사용자가 확인할 다음 사항",
					null
			));
		} catch (RuntimeException exception) {
			throw new GeneralException(AiChatErrorCode.AI_GENERATION_FAILED, exception);
		}
	}

	private void validateSendAccess(Long meetingId, Long userId, Long linkedSegmentId) {
		Meeting meeting = findMeeting(meetingId);
		if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
			throw new GeneralException(AiChatErrorCode.CHAT_NOT_AVAILABLE);
		}
		if (!meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(meetingId, userId)) {
			throw new GeneralException(AiChatErrorCode.NOT_MEETING_PARTICIPANT);
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
