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
			"이미 종료된 회의입니다."),
	NOT_PROJECT_MEMBER_TO_JOIN(HttpStatus.FORBIDDEN,
			"MEETING403_3",
			"프로젝트 멤버만 회의에 참여할 수 있습니다."),
	NOT_MEETING_PARTICIPANT(HttpStatus.FORBIDDEN,
			"MEETING403_4",
			"현재 회의 참여자만 이용할 수 있습니다."),
	NOT_HOST_TO_UPDATE_TITLE(HttpStatus.FORBIDDEN,
			"MEETING403_5",
			"회의 제목을 수정할 권한이 없습니다."),
	BLANK_TITLE(HttpStatus.BAD_REQUEST,
			"MEETING400_2",
			"회의 제목은 비어 있을 수 없습니다."),
	NOT_HOST_TO_DELETE(HttpStatus.FORBIDDEN,
			"MEETING403_6",
			"회의를 삭제할 권한이 없습니다."),
	CANNOT_DELETE_IN_PROGRESS_MEETING(HttpStatus.CONFLICT,
			"MEETING409_2",
			"진행 중인 회의는 삭제할 수 없습니다. 먼저 회의를 종료해주세요."),
	HOST_CANNOT_LEAVE(HttpStatus.FORBIDDEN,
			"MEETING403_7",
			"호스트는 회의를 나갈 수 없습니다. 회의 종료 또는 삭제를 이용해주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
