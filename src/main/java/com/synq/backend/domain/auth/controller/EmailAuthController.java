package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.dto.LoginRequest;
import com.synq.backend.domain.auth.dto.SignupRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.service.EmailAuthService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/auth")
public class EmailAuthController {

	private final EmailAuthService emailAuthService;

	public EmailAuthController(EmailAuthService emailAuthService) {
		this.emailAuthService = emailAuthService;
	}

	@PostMapping("/signup")
	public ApiResponse<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, emailAuthService.signup(request));
	}

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, emailAuthService.login(request));
	}
}
