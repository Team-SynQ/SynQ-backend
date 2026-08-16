package com.synq.backend.domain.auth.client.dto;


public record NaverUserResponse(
		String resultcode,
		Response response
) {

	public record Response(
			String id,
			String nickname,
			String name,
			String email
	) {
	}
}
