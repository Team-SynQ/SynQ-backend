package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.user.dto.ProfileImageResponse;
import com.synq.backend.domain.user.service.ProfileImageService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users/me/profile-image")
public class ProfileImageController implements ProfileImageControllerDocs {

	private final ProfileImageService profileImageService;

	public ProfileImageController(ProfileImageService profileImageService) {
		this.profileImageService = profileImageService;
	}

	@Override
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ProfileImageResponse>> upload(Long userId, @RequestParam("file") MultipartFile file) {
		String url = profileImageService.upload(userId, file);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, new ProfileImageResponse(url)));
	}

	@Override
	@DeleteMapping
	public ResponseEntity<ApiResponse<Void>> delete(Long userId) {
		profileImageService.delete(userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null));
	}
}
