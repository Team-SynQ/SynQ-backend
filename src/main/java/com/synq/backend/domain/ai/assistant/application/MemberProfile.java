package com.synq.backend.domain.ai.assistant.application;

import java.util.List;

/**
 * 프롬프트에 넣을 사용자 역할·관점. 값은 user 도메인 enum 의 원본 코드다.
 * 한글 라벨 변환은 프롬프트 렌더링 책임이라 여기서 하지 않는다.
 */
public record MemberProfile(String role, String detailRole, List<String> perspectives) {

	public MemberProfile {
		role = role == null ? "" : role;
		detailRole = detailRole == null ? "" : detailRole;
		perspectives = perspectives == null ? List.of() : List.copyOf(perspectives);
	}

	/** 역할·관점을 등록하지 않은 사용자. 힌트는 일반적인 톤으로 나가되 기능이 끊기지는 않는다. */
	public static MemberProfile empty() {
		return new MemberProfile("", "", List.of());
	}
}
