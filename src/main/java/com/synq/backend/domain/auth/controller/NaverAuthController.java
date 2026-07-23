package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.dto.NaverLoginRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.service.NaverAuthService;
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
public class NaverAuthController {

	private final NaverAuthService naverAuthService;

	public NaverAuthController(NaverAuthService naverAuthService) {
		this.naverAuthService = naverAuthService;
	}

	@PostMapping("/naver")
	public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody NaverLoginRequest request) {
		TokenResponse response = naverAuthService.login(request.code(), request.state());
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
