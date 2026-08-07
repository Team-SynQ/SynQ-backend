package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.user.dto.ProfileImageResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "유저")
@SecurityRequirement(name = "bearerAuth")
public interface ProfileImageControllerDocs {

	@Operation(summary = "프로필 이미지 등록/변경",
			description = "프로필 이미지를 등록하거나 기존 이미지를 새 파일로 교체한다(jpg/png/webp, 최대 5MB). "
					+ "기존 이미지가 있으면 교체 후 이전 파일은 삭제된다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록/변경 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "파일이 비어있거나, 형식이 jpg/png/webp가 아니거나, 5MB를 초과함"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
	})
	ResponseEntity<ApiResponse<ProfileImageResponse>> upload(
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@RequestParam("file") MultipartFile file);

	@Operation(summary = "프로필 이미지 삭제",
			description = "등록된 프로필 이미지를 삭제한다. 이미지가 없는 상태에서 호출해도 에러 없이 처리된다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
	})
	ResponseEntity<ApiResponse<Void>> delete(
			@AuthenticationPrincipal(expression = "userId") Long userId);
}
