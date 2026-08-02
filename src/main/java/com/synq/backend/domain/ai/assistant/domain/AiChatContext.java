package com.synq.backend.domain.ai.assistant.domain;

import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import java.util.List;

/**
 * 한 번의 AI Chat 호출에 사용할 회의·사용자 맥락이다.
 */
public record AiChatContext(
		Long meetingId,
		Long userId,
		String role,
		List<String> perspectives,
		LiveContextSnapshot liveContext,
		List<AiChatTranscript> transcripts,
		List<AiChatTurn> recentTurns,
		List<AiChatReference> references,
		List<AiChatSource> sourceCandidates
) {
	public AiChatContext {
		role = role == null ? "" : role;
		perspectives = List.copyOf(perspectives);
		transcripts = List.copyOf(transcripts);
		recentTurns = List.copyOf(recentTurns);
		references = List.copyOf(references);
		sourceCandidates = List.copyOf(sourceCandidates);
	}
}
