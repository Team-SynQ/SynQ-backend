package com.synq.backend.domain.ai.assistant.controller;

import com.synq.backend.domain.ai.assistant.dto.HintResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Assistant", description = "회의 중 AI 어시스턴트 API")
public interface HintControllerDocs {

	@Operation(summary = "3-hint 생성", description = "클릭한 세그먼트에 대해 의미/내 영향/팀 질문 3종 힌트를 생성한다.")
	@PostMapping("/hints")
	ResponseEntity<ApiResponse<HintResponse>> generate(
			@PathVariable Long meetingId,
			@PathVariable Long segmentId,
			// TODO: AUTH 도메인 완성되면 SecurityContext 기반 현재 유저 추출로 교체한다.
			@RequestHeader("X-User-Id") Long userId
	);
}
