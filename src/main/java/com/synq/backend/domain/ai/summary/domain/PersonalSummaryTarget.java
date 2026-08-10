package com.synq.backend.domain.ai.summary.domain;

import java.util.List;

/**
 * role 은 user 도메인 enum 의 원본 코드다. 한글 라벨 변환은 프롬프트 렌더링 책임이라 여기서 하지 않는다.
 */
public record PersonalSummaryTarget(
		Long userId,
		String role,
		String detailRole,
		List<String> perspectives
) {
	public PersonalSummaryTarget {
		role = role == null ? "" : role;
		detailRole = detailRole == null ? "" : detailRole;
		perspectives = perspectives == null ? List.of() : List.copyOf(perspectives);
	}

	public String roleDescription() {
		if (role.isBlank()) {
			return "";
		}
		return detailRole.isBlank() ? role : role + " - " + detailRole;
	}
}
