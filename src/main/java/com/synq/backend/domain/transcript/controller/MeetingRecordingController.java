package com.synq.backend.domain.transcript.controller;

import com.synq.backend.domain.transcript.dto.MeetingRecordingSegmentResponse;
import com.synq.backend.domain.transcript.service.MeetingRecordingQueryService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/meetings/{meetingId}")
@RequiredArgsConstructor
public class MeetingRecordingController implements MeetingRecordingControllerDocs {

	private final MeetingRecordingQueryService meetingRecordingQueryService;

	@Override
	public ResponseEntity<ApiResponse<List<MeetingRecordingSegmentResponse>>> findRecordings(
			Long meetingId, Long userId) {
		List<MeetingRecordingSegmentResponse> recordings =
				meetingRecordingQueryService.findRecordings(meetingId, userId);
		return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, recordings));
	}
}
