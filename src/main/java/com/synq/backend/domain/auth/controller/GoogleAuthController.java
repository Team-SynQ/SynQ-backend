package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.dto.GoogleLoginRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.service.GoogleAuthService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class GoogleAuthController implements GoogleAuthControllerDocs {

	private final GoogleAuthService googleAuthService;

	public GoogleAuthController(GoogleAuthService googleAuthService) {
		this.googleAuthService = googleAuthService;
	}

	@Override
	@PostMapping("/google")
	public ResponseEntity<ApiResponse<TokenResponse>> login(GoogleLoginRequest request) {
		TokenResponse response = googleAuthService.login(request.code(), request.redirectUri());
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
