package com.synq.backend.domain.ai.summary.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SummaryErrorCode implements BaseCode {
	MEETING_NOT_ENDED(HttpStatus.CONFLICT,
			"SUMMARY409_1",
			"종료된 회의만 요약을 생성할 수 있습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
