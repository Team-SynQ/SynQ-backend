package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.user.dto.RoleProfileRequest;
import com.synq.backend.domain.user.dto.RoleProfileResponse;
import com.synq.backend.domain.user.service.RoleProfileService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/me/role-profiles")
public class RoleProfileController implements RoleProfileControllerDocs {

	private final RoleProfileService roleProfileService;

	public RoleProfileController(RoleProfileService roleProfileService) {
		this.roleProfileService = roleProfileService;
	}

	@Override
	public ResponseEntity<ApiResponse<List<RoleProfileResponse>>> getMyRoleProfiles(Long userId) {
		List<RoleProfileResponse> response = roleProfileService.getMyRoleProfiles(userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<RoleProfileResponse>> create(Long userId, RoleProfileRequest request) {
		RoleProfileResponse response = roleProfileService.create(userId, request);
		return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response));
	}

	@Override
	public ResponseEntity<ApiResponse<RoleProfileResponse>> update(Long userId, Long profileId,
																	RoleProfileRequest request) {
		RoleProfileResponse response = roleProfileService.update(userId, profileId, request);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<Void>> delete(Long userId, Long profileId) {
		roleProfileService.delete(userId, profileId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null));
	}

	@Override
	public ResponseEntity<ApiResponse<Void>> setDefault(Long userId, Long profileId) {
		roleProfileService.setDefault(userId, profileId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null));
	}
}
