package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.auth.jwt.UserAuthDto;
import com.synq.backend.domain.project.dto.ProjectCreateRequest;
import com.synq.backend.domain.project.dto.ProjectCreateResponse;
import com.synq.backend.domain.project.dto.ProjectDetailResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationInfoResponse;
import com.synq.backend.domain.project.dto.ProjectInvitationResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestCreateRequest;
import com.synq.backend.domain.project.dto.ProjectJoinRequestCreateResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestApproveResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestListResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestRejectResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequestResultResponse;
import com.synq.backend.domain.project.dto.ProjectJoinRequest;
import com.synq.backend.domain.project.dto.ProjectJoinResponse;
import com.synq.backend.domain.project.dto.ProjectListResponse;
import com.synq.backend.domain.project.dto.ProjectMemberListResponse;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveResponse;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveUpdateRequest;
import com.synq.backend.domain.project.dto.ProjectRolePerspectiveUpdateResponse;
import com.synq.backend.domain.project.dto.ProjectUpdateRequest;
import com.synq.backend.domain.project.dto.ProjectUpdateResponse;
import com.synq.backend.domain.project.service.ProjectService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController implements ProjectControllerDocs {

	private final ProjectService projectService;

	@Override
	public ResponseEntity<Void> leave(Long projectId, Long userId) {
		projectService.leave(projectId, userId);
		return ResponseEntity.ok().build();
	}

	@Override
	public ResponseEntity<Void> deleteMember(Long projectId, Long memberId, Long userId) {
		projectService.deleteMember(projectId, memberId, userId);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> delete(Long projectId, Long userId) {
		projectService.delete(projectId, userId);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectMemberListResponse>> findMembers(Long projectId, Long userId) {
		ProjectMemberListResponse response = projectService.findMembers(projectId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectRolePerspectiveResponse>> findRolePerspective(
			Long projectId,
			Long userId
	) {
		ProjectRolePerspectiveResponse response = projectService.findRolePerspective(projectId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectRolePerspectiveUpdateResponse>> updateRolePerspective(
			Long projectId,
			Long userId,
			ProjectRolePerspectiveUpdateRequest request
	) {
		ProjectRolePerspectiveUpdateResponse response = projectService.updateRolePerspective(
				projectId,
				userId,
				request
		);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectUpdateResponse>> update(
			Long projectId,
			Long userId,
			ProjectUpdateRequest request
	) {
		ProjectUpdateResponse response = projectService.update(projectId, userId, request);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectDetailResponse>> findById(Long projectId, Long userId) {
		ProjectDetailResponse response = projectService.findById(projectId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<List<ProjectListResponse>>> findAll(Long userId) {
		List<ProjectListResponse> response = projectService.findAll(userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectCreateResponse>> create(Long userId, ProjectCreateRequest request) {
		ProjectCreateResponse response = projectService.create(userId, request);
		return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectJoinResponse>> join(Long userId, ProjectJoinRequest request) {
		ProjectJoinResponse response = projectService.join(userId, request.inviteToken());
		GeneralSuccessCode successCode = response.newlyJoined()
				? GeneralSuccessCode.CREATED
				: GeneralSuccessCode.REQUEST_OK;
		return ResponseEntity.status(successCode.getStatus())
				.body(ApiResponse.onSuccess(successCode, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectJoinRequestCreateResponse>> createJoinRequest(
			Long projectId,
			Long userId,
			ProjectJoinRequestCreateRequest request
	) {
		ProjectJoinRequestCreateResponse response = projectService.createJoinRequest(projectId, userId, request);
		return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectJoinRequestListResponse>> findPendingJoinRequests(
			Long projectId,
			Long userId
	) {
		ProjectJoinRequestListResponse response = projectService.findPendingJoinRequests(projectId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<List<ProjectJoinRequestResultResponse>>> findMyJoinRequestResults(Long userId) {
		List<ProjectJoinRequestResultResponse> response = projectService.findMyJoinRequestResults(userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectJoinRequestApproveResponse>> approveJoinRequest(
			Long projectId,
			Long requestId,
			Long userId
	) {
		ProjectJoinRequestApproveResponse response = projectService.approveJoinRequest(
				projectId, requestId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectJoinRequestRejectResponse>> rejectJoinRequest(
			Long projectId,
			Long requestId,
			Long userId
	) {
		ProjectJoinRequestRejectResponse response = projectService.rejectJoinRequest(
				projectId, requestId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectInvitationResponse>> createInvitation(Long projectId, Long userId) {
		ProjectInvitationResponse response = projectService.createInvitation(projectId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ProjectInvitationInfoResponse>> findInvitationInfo(
			String inviteToken,
			UserAuthDto principal
	) {
		Long userId = principal == null ? null : principal.getUserId();
		ProjectInvitationInfoResponse response = projectService.findInvitationInfo(inviteToken, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
