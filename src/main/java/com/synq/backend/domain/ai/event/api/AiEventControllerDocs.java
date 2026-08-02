package com.synq.backend.domain.ai.event.api;

import com.synq.backend.domain.ai.event.AiEventPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
			description = "Live Context와 회의 요약 처리 결과를 SSE로 구독합니다. 이벤트 이름은 "
					+ "connected, heartbeat, live-context.updated, summary.completed, summary.failed이며, "
					+ "각 이벤트 data는 type, meetingId, occurredAt, data 필드를 포함합니다."
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "SSE 연결 성공",
					content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
							schema = @Schema(implementation = AiEventPayload.class))
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
