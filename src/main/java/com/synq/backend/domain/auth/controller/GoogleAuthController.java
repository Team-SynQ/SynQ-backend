package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.dto.GoogleLoginRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.service.GoogleAuthService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/auth")
public class GoogleAuthController {

	private final GoogleAuthService googleAuthService;

	public GoogleAuthController(GoogleAuthService googleAuthService) {
		this.googleAuthService = googleAuthService;
	}

	@PostMapping("/google")
	public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody GoogleLoginRequest request) {
		TokenResponse response = googleAuthService.login(request.code());
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
