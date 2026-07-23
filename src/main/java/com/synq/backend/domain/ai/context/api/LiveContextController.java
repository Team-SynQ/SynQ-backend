package com.synq.backend.domain.ai.context.api;

import com.synq.backend.domain.ai.context.api.dto.LiveContextResponse;
import com.synq.backend.domain.ai.context.application.LiveContextService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/meetings/{meetingId}/live-context")
public class LiveContextController {

	private final LiveContextService liveContextService;

	public LiveContextController(LiveContextService liveContextService) {
		this.liveContextService = liveContextService;
	}

	@Operation(summary = "회의 중 최신 AI 맥락 조회")
	@GetMapping
	public ApiResponse<LiveContextResponse> get(@PathVariable @Positive Long meetingId) {
		return ApiResponse.onSuccess(
				GeneralSuccessCode.REQUEST_OK,
				LiveContextResponse.from(liveContextService.get(meetingId))
		);
	}
}
