package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingTitleUpdateRequest;
import com.synq.backend.domain.meeting.dto.MeetingTitleUpdateResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Meeting", description = "미팅 API")
public interface MeetingTitleControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회의 제목 수정", description = """
			진행자(회의 HOST)만 회의 제목을 수정할 수 있다. 회의 상태와 무관하게(진행 중/종료 후 모두) 수정할 수 있으며,
			수정 후에는 향후 AI 자동 제목 생성 로직이 이 회의의 제목을 덮어쓰지 않는다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제목 수정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "제목이 비어 있음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음(진행자 아님)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의")
	})
	@PatchMapping("/title")
	ResponseEntity<ApiResponse<MeetingTitleUpdateResponse>> updateTitle(
			@PathVariable Long meetingId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@RequestBody MeetingTitleUpdateRequest request
	);
}
