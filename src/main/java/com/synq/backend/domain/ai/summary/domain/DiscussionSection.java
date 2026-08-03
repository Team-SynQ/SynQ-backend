package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

/**
 * 회의에서 하나의 주제로 이어진 주요 논의와 세부 내용을 표현한다.
 */
public record DiscussionSection(
		String title,
		List<String> details
) {
	public DiscussionSection {
		details = details == null ? List.of() : List.copyOf(details);
	}
}
