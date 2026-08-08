package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingTitleUpdateRequest;
import com.synq.backend.domain.meeting.dto.MeetingTitleUpdateResponse;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.service.MeetingService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 제목 수정 등 회의 단건 대상 동작은 프로젝트에 종속되지 않는 flat 경로(/meetings/{meetingId})를 쓴다.
@RestController
@RequestMapping("/meetings/{meetingId}")
@RequiredArgsConstructor
public class MeetingTitleController implements MeetingTitleControllerDocs {

	private final MeetingService meetingService;

	@Override
	public ResponseEntity<ApiResponse<MeetingTitleUpdateResponse>> updateTitle(
			Long meetingId, Long userId, MeetingTitleUpdateRequest request) {
		Meeting meeting = meetingService.updateTitle(meetingId, userId, request.title());
		return ResponseEntity.ok(
				ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, MeetingTitleUpdateResponse.from(meeting)));
	}
}
