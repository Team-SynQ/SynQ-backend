package com.synq.backend.domain.auth.controller;

import com.synq.backend.domain.auth.dto.NaverLoginRequest;
import com.synq.backend.domain.auth.dto.NaverStateResponse;
import com.synq.backend.domain.auth.dto.TokenResponse;
import com.synq.backend.domain.auth.service.NaverAuthService;
import com.synq.backend.domain.auth.service.NaverOAuthStateService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class NaverAuthController implements NaverAuthControllerDocs {

	private final NaverAuthService naverAuthService;
	private final NaverOAuthStateService naverOAuthStateService;

	public NaverAuthController(NaverAuthService naverAuthService, NaverOAuthStateService naverOAuthStateService) {
		this.naverAuthService = naverAuthService;
		this.naverOAuthStateService = naverOAuthStateService;
	}

	@Override
	@GetMapping("/naver/state")
	public ResponseEntity<ApiResponse<NaverStateResponse>> issueState() {
		String state = naverOAuthStateService.issue();
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, new NaverStateResponse(state)));
	}

	@Override
	@PostMapping("/naver")
	public ResponseEntity<ApiResponse<TokenResponse>> login(NaverLoginRequest request) {
		TokenResponse response = naverAuthService.login(request.code(), request.state());
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
