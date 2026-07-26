package com.synq.backend.domain.ai.assistant.application;

import com.synq.backend.domain.ai.assistant.domain.AiChatMessage;
import com.synq.backend.domain.ai.assistant.domain.AiChatResult;
import com.synq.backend.domain.ai.assistant.repository.AiChatMessageRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 AI 호출과 DB 트랜잭션을 분리하기 위한 채팅 저장 경계다.
 */
@Component
public class AiChatStore {

	private final AiChatMessageRepository aiChatMessageRepository;

	public AiChatStore(AiChatMessageRepository aiChatMessageRepository) {
		this.aiChatMessageRepository = aiChatMessageRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AiChatMessage start(
			Long meetingId,
			Long userId,
			Long linkedSegmentId,
			UUID clientRequestId,
			String question
	) {
		return aiChatMessageRepository.saveAndFlush(
				AiChatMessage.start(meetingId, userId, linkedSegmentId, clientRequestId, question)
		);
	}

	@Transactional(readOnly = true)
	public Optional<AiChatMessage> findByRequestId(Long meetingId, Long userId, UUID clientRequestId) {
		return aiChatMessageRepository.findByMeetingIdAndUserIdAndClientRequestId(
				meetingId,
				userId,
				clientRequestId
		);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AiChatMessage complete(Long messageId, AiChatResult result) {
		AiChatMessage message = aiChatMessageRepository.findById(messageId).orElseThrow();
		message.complete(result);
		return aiChatMessageRepository.save(message);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void fail(Long messageId, String errorCode, String errorMessage) {
		aiChatMessageRepository.findById(messageId).ifPresent(message -> {
			message.fail(errorCode, errorMessage);
			aiChatMessageRepository.save(message);
		});
	}

	@Transactional(readOnly = true)
	public Page<AiChatMessage> findHistory(Long meetingId, Long userId, int page, int size) {
		return aiChatMessageRepository.findByMeetingIdAndUserIdOrderByCreatedAtDescIdDesc(
				meetingId,
				userId,
				PageRequest.of(page, size)
		);
	}
}
