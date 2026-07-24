package com.synq.backend.domain.ai.assistant.domain;

import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import java.util.List;

/**
 * 3-hint 프롬프트 재료. liveContext 는 압축된 과거, focus/window 는 정확한 현재다.
 */
public record HintInput(
		String focusSegment,
		List<String> windowBefore,
		List<String> windowAfter,
		String role,
		String perspective,
		LiveContextSnapshot liveContext,
		List<ChunkMatch> references
) {
}
