package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.user.dto.RoleProfileRequest;
import com.synq.backend.domain.user.dto.RoleProfileResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "User", description = "유저")
@SecurityRequirement(name = "bearerAuth")
public interface RoleProfileControllerDocs {

	@Operation(summary = "역할·관점 프로필 목록 조회",
			description = "로그인한 사용자가 가진 역할·관점 프로필 목록을 등록순으로 조회한다. "
					+ "프로필이 존재하는 경우 그중 하나는 기본(isDefault=true) 프로필이다. 아직 온보딩을 완료하지 않은 사용자는 빈 목록을 받을 수 있다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
	})
	@GetMapping
	ResponseEntity<ApiResponse<List<RoleProfileResponse>>> getMyRoleProfiles(
			@AuthenticationPrincipal(expression = "userId") Long userId);

	@Operation(summary = "역할·관점 프로필 추가",
			description = "역할(role, 8종 중 1개 필수), 세부 역할(detailRole, role이 'ETC'일 때만 필수), "
					+ "관심 관점(perspectives, 최대 3개)으로 새 프로필을 추가한다. "
					+ "이 유저의 첫 프로필이면 자동으로 기본(isDefault=true) 프로필이 된다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "추가 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "role이 'ETC'인데 detailRole이 없거나, perspectives가 3개를 초과함"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
					description = "동시 요청 충돌 - 잠시 후 다시 시도")
	})
	@PostMapping
	ResponseEntity<ApiResponse<RoleProfileResponse>> create(
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@Valid @RequestBody RoleProfileRequest request);

	@Operation(summary = "역할·관점 프로필 수정",
			description = "기존 프로필의 role/detailRole/perspectives를 통째로 교체한다. 본인 소유 프로필만 수정 가능하다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "role이 'ETC'인데 detailRole이 없거나, perspectives가 3개를 초과함"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
					description = "본인 소유가 아니거나 존재하지 않는 프로필")
	})
	@PatchMapping("/{profileId}")
	ResponseEntity<ApiResponse<RoleProfileResponse>> update(
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@PathVariable Long profileId,
			@Valid @RequestBody RoleProfileRequest request);

	@Operation(summary = "역할·관점 프로필 삭제",
			description = "본인 소유 프로필을 삭제한다. 기본(isDefault=true) 프로필은 삭제할 수 없다 - "
					+ "다른 프로필을 먼저 기본으로 설정한 후 삭제해야 한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "기본 프로필은 삭제할 수 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
					description = "본인 소유가 아니거나 존재하지 않는 프로필")
	})
	@DeleteMapping("/{profileId}")
	ResponseEntity<ApiResponse<Void>> delete(
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@PathVariable Long profileId);

	@Operation(summary = "역할·관점 프로필을 기본으로 설정",
			description = "지정한 프로필을 기본(isDefault=true)으로 바꾼다. 기존에 기본이었던 다른 프로필은 자동으로 해제된다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
					description = "본인 소유가 아니거나 존재하지 않는 프로필"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
					description = "동시 요청 충돌 - 잠시 후 다시 시도")
	})
	@PatchMapping("/{profileId}/default")
	ResponseEntity<ApiResponse<Void>> setDefault(
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@PathVariable Long profileId);
}
