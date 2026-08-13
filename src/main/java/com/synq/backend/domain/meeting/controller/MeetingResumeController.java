package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingResumeResponse;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.service.MeetingService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 종료/삭제 등 회의 단건 대상 동작은 프로젝트에 종속되지 않는 flat 경로(/meetings/{meetingId})를 쓴다.
@RestController
@RequestMapping("/meetings/{meetingId}")
@RequiredArgsConstructor
public class MeetingResumeController implements MeetingResumeControllerDocs {

	private final MeetingService meetingService;

	@Override
	public ResponseEntity<ApiResponse<MeetingResumeResponse>> resume(Long meetingId, Long userId) {
		Meeting meeting = meetingService.resume(meetingId, userId);
		return ResponseEntity.ok(
				ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, MeetingResumeResponse.from(meeting)));
	}
}
