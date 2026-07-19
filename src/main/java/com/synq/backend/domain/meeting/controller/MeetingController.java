package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingCreateRequest;
import com.synq.backend.domain.meeting.dto.MeetingCreateResponse;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.service.MeetingService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects/{projectId}/meetings")
@RequiredArgsConstructor
public class MeetingController implements MeetingControllerDocs {

	private final MeetingService meetingService;

	@Override
	public ResponseEntity<ApiResponse<MeetingCreateResponse>> create(Long projectId, Long userId, MeetingCreateRequest request) {
		Meeting meeting = meetingService.create(projectId, userId, request.consentAgreed());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, MeetingCreateResponse.from(meeting)));
	}
}
