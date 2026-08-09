package com.synq.backend.domain.ai.assistant.controller;

import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.domain.SegmentHint;
import com.synq.backend.domain.ai.assistant.dto.HintRecordListResponse;
import com.synq.backend.domain.ai.assistant.dto.HintResponse;
import com.synq.backend.domain.ai.assistant.service.HintService;
import com.synq.backend.domain.auth.jwt.CurrentUserIdResolver;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meetings/{meetingId}")
@RequiredArgsConstructor
public class HintController implements HintControllerDocs {

	private final HintService hintService;
	private final CurrentUserIdResolver currentUserIdResolver;

	@Override
	public ResponseEntity<ApiResponse<HintResponse>> generate(Long meetingId, Long segmentId, String authorization) {
		Long userId = currentUserIdResolver.resolve(authorization);
		HintResult result = hintService.generate(userId, meetingId, segmentId);
		return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, HintResponse.from(result)));
	}

	@Override
	public ResponseEntity<ApiResponse<HintRecordListResponse>> getMyHints(Long meetingId, String authorization) {
		Long userId = currentUserIdResolver.resolve(authorization);
		List<SegmentHint> hints = hintService.getMyHints(userId, meetingId);
		return ResponseEntity.ok(ApiResponse.onSuccess(
				GeneralSuccessCode.REQUEST_OK, HintRecordListResponse.from(meetingId, hints)));
	}
}
