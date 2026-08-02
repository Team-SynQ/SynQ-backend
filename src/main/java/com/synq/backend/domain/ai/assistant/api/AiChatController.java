package com.synq.backend.domain.ai.assistant.api;

import com.synq.backend.domain.ai.assistant.api.dto.AiChatHistoryResponse;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatMessageResponse;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatSendRequest;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatWelcomeResponse;
import com.synq.backend.domain.ai.assistant.application.AiChatService;
import com.synq.backend.domain.auth.jwt.CurrentUserIdResolver;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/meetings/{meetingId}/chat-messages")
public class AiChatController implements AiChatControllerDocs {

	private final AiChatService aiChatService;
	private final CurrentUserIdResolver currentUserIdResolver;

	public AiChatController(
			AiChatService aiChatService,
			CurrentUserIdResolver currentUserIdResolver
	) {
		this.aiChatService = aiChatService;
		this.currentUserIdResolver = currentUserIdResolver;
	}

	@Override
	public ApiResponse<AiChatWelcomeResponse> getWelcome(Long meetingId, String authorization) {
		Long userId = currentUserIdResolver.resolve(authorization);
		return ApiResponse.onSuccess(
				GeneralSuccessCode.REQUEST_OK,
				AiChatWelcomeResponse.from(aiChatService.getWelcome(meetingId, userId))
		);
	}

	@Override
	public ResponseEntity<ApiResponse<AiChatMessageResponse>> send(
			Long meetingId,
			String authorization,
			AiChatSendRequest request
	) {
		Long userId = currentUserIdResolver.resolve(authorization);
		var result = aiChatService.send(
				meetingId,
				userId,
				request.question(),
				request.linkedSegmentId(),
				request.clientRequestId()
		);
		GeneralSuccessCode successCode = result.created()
				? GeneralSuccessCode.CREATED
				: GeneralSuccessCode.REQUEST_OK;
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status)
				.body(ApiResponse.onSuccess(successCode, AiChatMessageResponse.from(result.message())));
	}

	@Override
	public ApiResponse<AiChatHistoryResponse> getHistory(
			Long meetingId,
			String authorization,
			int page,
			int size
	) {
		Long userId = currentUserIdResolver.resolve(authorization);
		return ApiResponse.onSuccess(
				GeneralSuccessCode.REQUEST_OK,
				AiChatHistoryResponse.from(aiChatService.getHistory(meetingId, userId, page, size))
		);
	}
}
