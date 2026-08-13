package com.synq.backend.domain.ai.personalization;

import java.util.List;

/**
 * AI 프롬프트에 사용할 사용자의 역할과 관심 관점이다.
 * 값은 user 도메인 enum의 원본 코드를 유지하고, 표시 문구 변환은 프롬프트에서 담당한다.
 */
public record MemberProfile(String role, String detailRole, List<String> perspectives) {

	public MemberProfile {
		role = role == null ? "" : role;
		detailRole = detailRole == null ? "" : detailRole;
		perspectives = perspectives == null ? List.of() : List.copyOf(perspectives);
	}

	public static MemberProfile empty() {
		return new MemberProfile("", "", List.of());
	}
}
