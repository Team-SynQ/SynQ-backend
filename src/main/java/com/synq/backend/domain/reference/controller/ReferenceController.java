package com.synq.backend.domain.reference.controller;

import com.synq.backend.domain.reference.dto.ReferenceFileCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateRequest;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceNameUpdateRequest;
import com.synq.backend.domain.reference.dto.ReferenceNameUpdateResponse;
import com.synq.backend.domain.reference.service.ReferenceService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/references")
@RequiredArgsConstructor
public class ReferenceController implements ReferenceControllerDocs {

	private final ReferenceService referenceService;

	@Override
	public ResponseEntity<ApiResponse<ReferenceFileCreateResponse>> createFiles(
			Long projectId,
			Long userId,
			List<MultipartFile> files
	) {
		ReferenceFileCreateResponse response = referenceService.createFiles(projectId, userId, files);
		return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response));
	}

	@Override
	public ResponseEntity<Void> delete(Long projectId, Long referenceId, Long userId) {
		referenceService.delete(projectId, referenceId, userId);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<ApiResponse<ReferenceNameUpdateResponse>> updateName(
			Long projectId,
			Long referenceId,
			Long userId,
			ReferenceNameUpdateRequest request
	) {
		ReferenceNameUpdateResponse response = referenceService.updateName(
				projectId, referenceId, userId, request);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ReferenceLinkCreateResponse>> createLink(
			Long projectId,
			Long userId,
			ReferenceLinkCreateRequest request
	) {
		ReferenceLinkCreateResponse response = referenceService.createLink(projectId, userId, request);
		return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, response));
	}

	@Override
	public ResponseEntity<ApiResponse<ReferenceListResponse>> findAll(Long projectId, Long userId) {
		ReferenceListResponse response = referenceService.findAll(projectId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
