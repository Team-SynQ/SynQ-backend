package com.synq.backend.domain.ai.summary.api;

import com.synq.backend.domain.ai.summary.application.MeetingSummaryService;
import com.synq.backend.domain.ai.summary.api.dto.MeetingSummaryResponse;
import com.synq.backend.domain.ai.summary.api.dto.SummaryJobResponse;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/meetings/{meetingId}")
public class AiSummaryController {

	private final MeetingSummaryService meetingSummaryService;

	public AiSummaryController(MeetingSummaryService meetingSummaryService) {
		this.meetingSummaryService = meetingSummaryService;
	}

	@Operation(summary = "회의 종료 후 AI 요약 생성")
	@PostMapping("/ai-summary/generate")
	public ResponseEntity<ApiResponse<SummaryJobResponse>> generate(
			@PathVariable @Positive Long meetingId
	) {
		var job = meetingSummaryService.request(meetingId);
		// 결과 생성은 백그라운드에서 진행되므로 완료 응답 대신 요청 접수 상태를 반환한다.
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(ApiResponse.onSuccess(GeneralSuccessCode.ACCEPTED, SummaryJobResponse.from(job)));
	}

	@Operation(summary = "회의 종료 후 AI 요약 작업 상태 조회")
	@GetMapping("/ai-summary/status")
	public ApiResponse<SummaryJobResponse> getStatus(
			@PathVariable @Positive Long meetingId,
			@RequestParam UUID jobId
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK,
				SummaryJobResponse.from(meetingSummaryService.getJob(meetingId, jobId)));
	}

	@Operation(summary = "회의 종료 후 AI 요약 조회")
	@GetMapping("/summary")
	public ApiResponse<MeetingSummaryResponse> getSummary(@PathVariable @Positive Long meetingId) {
		return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK,
				MeetingSummaryResponse.from(meetingSummaryService.getLatestSummary(meetingId)));
	}
}
