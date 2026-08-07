package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.dto.KakaoLoginRequest;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.service.KakaoAuthService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class KakaoAuthController implements KakaoAuthControllerDocs {

	private final KakaoAuthService kakaoAuthService;

	public KakaoAuthController(KakaoAuthService kakaoAuthService) {
		this.kakaoAuthService = kakaoAuthService;
	}

	@Override
	@PostMapping("/kakao")
	public ResponseEntity<ApiResponse<TokenResponse>> login(KakaoLoginRequest request) {
		TokenResponse response = kakaoAuthService.login(request.code());
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
