package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

public record PersonalSummaryTarget(
		Long userId,
		String roleDescription,
		List<String> perspectives
) {
	public PersonalSummaryTarget {
		perspectives = perspectives == null ? List.of() : List.copyOf(perspectives);
	}
}
