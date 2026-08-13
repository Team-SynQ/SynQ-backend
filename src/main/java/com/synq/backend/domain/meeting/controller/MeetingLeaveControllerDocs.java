package com.synq.backend.domain.meeting.controller;

import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "Meeting", description = "회의 API")
public interface MeetingLeaveControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회의 나가기", description = """
			현재 활성 참여자만 회의를 나갈 수 있다. 호스트는 나갈 수 없으며, 회의 종료 또는 삭제를 이용해야 한다.
			나간 뒤 다시 참여하고 싶으면 참여 API를 다시 호출하면 된다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회의 나가기 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "참여자가 아니거나 호스트라 나갈 수 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의")
	})
	@PostMapping("/leave")
	ResponseEntity<ApiResponse<Void>> leave(
			@PathVariable Long meetingId,
			@AuthenticationPrincipal(expression = "userId") Long userId
	);
}
