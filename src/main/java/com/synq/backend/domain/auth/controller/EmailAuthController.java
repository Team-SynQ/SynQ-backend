package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.dto.LoginRequest;
import com.synq.backend.domain.auth.dto.SignupRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.service.EmailAuthService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class EmailAuthController implements EmailAuthControllerDocs {

	private final EmailAuthService emailAuthService;

	public EmailAuthController(EmailAuthService emailAuthService) {
		this.emailAuthService = emailAuthService;
	}

	@Override
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<TokenResponse>> signup(SignupRequest request) {
		return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, emailAuthService.signup(request)));
	}

	@Override
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(LoginRequest request) {
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, emailAuthService.login(request)));
	}
}
