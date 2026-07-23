package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.project.dto.ProjectCreateRequest;
import com.synq.backend.domain.project.dto.ProjectCreateResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequest;
import com.synq.backend.domain.project.dto.ProjectJoinResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
}
