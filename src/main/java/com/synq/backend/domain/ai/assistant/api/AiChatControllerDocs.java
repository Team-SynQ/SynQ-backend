package com.synq.backend.domain.ai.assistant.api;

import com.synq.backend.domain.ai.assistant.api.dto.AiChatHistoryResponse;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatMessageResponse;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatSendRequest;
import com.synq.backend.domain.ai.assistant.api.dto.AiChatWelcomeResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
public interface AiChatControllerDocs {

	@Operation(summary = "AI Chat 초기 안내 및 추천 질문 조회")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "초기 안내 및 추천 질문 조회 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참여자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "진행 중이 아닌 회의"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 안내 생성 실패")
	})
	@GetMapping("/suggestions")
	ApiResponse<AiChatWelcomeResponse> getWelcome(
			@PathVariable @Positive Long meetingId,
			@Parameter(hidden = true)
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
	);

	@Operation(summary = "AI 채팅 질문 전송")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "동일한 요청의 기존 답변 반환", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "AI 답변 생성 및 저장 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "질문 또는 요청값 오류"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참여자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "진행 중이 아닌 회의 또는 clientRequestId 재사용"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "선택 발화 연동을 사용할 수 없음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 답변 생성 실패")
	})
	@PostMapping
	ResponseEntity<ApiResponse<AiChatMessageResponse>> send(
			@PathVariable @Positive Long meetingId,
			@Parameter(hidden = true)
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody @Valid AiChatSendRequest request
	);

	@Operation(summary = "내 AI 채팅 내역 조회")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI 채팅 내역 조회 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "페이지 요청값 오류"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참여자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의")
	})
	@GetMapping
	ApiResponse<AiChatHistoryResponse> getHistory(
			@PathVariable @Positive Long meetingId,
			@Parameter(hidden = true)
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	);
}
