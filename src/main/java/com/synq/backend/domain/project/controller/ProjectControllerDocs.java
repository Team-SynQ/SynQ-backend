package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.project.dto.ProjectCreateRequest;
import com.synq.backend.domain.project.dto.ProjectCreateResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationInfoResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequest;
import com.synq.backend.domain.project.dto.ProjectJoinResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Project", description = "프로젝트 API")
public interface ProjectControllerDocs {

	@Operation(summary = "프로젝트 생성", description = "프로젝트를 생성하고 생성자를 OWNER로 등록한다.")
	@ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "프로젝트 생성 성공"))
	@PostMapping
	ResponseEntity<ApiResponse<ProjectCreateResponse>> create(
			@RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody ProjectCreateRequest request
	);

	@Operation(summary = "프로젝트 참여", description = "저장된 초대 토큰을 통해 프로젝트에 참여한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "프로젝트 참여 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미 참여 중인 프로젝트")
	})
	@PostMapping("/join")
	ResponseEntity<ApiResponse<ProjectJoinResponse>> join(
			@RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody ProjectJoinRequest request
	);

	@Operation(summary = "프로젝트 초대 링크 생성", description = "프로젝트 소유자가 초대 링크를 생성하거나 유효한 기존 링크를 조회한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "초대 링크 생성 또는 조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "초대 링크 생성 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 프로젝트를 찾을 수 없음")
	})
	@PostMapping("/{projectId}/invitation")
	ResponseEntity<ApiResponse<ProjectInvitationResponse>> createInvitation(
			@PathVariable Long projectId,
			@RequestHeader(value = "X-User-Id", required = false) Long userId
	);

	@Operation(summary = "프로젝트 초대 정보 조회", description = "초대 링크 접속 시 프로젝트 참여 전 확인에 필요한 정보를 조회한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 초대 정보 조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유효하지 않은 초대 링크"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "초대 링크 만료")
	})
	@GetMapping("/invitations/{inviteToken}")
	ResponseEntity<ApiResponse<ProjectInvitationInfoResponse>> findInvitationInfo(
			@PathVariable String inviteToken,
			@RequestHeader(value = "X-User-Id", required = false) Long userId
	);
}
