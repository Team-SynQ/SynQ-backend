package com.synq.backend.domain.auth.jwt;


public class UserAuthDto {

	private final Long userId;

	public UserAuthDto(Long userId) {
		this.userId = userId;
	}

	public Long getUserId() {
		return userId;
	}
}
