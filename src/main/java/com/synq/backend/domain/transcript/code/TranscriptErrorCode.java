package com.synq.backend.domain.transcript.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TranscriptErrorCode implements BaseCode {
	TIMESTAMP_OUT_OF_RANGE(HttpStatus.BAD_REQUEST,
			"TRANSCRIPT400_1",
			"전사 타임스탬프가 허용 범위를 벗어났습니다."),
	NOT_HOST(HttpStatus.FORBIDDEN,
			"TRANSCRIPT403_1",
			"회의 호스트만 오디오를 전송할 수 있습니다."),
	MEETING_NOT_FOUND(HttpStatus.NOT_FOUND,
			"TRANSCRIPT404_1",
			"존재하지 않는 회의입니다."),
	MEETING_NOT_IN_PROGRESS(HttpStatus.CONFLICT,
			"TRANSCRIPT409_1",
			"진행 중인 회의에만 오디오를 전송할 수 있습니다."),
	STT_UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
			"TRANSCRIPT503_1",
			"전사 서비스에 연결할 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
