package com.synq.backend.domain.ai.event.api;

import com.synq.backend.domain.ai.event.AiEventPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI Event", description = "회의 AI 결과 SSE 구독 API")
@SecurityRequirement(name = "bearerAuth")
public interface AiEventControllerDocs {

	@Operation(
			summary = "회의 AI 결과 구독",
			description = """
				Live Context, 자동 3-hint, 회의 요약 처리 결과를 SSE로 구독합니다.
				이벤트 이름은 connected, heartbeat, live-context.updated, hint.auto-created,
				summary.completed, summary.failed이며, 각 이벤트 data는 type, meetingId, occurredAt, data 필드를 포함합니다.
				hint.auto-created는 개인화된 결과이므로 힌트 대상 사용자의 연결에만 전송됩니다."""
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "SSE 연결 성공",
					content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
							schema = @Schema(implementation = AiEventPayload.class),
							examples = {
								@ExampleObject(
										name = "SSE 연결",
										value = """
											{"type":"CONNECTED","meetingId":1,"occurredAt":"2026-08-13T10:00:00Z","data":{"status":"connected"}}"""
								),
								@ExampleObject(
										name = "연결 유지",
										value = """
											{"type":"HEARTBEAT","meetingId":1,"occurredAt":"2026-08-13T10:00:10Z","data":{"status":"alive"}}"""
								),
								@ExampleObject(
										name = "Live Context 갱신",
										value = """
											{
											  "type": "LIVE_CONTEXT_UPDATED",
											  "meetingId": 1,
											  "occurredAt": "2026-08-13T10:01:00Z",
											  "data": {
											    "meetingId": 1,
											    "rollingSummary": "배포 일정과 QA 일정을 조율 중입니다.",
											    "currentTopic": "배포 일정",
											    "decisions": [],
											    "actionItems": ["QA 기간 확인"],
											    "openQuestions": ["출시일을 조정할 것인가?"],
											    "lastSegmentId": 25,
											    "lastSequenceIndex": 24
											  }
											}"""
								),
								@ExampleObject(
										name = "자동 3-hint 생성",
										value = """
											{
											  "type": "AUTO_HINT_CREATED",
											  "meetingId": 1,
											  "occurredAt": "2026-08-13T10:01:02Z",
											  "data": {
											    "meetingId": 1,
											    "segmentId": 25,
											    "meaning": "배포 일정이 재조정될 수 있다는 의미입니다.",
											    "myImpact": "담당 일정을 다시 확인해야 합니다.",
											    "teamQuestion": "변경된 일정을 언제 확정할까요?",
											    "importance": 85,
											    "triggerReason": "배포 일정 변경 가능성"
											  }
											}"""
								),
								@ExampleObject(
										name = "회의 요약 완료",
										value = """
											{"type":"SUMMARY_COMPLETED","meetingId":1,"occurredAt":"2026-08-13T11:00:00Z","data":{"jobId":"550e8400-e29b-41d4-a716-446655440000"}}"""
								),
								@ExampleObject(
										name = "회의 요약 실패",
										value = """
											{"type":"SUMMARY_FAILED","meetingId":1,"occurredAt":"2026-08-13T11:00:00Z","data":{"jobId":"550e8400-e29b-41d4-a716-446655440000","reason":"AI 요약 생성 실패"}}"""
								)
							})
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참여자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의")
	})
	@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	ResponseEntity<SseEmitter> subscribe(
			@PathVariable @Positive Long meetingId,
			@Parameter(hidden = true)
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
	);
}
