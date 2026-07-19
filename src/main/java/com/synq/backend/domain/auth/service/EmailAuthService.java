package com.synq.backend.domain.auth.service;

import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.domain.auth.dto.LoginRequest;
import com.synq.backend.domain.auth.dto.SignupRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.user.entity.Provider;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// dev 임시 이메일 로그인
@Service
public class EmailAuthService {

	private final UserRepository userRepository;
	private final AuthTokenService authTokenService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public EmailAuthService(UserRepository userRepository, AuthTokenService authTokenService) {
		this.userRepository = userRepository;
		this.authTokenService = authTokenService;
	}

	@Transactional
	public TokenResponse signup(SignupRequest request) {
		if (userRepository.findByProviderAndEmail(Provider.LOCAL, request.email()).isPresent()) {
			throw new GeneralException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}

		String passwordHash = passwordEncoder.encode(request.password());
		User user = userRepository.save(User.ofLocal(request.name(), request.email(), passwordHash));

		return authTokenService.issue(user.getUserId(), true);
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByProviderAndEmail(Provider.LOCAL, request.email())
				.orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new GeneralException(AuthErrorCode.INVALID_CREDENTIALS);
		}

		return authTokenService.issue(user.getUserId(), false);
	}
}
