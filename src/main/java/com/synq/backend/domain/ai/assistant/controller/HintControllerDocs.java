package com.synq.backend.domain.ai.assistant.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Assistant", description = "회의 중 AI 어시스턴트 API")
@SecurityRequirement(name = "bearerAuth")
public interface HintControllerDocs {

	@Operation(summary = "3-hint 생성", description = "클릭한 세그먼트에 대해 의미/내 영향/팀 질문 3종 힌트를 생성한다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "3-hint 생성 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "회의와 세그먼트 연결이 올바르지 않음"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의 또는 세그먼트")
	})
	@PostMapping("/hints")
	ResponseEntity<ApiResponse<HintResponse>> generate(
			@PathVariable @Positive Long meetingId,
			@PathVariable @Positive Long segmentId,
			@Parameter(hidden = true)
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
	);
}
