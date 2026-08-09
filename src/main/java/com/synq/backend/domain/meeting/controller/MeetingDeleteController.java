package com.synq.backend.domain.meeting.controller;

import com.synq.backend.domain.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 종료/삭제 등 회의 단건 대상 동작은 프로젝트에 종속되지 않는 flat 경로(/meetings/{meetingId})를 쓴다.
@RestController
@RequestMapping("/meetings/{meetingId}")
@RequiredArgsConstructor
public class MeetingDeleteController implements MeetingDeleteControllerDocs {

	private final MeetingService meetingService;

	@Override
	public ResponseEntity<Void> delete(Long meetingId, Long userId) {
		meetingService.delete(meetingId, userId);
		return ResponseEntity.noContent().build();
	}
}
