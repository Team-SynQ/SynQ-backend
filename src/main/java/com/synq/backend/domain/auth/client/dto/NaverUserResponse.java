package com.synq.backend.domain.auth.client.dto;


public record NaverUserResponse(
		String resultcode,
		Response response
) {

	public record Response(
			String id,
			// 실명(회원이름) 동의항목은 요청하지 않아서 늘 닉네임만 쓴다.
			String nickname,
			String email
	) {
	}
}
