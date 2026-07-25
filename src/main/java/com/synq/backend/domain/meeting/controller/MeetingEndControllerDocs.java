package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingEndResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Meeting", description = "미팅 API")
public interface MeetingEndControllerDocs {

	@Operation(summary = "회의 종료", description = """
			진행자(회의 HOST)만 회의를 종료할 수 있다. 종료 즉시 상태가 SUMMARIZING 으로 전환되고,
			이후 AI 정리(개인별/전체)가 비동기로 생성된다. 정리 실패 시 SUMMARY_FAILED 로 전이된다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회의 종료 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "종료 권한 없음(진행자 아님)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 종료된 회의")
	})
	@PostMapping("/end")
	ResponseEntity<ApiResponse<MeetingEndResponse>> end(
			@PathVariable Long meetingId,
			// TODO: AUTH 도메인 완성되면 SecurityContext 기반 현재 유저 추출로 교체한다.
			@RequestHeader("X-User-Id") Long userId
	);
}
