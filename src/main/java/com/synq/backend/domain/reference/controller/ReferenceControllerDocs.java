package com.synq.backend.domain.reference.controller;

import com.synq.backend.domain.reference.dto.ReferenceFileCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateRequest;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceNameUpdateRequest;
import com.synq.backend.domain.reference.dto.ReferenceNameUpdateResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Reference", description = "참고자료 API")
public interface ReferenceControllerDocs {

	@Operation(summary = "참고자료 파일 등록", description = "프로젝트 멤버가 파일 참고자료를 최대 5개 등록한다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "참고자료 파일 등록 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일 없음 또는 파일 개수 제한 초과"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "프로젝트 참고자료 개수 초과"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "파일 용량 제한 초과"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "지원하지 않는 파일 형식"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "참고자료 파일 업로드 실패")
	})
	@PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ResponseEntity<ApiResponse<ReferenceFileCreateResponse>> createFiles(
			@PathVariable Long projectId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@RequestPart(value = "files", required = false) List<MultipartFile> files
	);

	@Operation(summary = "참고자료 삭제", description = "프로젝트 소유자 또는 참고자료 등록자가 참고자료를 삭제한다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "참고자료 삭제 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 또는 참고자료 삭제 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자, 프로젝트 또는 참고자료 없음")
	})
	@DeleteMapping("/{referenceId}")
	ResponseEntity<Void> delete(
			@PathVariable Long projectId,
			@PathVariable Long referenceId,
			@AuthenticationPrincipal(expression = "userId") Long userId
	);

	@Operation(summary = "참고자료 제목 수정", description = "프로젝트 소유자 또는 참고자료 등록자가 참고자료 제목을 수정한다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참고자료 제목 수정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "참고자료 제목 입력값 오류"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 또는 참고자료 수정 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자, 프로젝트 또는 참고자료 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "참고자료 제목 수정 실패")
	})
	@PatchMapping("/{referenceId}")
	ResponseEntity<ApiResponse<ReferenceNameUpdateResponse>> updateName(
			@PathVariable Long projectId,
			@PathVariable Long referenceId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@Valid @RequestBody ReferenceNameUpdateRequest request
	);

	@Operation(summary = "참고자료 링크 등록", description = "프로젝트 멤버가 링크 참고자료 정보를 등록한다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "참고자료 링크 등록 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "URL 입력값 오류"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "프로젝트 참고자료 개수 초과")
	})
	@PostMapping("/links")
	ResponseEntity<ApiResponse<ReferenceLinkCreateResponse>> createLink(
			@PathVariable Long projectId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@Valid @RequestBody ReferenceLinkCreateRequest request
	);

	@Operation(summary = "참고자료 목록 조회", description = "프로젝트 멤버가 파일과 링크 참고자료 목록을 조회한다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참고자료 목록 조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자 없음")
	})
	@GetMapping
	ResponseEntity<ApiResponse<ReferenceListResponse>> findAll(
			@PathVariable Long projectId,
			@AuthenticationPrincipal(expression = "userId") Long userId
	);
}
