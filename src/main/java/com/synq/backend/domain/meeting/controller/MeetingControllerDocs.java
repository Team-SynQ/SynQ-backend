package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingCreateRequest;
import com.synq.backend.domain.meeting.dto.MeetingCreateResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Meeting", description = "미팅 API")
public interface MeetingControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "미팅 생성", description = "생성과 동시에 녹음이 시작되며(IN_PROGRESS), 참여 동의(consentAgreed)가 필요하다.")
	@ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "미팅 생성 성공"))
	@PostMapping
	ResponseEntity<ApiResponse<MeetingCreateResponse>> create(
			@PathVariable Long projectId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@RequestBody MeetingCreateRequest request
	);
}
