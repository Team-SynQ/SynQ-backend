package com.synq.backend.domain.transcript.controller;

import com.synq.backend.domain.transcript.dto.TranscriptSegmentUpdateRequest;
import com.synq.backend.domain.transcript.dto.TranscriptSegmentUpdateResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Transcript", description = "전사 API")
public interface TranscriptSegmentControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "전사 세그먼트 수정", description = """
			사용자가 STT 결과 텍스트의 오타/오인식을 직접 교정한다. 회의에 남아있는 참가자면 누구나 수정할 수 있다(호스트 제한 없음).
			start_ms/end_ms/sequenceIndex 는 발화 순서 보장을 위해 바뀌지 않는다.
			회의가 진행 중(IN_PROGRESS)이 아니면 더 이상 수정할 수 없다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전사 세그먼트 수정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참가자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의 또는 세그먼트"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "진행 중이 아닌 회의의 전사 수정 시도")
	})
	@PatchMapping("/transcript-segments/{segmentId}")
	ResponseEntity<ApiResponse<TranscriptSegmentUpdateResponse>> update(
			@PathVariable Long meetingId,
			@PathVariable Long segmentId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@Valid @RequestBody TranscriptSegmentUpdateRequest request
	);
}
