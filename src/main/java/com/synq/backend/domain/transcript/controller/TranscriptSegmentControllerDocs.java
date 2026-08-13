package com.synq.backend.domain.transcript.controller;

import com.synq.backend.domain.transcript.dto.TranscriptSegmentListResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Transcript", description = """
		전사 API

		실시간 전사는 REST가 아닌 raw WebSocket으로 별도 제공되며, 이 Swagger 문서에서 직접 호출/테스트할 수 없다. \
		프로토콜은 다음과 같다.

		연결: `{wsScheme}://{host}/ws/meetings/{meetingId}/stt` (accessToken은 handshake 시 쿼리 파라미터로 전달, \
		역할(HOST/MEMBER)에 따라 동작이 갈린다)

		- HOST: 오디오(Binary WebSocket 프레임)를 서버로 전송하면 Soniox STT로 릴레이되어 실시간 전사가 이루어진다.
		- MEMBER(참여자): 오디오를 보내지 않고, 확정된 전사(TRANSCRIPT) 메시지만 수신한다.

		서버 → 클라이언트 메시지는 JSON이며 `type` 필드로 구분된다.

		- `TRANSCRIPT`: 확정 전사(isFinal=true, utteranceId/sequence 포함) 또는 중간 캡션(isFinal=false, 둘 다 비어있음)
		- `CONNECTION_STATUS`: 연결 상태 변경 알림
		- `TRANSCRIPT_INTERRUPTED`: 전사 중단 사유(reason) 알림
		- `MEETING_ENDED`: 회의 종료(정상 종료 또는 호스트 연결 끊김에 의한 강제 종료) 알림
		- `MEETING_PAUSED` / `MEETING_RESUMED`: 회의 일시정지/재개 알림 (activeSeconds: 시점의 누적 활성 시간(초))

		아래 REST API들은 WebSocket으로 확정된 전사를 사후에 조회/교정하는 용도다.""")
public interface TranscriptSegmentControllerDocs {

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "전사 세그먼트 수정", description = """
			사용자가 STT 결과 텍스트의 오타/오인식을 직접 교정한다. 회의에 남아있는 참여자면 누구나 수정할 수 있다(호스트 제한 없음).
			start_ms/end_ms/sequenceIndex 는 발화 순서 보장을 위해 바뀌지 않는다.
			회의가 진행 중(IN_PROGRESS)이 아니면 더 이상 수정할 수 없다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전사 세그먼트 수정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참여자가 아님"),
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

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "전사 세그먼트 목록 조회", description = """
			회의의 확정된 전사 세그먼트를 start_ms, sequenceIndex 순으로 조회한다. 회의에 남아있는 참여자면 누구나 조회할 수 있다(호스트 제한 없음).
			사용자가 교정한 세그먼트는 수정된 content 로 응답하며 isModified 로 구분할 수 있다. 회의 진행 상태와 무관하게 조회할 수 있다.
			afterSequenceIndex를 주면 그보다 큰 sequenceIndex의 세그먼트만 반환한다(폴링으로 실시간 전사를 따라갈 때, 이미 받은 구간을 다시 받지 않기 위한 증분 조회용).""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전사 세그먼트 목록 조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참여자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의")
	})
	@GetMapping("/transcript-segments")
	ResponseEntity<ApiResponse<TranscriptSegmentListResponse>> getSegments(
			@PathVariable Long meetingId,
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@RequestParam(required = false) Integer afterSequenceIndex
	);
}
