package com.synq.backend.domain.reference.controller;

import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Reference", description = "참고자료 API")
public interface ReferenceControllerDocs {

	@Operation(summary = "참고자료 목록 조회", description = "프로젝트 멤버가 파일과 링크 참고자료 목록을 조회한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참고자료 목록 조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자 없음")
	})
	@GetMapping
	ResponseEntity<ApiResponse<ReferenceListResponse>> findAll(
			@PathVariable Long projectId,
			@RequestHeader(value = "X-User-Id", required = false) Long userId
	);
}
