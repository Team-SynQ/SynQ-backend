package com.synq.backend.domain.ai.assistant.controller;

import com.synq.backend.domain.ai.assistant.dto.HintRecordListResponse;
import com.synq.backend.domain.ai.assistant.dto.HintResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Assistant", description = "회의 중 AI 어시스턴트 API")
@SecurityRequirement(name = "bearerAuth")
public interface HintControllerDocs {

	@Operation(summary = "3-hint 생성", description = """
			사용자가 선택한 세그먼트에 대해 의미/내 영향/팀 질문 3종 힌트를 생성하고 저장하는 수동 생성 API입니다.
			같은 사용자가 같은 세그먼트를 다시 요청하면 새로 생성해 이전 결과를 덮어씁니다.
			중요 전사의 자동 힌트는 AI 이벤트 구독 API의 hint.auto-created 이벤트로 받고,
			저장된 자동/수동 힌트는 내 3-hint 기록 조회 API에서 함께 조회할 수 있습니다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "3-hint 생성 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "회의와 세그먼트 연결이 올바르지 않음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "현재 회의 참여자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의 또는 세그먼트"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 힌트 생성 또는 응답 해석 실패")
	})
	@PostMapping("/segments/{segmentId}/hints")
	ResponseEntity<ApiResponse<HintResponse>> generate(
			@PathVariable @Positive Long meetingId,
			@PathVariable @Positive Long segmentId,
			@Parameter(hidden = true)
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
	);

	@Operation(summary = "내 3-hint 기록 조회", description = """
			회의 기록 화면에서 쓴다. 내가 이 회의에서 생성했던 3-hint 를 세그먼트별로 반환한다.
			중요 전사는 자동으로 생성될 수 있으며, source=AUTO 와 중요도·판단 근거를 함께 반환한다.
			힌트는 사용자의 역할·관점에 따라 내용이 다르므로 본인 것만 조회된다.
			같은 세그먼트를 여러 번 눌렀다면 마지막 결과 하나만 있다.
			프론트는 segmentId 로 전사 목록과 머지한다. 회의 종료 후에도 조회할 수 있다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "힌트 기록 조회 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "회의 참여자가 아님"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의")
	})
	@GetMapping("/hints")
	ResponseEntity<ApiResponse<HintRecordListResponse>> getMyHints(
			@PathVariable @Positive Long meetingId,
			@Parameter(hidden = true)
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
	);
}
