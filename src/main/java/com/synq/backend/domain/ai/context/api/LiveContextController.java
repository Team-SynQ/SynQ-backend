package com.synq.backend.domain.ai.context.api;

import com.synq.backend.domain.ai.context.api.dto.LiveContextResponse;
import com.synq.backend.domain.ai.context.application.LiveContextService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/meetings/{meetingId}/live-context")
@Tag(name = "Live Context", description = "회의 중 누적 AI 맥락 조회 API")
public class LiveContextController {

	private final LiveContextService liveContextService;

	public LiveContextController(LiveContextService liveContextService) {
		this.liveContextService = liveContextService;
	}

	@Operation(summary = "회의 중 최신 AI 맥락 조회")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "최신 AI 맥락 조회 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의 맥락")
	})
	@GetMapping
	public ApiResponse<LiveContextResponse> get(@PathVariable @Positive Long meetingId) {
		return ApiResponse.onSuccess(
				GeneralSuccessCode.REQUEST_OK,
				LiveContextResponse.from(liveContextService.get(meetingId))
		);
	}
}
