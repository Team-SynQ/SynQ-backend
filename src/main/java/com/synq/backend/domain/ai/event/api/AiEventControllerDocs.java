package com.synq.backend.domain.ai.event.api;

import io.swagger.v3.oas.annotations.Operation;
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
public interface AiEventControllerDocs {

	@Operation(
			summary = "회의 AI 결과 구독",
			description = "Live Context와 회의 요약 처리 결과를 SSE로 구독합니다. Authorization 헤더가 필요하므로 "
					+ "브라우저에서는 헤더를 지원하는 SSE 클라이언트를 사용합니다."
	)
	@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	ResponseEntity<SseEmitter> subscribe(
			@PathVariable @Positive Long meetingId,
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
	);
}
