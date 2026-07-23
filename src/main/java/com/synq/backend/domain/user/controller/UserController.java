package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.user.dto.UserMeResponse;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "유저")
@RestController
@RequestMapping("/users")
public class UserController {

	private final UserRepository userRepository;

	public UserController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserMeResponse>> me(
			@AuthenticationPrincipal(expression = "userId") Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, UserMeResponse.from(user)));
	}
}
