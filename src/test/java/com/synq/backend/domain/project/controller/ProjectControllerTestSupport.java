package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.support.PostgresTestContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class ProjectControllerTestSupport extends PostgresTestContainer {

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	protected String bearer(User user) {
		return bearer(user.getUserId());
	}

	protected String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}
}
