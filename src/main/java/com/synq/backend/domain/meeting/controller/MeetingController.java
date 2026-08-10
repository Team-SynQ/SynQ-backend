package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingCreateRequest;
import com.synq.backend.domain.meeting.dto.MeetingCreateResponse;
import com.synq.backend.domain.meeting.dto.MeetingListResponse;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.service.MeetingService;
import com.synq.backend.domain.meeting.web.MeetingWsUrlResolver;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
	private final MeetingWsUrlResolver wsUrlResolver;

	@Override
	public ResponseEntity<ApiResponse<MeetingCreateResponse>> create(
			Long projectId, Long userId, MeetingCreateRequest request, HttpServletRequest servletRequest) {
		Meeting meeting = meetingService.create(projectId, userId, request.consentAgreed());
		String wsUrl = wsUrlResolver.resolve(servletRequest, meeting.getId());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, MeetingCreateResponse.from(meeting, wsUrl)));
	}

	@Override
	public ResponseEntity<ApiResponse<List<MeetingListResponse>>> findAll(Long projectId, Long userId) {
		List<MeetingListResponse> meetings = meetingService.findAll(projectId, userId);
		return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, meetings));
	}
}
