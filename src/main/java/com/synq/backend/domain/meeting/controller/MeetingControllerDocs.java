package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingCreateRequest;
import com.synq.backend.domain.meeting.dto.MeetingCreateResponse;
import com.synq.backend.domain.meeting.dto.MeetingListResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Meeting", description = "미팅 API")
public interface MeetingControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "미팅 생성", description = "생성과 동시에 녹음이 시작되며(IN_PROGRESS), 참여 동의(consentAgreed)가 필요하다. "
			+ "wsUrl은 요청 scheme에 맞춰 ws:// 또는 wss:// 절대 URL로 내려간다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "미팅 생성 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "녹음 및 전사 참여 동의 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 멤버가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "프로젝트에 이미 진행 중인 회의가 있음")
	})
	@PostMapping
	ResponseEntity<ApiResponse<MeetingCreateResponse>> create(
			@PathVariable Long projectId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@RequestBody MeetingCreateRequest request,
			HttpServletRequest servletRequest
	);

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "미팅 목록 조회", description = "프로젝트의 회의 목록을 생성일 최신순으로 조회한다. "
			+ "keyTopics는 요약이 아직 생성되지 않았으면 null, 요약은 있으나 주제가 없으면 빈 배열이다. "
			+ "durationSeconds는 회의가 아직 진행 중이면 null이다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "미팅 목록 조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 멤버가 아님")
	})
	@GetMapping
	ResponseEntity<ApiResponse<List<MeetingListResponse>>> findAll(
			@PathVariable Long projectId,
			@AuthenticationPrincipal(expression = "userId") Long userId
	);
}
