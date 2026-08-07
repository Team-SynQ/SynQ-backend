package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.user.dto.UserMeResponse;
import com.synq.backend.domain.user.dto.UserNameUpdateRequest;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "유저")
@SecurityRequirement(name = "bearerAuth")
public interface UserControllerDocs {

	@Operation(summary = "마이페이지 기본 정보 조회",
			description = "로그인한 사용자의 기본 정보(userId, name, email, provider, profileImageUrl)를 조회한다. "
					+ "email은 소셜 로그인 사용자의 경우 없을 수 있다(null). "
					+ "profileImageUrl은 프로필 이미지를 등록한 적이 없으면 null이다. "
					+ "마이페이지에서 역할·관점 프로필까지 함께 보여줘야 한다면 "
					+ "GET /users/me/role-profiles를 별도로 호출해서 조합해야 한다(하나의 응답으로 합쳐서 제공하지 않는다).")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
	})
	ResponseEntity<ApiResponse<UserMeResponse>> me(
			@AuthenticationPrincipal(expression = "userId") Long userId);

	@Operation(summary = "이름 변경",
			description = "로그인한 사용자의 이름을 변경한다. 공백일 수 없고 최대 20자까지 허용된다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이름이 비어있거나 20자를 초과함"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
	})
	ResponseEntity<ApiResponse<UserMeResponse>> updateName(
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@Valid @RequestBody UserNameUpdateRequest request);
}
