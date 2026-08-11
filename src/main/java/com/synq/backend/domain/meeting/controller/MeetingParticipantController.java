package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingParticipantResponse;
import com.synq.backend.domain.meeting.service.MeetingService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 조회도 다른 회의 단건 대상 동작과 마찬가지로 프로젝트에 종속되지 않는 flat 경로(/meetings/{meetingId})를 쓴다.
@RestController
@RequestMapping("/meetings/{meetingId}")
@RequiredArgsConstructor
public class MeetingParticipantController implements MeetingParticipantControllerDocs {

	private final MeetingService meetingService;

	@Override
	public ResponseEntity<ApiResponse<List<MeetingParticipantResponse>>> findParticipants(
			Long meetingId, Long userId) {
		List<MeetingParticipantResponse> participants = meetingService.findParticipants(meetingId, userId);
		return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, participants));
	}
}
