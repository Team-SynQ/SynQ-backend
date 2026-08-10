package com.synq.backend.domain.ai.assistant.domain;

import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import java.util.List;

/**
 * 한 번의 AI Chat 호출에 사용할 회의·사용자 맥락이다.
 *
 * <p>role 은 user 도메인 enum 의 원본 코드다. 한글 라벨 변환은 프롬프트 렌더링 책임이라
 * 여기서 하지 않는다. detailRole 은 사용자 자유 입력이라 변환 대상이 아니다.
 */
public record AiChatContext(
		Long meetingId,
		Long userId,
		String role,
		String detailRole,
		List<String> perspectives,
		LiveContextSnapshot liveContext,
		List<AiChatTranscript> transcripts,
		List<AiChatTurn> recentTurns,
		List<AiChatReference> references,
		List<AiChatSource> sourceCandidates
) {
	public AiChatContext {
		role = role == null ? "" : role;
		detailRole = detailRole == null ? "" : detailRole;
		perspectives = List.copyOf(perspectives);
		transcripts = List.copyOf(transcripts);
		recentTurns = List.copyOf(recentTurns);
		references = List.copyOf(references);
		sourceCandidates = List.copyOf(sourceCandidates);
	}
}
