package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingJoinResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "Meeting", description = "미팅 API")
public interface MeetingJoinControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회의 참여", description = """
			프로젝트 멤버가 진행 중인 회의에 참여자(MEMBER)로 등록된다. 이미 참여 중이면 에러 없이
			기존 참여 정보를 그대로 반환한다(멱등). HOST는 회의 생성 시에만 부여되며 이 API로는 부여되지 않는다.
			wsUrl로 연결하면 오디오 송신 없이 실시간 전사(TRANSCRIPT)를 받을 수 있다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회의 참여 성공(또는 이미 참여 중)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로젝트 멤버가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 종료된 회의")
	})
	@PostMapping("/join")
	ResponseEntity<ApiResponse<MeetingJoinResponse>> join(
			@PathVariable Long meetingId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			HttpServletRequest servletRequest
	);
}
