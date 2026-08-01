package com.synq.backend.domain.reference.controller;

import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.domain.reference.service.ReferenceService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/references")
@RequiredArgsConstructor
public class ReferenceController implements ReferenceControllerDocs {

	private final ReferenceService referenceService;

	@Override
	public ResponseEntity<ApiResponse<ReferenceListResponse>> findAll(Long projectId, Long userId) {
		ReferenceListResponse response = referenceService.findAll(projectId, userId);
		return ResponseEntity.status(GeneralSuccessCode.REQUEST_OK.getStatus())
				.body(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response));
	}
}
