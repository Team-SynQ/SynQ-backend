package com.synq.backend.domain.meeting.controller;

import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Meeting", description = "회의 API")
public interface MeetingDeleteControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회의 삭제", description = """
			진행자(회의 HOST)만 회의를 삭제할 수 있다. 진행 중(IN_PROGRESS)인 회의는 삭제할 수 없으며,
			먼저 회의를 종료해야 한다. 참여자/전사/요약 등 회의에 속한 데이터는 함께 삭제된다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회의 삭제 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음(진행자 아님)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "진행 중인 회의는 삭제 불가")
	})
	@DeleteMapping
	ResponseEntity<ApiResponse<Void>> delete(
			@PathVariable Long meetingId,
			@AuthenticationPrincipal(expression = "userId") Long userId
	);
}
