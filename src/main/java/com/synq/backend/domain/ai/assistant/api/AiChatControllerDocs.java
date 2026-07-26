package com.synq.backend.domain.ai.assistant.api;

import com.synq.backend.domain.ai.assistant.api.dto.AiChatHistoryResponse;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatMessageResponse;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatSendRequest;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AI Chat", description = "회의 중 AI 채팅 API")
public interface AiChatControllerDocs {

	@Operation(summary = "AI 채팅 질문 전송")
	@PostMapping
	ResponseEntity<ApiResponse<AiChatMessageResponse>> send(
			@PathVariable @Positive Long meetingId,
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody @Valid AiChatSendRequest request
	);

	@Operation(summary = "내 AI 채팅 내역 조회")
	@GetMapping
	ApiResponse<AiChatHistoryResponse> getHistory(
			@PathVariable @Positive Long meetingId,
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	);
}
