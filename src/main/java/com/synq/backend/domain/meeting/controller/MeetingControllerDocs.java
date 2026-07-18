package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingCreateRequest;
import com.synq.backend.domain.meeting.dto.MeetingCreateResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Meeting", description = "미팅 API")
public interface MeetingControllerDocs {

	@Operation(summary = "미팅 생성", description = "생성과 동시에 녹음이 시작되며(IN_PROGRESS), 참여 동의(consentAgreed)가 필요하다.")
	@PostMapping
	ResponseEntity<ApiResponse<MeetingCreateResponse>> create(
			@PathVariable Long projectId,
			// TODO: AUTH 도메인 완성되면 SecurityContext 기반 현재 유저 추출로 교체한다.
			@RequestHeader("X-User-Id") Long userId,
			@RequestBody MeetingCreateRequest request
	);
}
