package com.synq.backend.domain.ai.assistant.controller;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.dto.HintResponse;
import com.synq.backend.domain.ai.assistant.service.HintService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HintController implements HintControllerDocs {

	private final HintService hintService;

	@Override
	public ResponseEntity<ApiResponse<HintResponse>> generate(Long meetingId, Long segmentId, Long userId) {
		HintResult result = hintService.generate(userId, meetingId, segmentId);
		return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, HintResponse.from(result)));
	}
}
