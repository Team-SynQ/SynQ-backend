package com.synq.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record NaverLoginRequest(
		@NotBlank String code,
		@NotBlank String state,
		// 인가 요청에 쓴 값과 다르면 네이버가 토큰 교환을 거부한다. 비우면 서버 설정값으로 폴백한다.
		String redirectUri
) {
}
