package com.synq.backend.domain.transcript.controller;

import com.synq.backend.domain.transcript.dto.MeetingRecordingSegmentResponse;
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

@Tag(name = "Transcript", description = "전사 API")
public interface MeetingRecordingControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회의 녹음 목록 조회", description = """
			회의에 남아있는 참가자면 누구나 조회할 수 있다(호스트 제한 없음). 회의 진행 상태와 무관하게 조회할 수 있다.
			세그먼트는 생성 순서(=재생 순서)대로 반환되며, 각 url은 짧은 TTL(15분)의 presigned URL이라 그 안에서만
			재생 가능하다. 호스트가 회의 중 재연결한 적이 있으면 세그먼트가 여러 개일 수 있고, 그때는 순서대로 이어
			재생하면 된다. 아직 업로드된 세그먼트가 없으면 빈 배열을 반환한다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참가자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의")
	})
	@GetMapping("/recordings")
	ResponseEntity<ApiResponse<List<MeetingRecordingSegmentResponse>>> findRecordings(
			@PathVariable Long meetingId,
			@AuthenticationPrincipal(expression = "userId") Long userId
	);
}
