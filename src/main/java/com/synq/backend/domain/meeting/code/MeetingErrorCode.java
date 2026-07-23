package com.synq.backend.domain.meeting.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingErrorCode implements BaseCode {
	CONSENT_REQUIRED(HttpStatus.BAD_REQUEST,
			"MEETING400_1",
			"녹음/전사/AI 활용 동의가 필요합니다."),
	NOT_PROJECT_MEMBER(HttpStatus.FORBIDDEN,
			"MEETING403_1",
			"프로젝트 멤버만 회의를 생성할 수 있습니다."),
	NOT_HOST(HttpStatus.FORBIDDEN,
			"MEETING403_2",
			"회의를 종료할 권한이 없습니다."),
	MEETING_NOT_FOUND(HttpStatus.NOT_FOUND,
			"MEETING404_1",
			"존재하지 않는 회의입니다."),
	MEETING_ALREADY_ENDED(HttpStatus.CONFLICT,
			"MEETING409_1",
			"이미 종료된 회의입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
