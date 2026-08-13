package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingPauseResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "Meeting", description = "미팅 API")
public interface MeetingPauseControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회의 일시정지", description = """
			진행자(회의 HOST)만 회의를 일시정지할 수 있다. 일시정지 시점의 누적 활성 시간(activeSeconds)이
			참여자에게 전사 WebSocket의 MEETING_PAUSED 메시지로 브로드캐스트된다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일시정지 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "일시정지 권한 없음(진행자 아님)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 종료됐거나 이미 일시정지된 회의")
	})
	@PostMapping("/pause")
	ResponseEntity<ApiResponse<MeetingPauseResponse>> pause(
			@PathVariable Long meetingId,
			@AuthenticationPrincipal(expression = "userId") Long userId
	);
}
