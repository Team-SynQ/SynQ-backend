package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.user.dto.UserMeResponse;
import com.synq.backend.domain.user.dto.UserNameUpdateRequest;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.domain.user.service.UserService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController implements UserControllerDocs {

	private final UserRepository userRepository;
	private final UserService userService;

	public UserController(UserRepository userRepository, UserService userService) {
		this.userRepository = userRepository;
		this.userService = userService;
	}

	@Override
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserMeResponse>> me(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, UserMeResponse.from(user)));
	}

	@Override
	@PatchMapping("/me/name")
	public ResponseEntity<ApiResponse<UserMeResponse>> updateName(Long userId, UserNameUpdateRequest request) {
		UserMeResponse response = userService.updateName(userId, request.name());
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
