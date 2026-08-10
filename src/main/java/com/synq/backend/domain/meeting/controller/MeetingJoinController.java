package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.dto.MeetingJoinResponse;
import com.synq.backend.domain.meeting.service.MeetingJoinResult;
import com.synq.backend.domain.meeting.service.MeetingService;
import com.synq.backend.domain.meeting.web.MeetingWsUrlResolver;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 참여도 종료와 마찬가지로 프로젝트에 종속되지 않는 flat 경로(/meetings/{meetingId})를 쓴다.
@RestController
@RequestMapping("/meetings/{meetingId}")
@RequiredArgsConstructor
public class MeetingJoinController implements MeetingJoinControllerDocs {

	private final MeetingService meetingService;
	private final MeetingWsUrlResolver wsUrlResolver;

	@Override
	public ResponseEntity<ApiResponse<MeetingJoinResponse>> join(
			Long meetingId, Long userId, HttpServletRequest servletRequest) {
		MeetingJoinResult result = meetingService.join(meetingId, userId);
		String wsUrl = wsUrlResolver.resolve(servletRequest, meetingId);
		return ResponseEntity.ok(
				ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, MeetingJoinResponse.from(result, wsUrl)));
	}
}
