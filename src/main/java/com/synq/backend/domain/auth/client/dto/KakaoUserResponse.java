package com.synq.backend.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
		Long id,
		@JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

	public record KakaoAccount(
			Profile profile,
			// 사업자 인증 없는 상태므로 null
			String email
	) {
	}

	public record Profile(
			String nickname
	) {
	}
}
