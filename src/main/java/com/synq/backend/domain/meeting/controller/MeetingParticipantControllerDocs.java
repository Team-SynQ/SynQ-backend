package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingParticipantResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Meeting", description = "미팅 API")
public interface MeetingParticipantControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회의 참여자 목록 조회", description = """
			현재 회의에 활성 상태로 참여 중인(나가지 않은) 참여자 목록을 반환한다. 진행자는 role=HOST,
			그 외는 role=MEMBER로 구분된다. 프로젝트 멤버 목록(GET /projects/{projectId}/members)과는
			별개로, 실제로 이 회의에 입장한 사람만 대상이다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여자 목록 조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "현재 회의 참여자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의")
	})
	@GetMapping("/participants")
	ResponseEntity<ApiResponse<List<MeetingParticipantResponse>>> findParticipants(
			@PathVariable Long meetingId,
			@AuthenticationPrincipal(expression = "userId") Long userId
	);
}
