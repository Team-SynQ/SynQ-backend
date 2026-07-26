package com.synq.backend.domain.ai.assistant.domain;

import com.synq.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 사용자 질문과 그 질문에 대한 AI 답변을 하나의 대화 단위로 보관한다.
 */
@Entity
@Table(name = "ai_chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatMessage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "linked_segment_id")
	private Long linkedSegmentId;

	@Column(name = "client_request_id", nullable = false)
	private UUID clientRequestId;

	@Column(nullable = false, columnDefinition = "text")
	private String question;

	@Column(columnDefinition = "text")
	private String answer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AiChatStatus status;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "source_refs", nullable = false, columnDefinition = "jsonb")
	private List<AiChatSource> sourceRefs = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "suggested_questions", nullable = false, columnDefinition = "jsonb")
	private List<String> suggestedQuestions = new ArrayList<>();

	@Column(name = "error_code", length = 50)
	private String errorCode;

	@Column(name = "error_message", columnDefinition = "text")
	private String errorMessage;

	private AiChatMessage(
			Long meetingId,
			Long userId,
			Long linkedSegmentId,
			UUID clientRequestId,
			String question
	) {
		this.meetingId = meetingId;
		this.userId = userId;
		this.linkedSegmentId = linkedSegmentId;
		this.clientRequestId = clientRequestId;
		this.question = question;
		this.status = AiChatStatus.GENERATING;
	}

	public static AiChatMessage start(
			Long meetingId,
			Long userId,
			Long linkedSegmentId,
			UUID clientRequestId,
			String question
	) {
		return new AiChatMessage(meetingId, userId, linkedSegmentId, clientRequestId, question);
	}

	public void complete(AiChatResult result) {
		this.answer = result.answer();
		this.sourceRefs = new ArrayList<>(result.sources());
		this.suggestedQuestions = new ArrayList<>(result.suggestedQuestions());
		this.status = AiChatStatus.COMPLETED;
		this.errorCode = null;
		this.errorMessage = null;
	}

	public void fail(String errorCode, String errorMessage) {
		this.status = AiChatStatus.FAILED;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
}
