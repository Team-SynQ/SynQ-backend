package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.domain.auth.jwt.ActiveUserChecker;
import com.synq.backend.domain.auth.jwt.JwtProperties;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.auth.repository.RefreshTokenRedisRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthTokenServiceTest {

	private final JwtProvider jwtProvider = mock(JwtProvider.class);
	private final RefreshTokenRedisRepository refreshTokenRepository = mock(RefreshTokenRedisRepository.class);
	private final RoleProfileRepository roleProfileRepository = mock(RoleProfileRepository.class);
	private final ActiveUserChecker activeUserChecker = mock(ActiveUserChecker.class);
	private final AuthTokenService service = new AuthTokenService(
			jwtProvider,
			refreshTokenRepository,
			new JwtProperties("test-secret-that-is-long-enough-for-jwt-signing", 30, 14),
			roleProfileRepository,
			activeUserChecker
	);

	@Test
	void 탈퇴한_사용자의_Refresh_Token_갱신을_거부한다() {
		when(refreshTokenRepository.findUserIdByTokenHash(anyString())).thenReturn(Optional.of(10L));
		when(activeUserChecker.isActive(10L)).thenReturn(false);

		assertThatThrownBy(() -> service.refresh("withdrawn-refresh-token"))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
								.isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));

		verify(refreshTokenRepository, never()).rotate(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any());
		verify(jwtProvider, never()).createAccessToken(org.mockito.ArgumentMatchers.anyLong());
	}
}
