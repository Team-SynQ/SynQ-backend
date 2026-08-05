package com.synq.backend.domain.reference.code;

import com.synq.backend.global.apipayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReferenceErrorCode implements BaseCode {
	LINK_ADDRESS_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
			"REFERENCE400_1",
			"허용되지 않는 주소입니다."),
	LINK_UNREACHABLE(HttpStatus.BAD_REQUEST,
			"REFERENCE400_2",
			"링크에 접근할 수 없습니다."),
	LINK_UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST,
			"REFERENCE400_3",
			"HTML 또는 텍스트 문서 링크만 등록할 수 있습니다."),
	LINK_TOO_LARGE(HttpStatus.BAD_REQUEST,
			"REFERENCE400_4",
			"링크 문서가 너무 큽니다."),
	REFERENCE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN,
			"REFERENCE403_1",
			"참고자료 삭제 권한이 없습니다."),
	REFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND,
			"REFERENCE404_1",
			"참고자료를 찾을 수 없습니다."),
	REFERENCE_LIMIT_EXCEEDED(HttpStatus.CONFLICT,
			"REFERENCE409_1",
			"프로젝트별 최대 참고자료 수를 초과했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
