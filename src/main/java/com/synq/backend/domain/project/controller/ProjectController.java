package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.project.dto.ProjectCreateRequest;
import com.synq.backend.domain.project.dto.ProjectCreateResponse;
import com.synq.backend.domain.project.service.ProjectService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController implements ProjectControllerDocs {

	private final ProjectService projectService;

	@Override
	public ResponseEntity<ApiResponse<ProjectCreateResponse>> create(Long userId, ProjectCreateRequest request) {
		ProjectCreateResponse response = projectService.create(userId, request);
		return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response));
	}
}
